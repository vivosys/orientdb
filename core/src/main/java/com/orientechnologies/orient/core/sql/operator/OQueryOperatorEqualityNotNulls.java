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
package com.orientechnologies.orient.core.sql.operator;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;

/**
 * Base equality operator that not admit NULL in the LEFT and in the RIGHT operator. Abstract class.
 * 
 * @author Luca Garulli
 * 
 */
public abstract class OQueryOperatorEqualityNotNulls extends OQueryOperatorEquality {

	protected OQueryOperatorEqualityNotNulls(String iKeyword, int iPrecedence, boolean iLogical) {
		super(iKeyword, iPrecedence, iLogical);
	}

	@Override
	public boolean evaluate(final ODatabaseRecord<?> iDatabase, final OSQLFilterCondition iCondition, final Object iLeft,
			final Object iRight) {
		if (iLeft == null || iRight == null)
			return false;

		return super.evaluate(iDatabase, iCondition, iLeft, iRight);
	}
}
