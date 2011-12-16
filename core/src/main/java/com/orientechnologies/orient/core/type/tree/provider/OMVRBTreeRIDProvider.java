/*
 * Copyright 1999-2011 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.type.tree.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.StringTokenizer;

import com.orientechnologies.common.collection.OMVRBTree;
import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.memory.OMemoryWatchDog.Listener;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.serialization.serializer.string.OStringBuilderSerializable;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeEntryPersistent;
import com.orientechnologies.orient.core.type.tree.OMVRBTreePersistent;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRID;

/**
 * MVRB-Tree implementation to handle a set of RID. It's serialized as embedded or external binary. Once external cannot come back
 * to the embedded mode.
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 */
public class OMVRBTreeRIDProvider extends OMVRBTreeProviderAbstract<OIdentifiable, OIdentifiable> implements
		OStringBuilderSerializable {
	public static final String	PERSISTENT_CLASS_NAME	= "ORIDs";
	private static final long		serialVersionUID			= 1L;

	private OMVRBTreeRID				tree;
	private boolean							embeddedStreaming			= true;								// KEEP THE STREAMING MODE
	private final StringBuilder	buffer								= new StringBuilder();
	private boolean							marshalling						= true;
	private Listener						watchDog;

	/**
	 * Copy constructor
	 */
	public OMVRBTreeRIDProvider(final OMVRBTreeRIDProvider iSource) {
		this(null, iSource.getClusterId(), iSource.getRoot());
		buffer.append(iSource.buffer);
		marshalling = false;
	}

	public OMVRBTreeRIDProvider(final OStorage iStorage, final int iClusterId, final ORID iRID) {
		this(iStorage, getDatabase().getClusterNameById(iClusterId));
		if (iRID != null)
			root = (ORecordId) iRID.copy();
		marshalling = false;
	}

	public OMVRBTreeRIDProvider(final ORID iRID) {
		this(null, getDatabase().getDefaultClusterId());
		if (iRID != null)
			root = (ORecordId) iRID.copy();
		marshalling = false;
	}

	public OMVRBTreeRIDProvider(final OStorage iStorage, final int iClusterId) {
		this(iStorage, getDatabase().getClusterNameById(iClusterId));
		marshalling = false;
	}

	public OMVRBTreeRIDProvider(final String iClusterName) {
		this(null, iClusterName);
		marshalling = false;
	}

	protected OMVRBTreeRIDProvider(final OStorage iStorage, final String iClusterName) {
		super(new ODocument(getDatabase()), iStorage, iClusterName);
		((ODocument) record).field("pageSize", pageSize);
	}

	public OMVRBTreeRIDEntryProvider getEntry(final ORID iRid) {
		return new OMVRBTreeRIDEntryProvider(this, iRid);
	}

	public OMVRBTreeRIDEntryProvider createEntry() {
		return new OMVRBTreeRIDEntryProvider(this);
	}

	//
	// @Override
	// protected void finalize() throws Throwable {
	// if (watchDog != null)
	// Orient.instance().getMemoryWatchDog().removeListener(watchDog);
	// }

	public OStringBuilderSerializable toStream(final StringBuilder iBuffer) throws OSerializationException {
		final long timer = OProfiler.getInstance().startChrono();

		if (buffer.length() > 0 && getDatabase().getTransaction().isActive() && buffer.indexOf("-") > -1) {
			// IN TRANSACTION: UNMARSHALL THE BUFFER TO AVOID TO STORE TEMP RIDS
			lazyUnmarshall();
			buffer.setLength(0);
		}

		tree.saveAllNewEntries();

		if (buffer.length() == 0)
			// MARSHALL IT
			try {
				if (isEmbeddedStreaming()) {
					marshalling = true;
					// SERIALIZE AS AN EMBEDDED STRING
					buffer.append(OStringSerializerHelper.COLLECTION_BEGIN);

					// PERSISTENT RIDS
					boolean first = true;
					for (OIdentifiable rid : tree.keySet()) {
						if (!first)
							buffer.append(OStringSerializerHelper.COLLECTION_SEPARATOR);
						else
							first = false;

						rid.getIdentity().toString(buffer);
					}

					// TEMPORARY RIDS
					final IdentityHashMap<ORecord<?>, Object> tempRIDs = tree.getTemporaryEntries();
					if (tempRIDs != null && !tempRIDs.isEmpty())
						for (ORecord<?> rec : tempRIDs.keySet()) {
							if (!first)
								buffer.append(OStringSerializerHelper.COLLECTION_SEPARATOR);
							else
								first = false;

							rec.getIdentity().toString(buffer);
						}

					buffer.append(OStringSerializerHelper.COLLECTION_END);
				} else {
					marshalling = true;
					buffer.append(OStringSerializerHelper.EMBEDDED_BEGIN);
					buffer.append(new String(toDocument().toStream()));
					buffer.append(OStringSerializerHelper.EMBEDDED_END);
				}

			} finally {
				marshalling = false;
				OProfiler.getInstance().stopChrono("OMVRBTreeRIDProvider.toStream", timer);
			}

		iBuffer.append(buffer);

		return this;
	}

	public OSerializableStream fromStream(final byte[] iStream) throws OSerializationException {
		record.fromStream(iStream);
		fromDocument((ODocument) record);
		return this;
	}

	public OStringBuilderSerializable fromStream(final StringBuilder iInput) throws OSerializationException {
		if (iInput != null) {
			// COPY THE BUFFER: IF THE TREE IS UNTOUCHED RETURN IT
			buffer.setLength(0);
			buffer.append(iInput);
		}

		return this;
	}

	public void lazyUnmarshall() {
		if (getSize() > 0 || marshalling || buffer.length() == 0)
			// ALREADY UNMARSHALLED
			return;

		marshalling = true;

		try {
			final char firstChar = buffer.charAt(0);

			String value = firstChar == OStringSerializerHelper.COLLECTION_BEGIN ? buffer.substring(1, buffer.length() - 1) : buffer
					.toString();

			if (firstChar == OStringSerializerHelper.COLLECTION_BEGIN || firstChar == OStringSerializerHelper.LINK) {
				setEmbeddedStreaming(true);
				final StringTokenizer tokenizer = new StringTokenizer(value, ",");
				while (tokenizer.hasMoreElements()) {
					final ORecordId rid = new ORecordId(tokenizer.nextToken());
					tree.put(rid, rid);
				}
			} else {
				setEmbeddedStreaming(false);
				value = firstChar == OStringSerializerHelper.EMBEDDED_BEGIN ? value.substring(1, value.length() - 1) : value.toString();
				fromStream(value.getBytes());
				tree.load();
			}
		} finally {
			marshalling = false;
		}
	}

	public byte[] toStream() throws OSerializationException {
		return toDocument().toStream();
	}

	public OMVRBTree<OIdentifiable, OIdentifiable> getTree() {
		return tree;
	}

	public void setTree(final OMVRBTreePersistent<OIdentifiable, OIdentifiable> tree) {
		this.tree = (OMVRBTreeRID) tree;
	}

	@Override
	public int getSize() {
		if (embeddedStreaming)
			return size;
		else {
			final OMVRBTreeEntryPersistent<OIdentifiable, OIdentifiable> r = (OMVRBTreeEntryPersistent<OIdentifiable, OIdentifiable>) tree
					.getRoot();
			return r == null ? 0 : (((OMVRBTreeRIDEntryProvider) r.getProvider()).getTreeSize());
		}
	}

	@Override
	public boolean setSize(final int iSize) {
		if (embeddedStreaming)
			super.setSize(iSize);
		else {
			final OMVRBTreeEntryPersistent<OIdentifiable, OIdentifiable> r = (OMVRBTreeEntryPersistent<OIdentifiable, OIdentifiable>) tree
					.getRoot();
			if (r != null) {
				if (((OMVRBTreeRIDEntryProvider) r.getProvider()).setTreeSize(iSize))
					r.markDirty();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean setDirty() {
		if (!marshalling) {
			if (buffer != null)
				buffer.setLength(0);
			return super.setDirty();
		}
		return false;
	}

	public ODocument toDocument() {
		tree.saveAllNewEntries();

		// SERIALIZE AS LINK TO THE TREE STRUCTURE
		final ODocument doc = (ODocument) record;
		doc.setClassName(PERSISTENT_CLASS_NAME);
		doc.field("root", root != null ? root : null);
		if (tree.getTemporaryEntries() != null && tree.getTemporaryEntries().size() > 0)
			doc.field("tempEntries", new ArrayList<ORecord<?>>(tree.getTemporaryEntries().keySet()));

		return doc;
	}

	public void fromDocument(final ODocument iDocument) {
		pageSize = (Integer) iDocument.field("pageSize");
		root = iDocument.field("root", OType.LINK);
		final Collection<OIdentifiable> tempEntries = iDocument.field("tempEntries");
		if (tempEntries != null && !tempEntries.isEmpty())
			for (OIdentifiable entry : tempEntries)
				tree.put(entry, null);
	}

	public boolean isEmbeddedStreaming() {
		if (embeddedStreaming && !marshalling) {
			final int binaryThreshold = OGlobalConfiguration.MVRBTREE_RID_BINARY_THRESHOLD.getValueAsInteger();
			if (binaryThreshold > 0 && getSize() > binaryThreshold && tree != null) {
				// CHANGE TO EXTERNAL BINARY
				tree.setDirtyOwner();
				setEmbeddedStreaming(false);
			}
		}
		return embeddedStreaming;
	}

	protected void setEmbeddedStreaming(final boolean iValue) {
		if (embeddedStreaming != iValue) {
			embeddedStreaming = iValue;

			if (!iValue)
				// ASSURE TO SAVE THE SIZE IN THE ROOT NODE
				setSize(size);
			//
			// if (!iValue) {
			// // INSTALL WATCHDOG TO CONTROL TREE SIZE IN MEMORY
			// watchDog = new Listener() {
			// public void memoryUsageLow(final long iFreeMemory, final long iFreeMemoryPercentage) {
			// tree.setOptimization(iFreeMemoryPercentage < 10 ? 2 : 1);
			// }
			// };
			// }
			// Orient.instance().getMemoryWatchDog().addListener(watchDog);
		}
	}

	@Override
	public boolean updateConfig() {
		pageSize = OGlobalConfiguration.MVRBTREE_RID_NODE_PAGE_SIZE.getValueAsInteger();
		return false;
	}
}
