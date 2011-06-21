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
package com.orientechnologies.orient.core.index;

import java.util.Map.Entry;
import java.util.Set;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Interface to handle index.
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 * 
 */
public interface OIndexInternal extends OIndex, Iterable<Entry<Object, Set<OIdentifiable>>> {

	public static final String	CONFIG_TYPE				= "type";
	public static final String	CONFIG_NAME				= "name";
	public static final String	CONFIG_AUTOMATIC	= "automatic";

	public void checkEntry(final OIdentifiable iRecord, final Object iKey);

	public void close();

	public OIndexInternal rebuild();

	/**
	 * Populate the index with all the existent records.
	 */
	public OIndexInternal rebuild(final OProgressListener iProgressListener);

	/**
	 * Counts the times a value is indexed in all the keys
	 * 
	 * @param iRecord
	 *          Record to search
	 * @return Times the record is found, 0 if not found at all
	 */
	public int count(final OIdentifiable iRecord);

	public OIndexInternal loadFromConfiguration(ODocument iConfig);

	public ODocument updateConfiguration();
}
