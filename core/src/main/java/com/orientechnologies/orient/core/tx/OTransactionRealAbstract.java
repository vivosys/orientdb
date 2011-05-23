/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
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
package com.orientechnologies.orient.core.tx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerStringAbstract;

public abstract class OTransactionRealAbstract extends OTransactionAbstract {
	protected Map<ORID, OTransactionRecordEntry>				recordEntries			= new HashMap<ORID, OTransactionRecordEntry>();
	protected Map<String, List<OTransactionIndexEntry>>	indexEntries			= new HashMap<String, List<OTransactionIndexEntry>>();
	protected int																				id;
	protected int																				newObjectCounter	= -2;

	protected OTransactionRealAbstract(ODatabaseRecordTx iDatabase, int iId) {
		super(iDatabase);
		id = iId;
	}

	public int getId() {
		return id;
	}

	public void clearRecordEntries() {
		recordEntries.clear();
	}

	public Collection<OTransactionRecordEntry> getRecordEntries() {
		return recordEntries.values();
	}

	public ORecordInternal<?> getRecordEntry(final ORecordId rid) {
		OTransactionRecordEntry e = recordEntries.get(rid);
		if (e != null)
			return e.getRecord();
		return null;
	}

	public List<OTransactionRecordEntry> getRecordEntriesByClass(final String iClassName) {
		final List<OTransactionRecordEntry> result = new ArrayList<OTransactionRecordEntry>();

		if (iClassName == null || iClassName.length() == 0)
			// RETURN ALL THE RECORDS
			for (OTransactionRecordEntry entry : recordEntries.values()) {
				result.add(entry);
			}
		else
			// FILTER RECORDS BY CLASSNAME
			for (OTransactionRecordEntry entry : recordEntries.values()) {
				if (entry.getRecord() != null && entry.getRecord() instanceof ODocument
						&& iClassName.equals(((ODocument) entry.getRecord()).getClassName()))
					result.add(entry);
			}

		return result;
	}

	public List<OTransactionRecordEntry> getRecordEntriesByClusterIds(final int[] iIds) {
		final List<OTransactionRecordEntry> result = new ArrayList<OTransactionRecordEntry>();

		if (iIds == null)
			// RETURN ALL THE RECORDS
			for (OTransactionRecordEntry entry : recordEntries.values()) {
				result.add(entry);
			}
		else
			// FILTER RECORDS BY ID
			for (OTransactionRecordEntry entry : recordEntries.values()) {
				for (int id : iIds) {
					if (entry.getRecord() != null && entry.getRecord().getIdentity().getClusterId() == id) {
						result.add(entry);
						break;
					}
				}
			}

		return result;
	}

	public int getRecordEntriesSize() {
		return recordEntries.size();
	}

	public void clearIndexEntries() {
		indexEntries.clear();
	}

	public ODocument getIndexEntries() {
		final StringBuilder value = new StringBuilder();

		final ODocument doc = new ODocument();
		for (Entry<String, List<OTransactionIndexEntry>> indexEntry : indexEntries.entrySet()) {
			// STORE INDEX NAME
			final List<ODocument> indexDocs = new ArrayList<ODocument>();
			doc.field(indexEntry.getKey(), indexDocs);

			// STORE INDEX ENTRIES
			for (OTransactionIndexEntry entry : indexEntry.getValue()) {
				final ODocument indexDoc = new ODocument();

				// END OF INDEX ENTRIES
				indexDoc.field("s", entry.status.ordinal());

				// SERIALIZE KEY
				value.setLength(0);
				ORecordSerializerStringAbstract.fieldTypeToString(value, null, OType.getTypeByClass(entry.key.getClass()), entry.key);
				indexDoc.field("k", value.toString());

				// SERIALIZE VALUE
				if (entry.value != null) {
					value.setLength(0);
					ORecordSerializerStringAbstract.fieldTypeToString(value, null, OType.getTypeByClass(entry.value.getClass()), entry.value);
					indexDoc.field("v", value.toString());
				}

				indexDocs.add(indexDoc);
			}
		}
		return doc;
	}

	/**
	 * Bufferizes index changes to be flushed at commit time.
	 */
	public void addIndexEntry(final OIndex delegate, final String iIndexName, final OTransactionIndexEntry.STATUSES iStatus,
			final Object iKey, final OIdentifiable iValue) {
		List<OTransactionIndexEntry> indexEntry = indexEntries.get(iIndexName);
		if (indexEntry == null) {
			indexEntry = new ArrayList<OTransactionIndexEntry>();
			indexEntries.put(iIndexName, indexEntry);
		}

		indexEntry.add(new OTransactionIndexEntry(iStatus, iKey, iValue));
	}

	@Override
	protected void checkTransaction() {
		if (status == TXSTATUS.INVALID)
			throw new OTransactionException("Invalid state of the transaction. The transaction must be begun.");
	}
}
