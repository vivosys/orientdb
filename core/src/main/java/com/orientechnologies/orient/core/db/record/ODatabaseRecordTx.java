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
package com.orientechnologies.orient.core.db.record;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.core.tx.OTransaction.TXSTATUS;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import com.orientechnologies.orient.core.tx.OTransactionNoTx;
import com.orientechnologies.orient.core.tx.OTransactionOptimistic;

/**
 * Delegates all the CRUD operations to the current transaction.
 * 
 */
public class ODatabaseRecordTx extends ODatabaseRecordAbstract {
	private OTransaction				currentTx;
	private static volatile int	txSerial	= 0;

	public ODatabaseRecordTx(final String iURL, final Class<? extends ORecordInternal<?>> iRecordClass) {
		super(iURL, iRecordClass);
		init();
	}

	public ODatabaseRecord begin() {
		return begin(TXTYPE.OPTIMISTIC);
	}

	public ODatabaseRecord begin(final TXTYPE iType) {
		currentTx.rollback();

		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onBeforeTxBegin(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error before tx begin", t);
			}

		switch (iType) {
		case NOTX:
			setDefaultTransactionMode();
			break;

		case OPTIMISTIC:
			currentTx = new OTransactionOptimistic(this, txSerial++);
			break;

		case PESSIMISTIC:
			throw new UnsupportedOperationException("Pessimistic transaction");
		}

		currentTx.begin();
		return this;
	}

	public ODatabaseRecord begin(final OTransaction iTx) {
		currentTx.rollback();

		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onBeforeTxBegin(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error before tx begin", t);
			}

		currentTx = iTx;
		currentTx.begin();

		return this;
	}

	public ODatabaseRecord commit() {
		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onBeforeTxCommit(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error before tx commit", t);
			}

		try {
			currentTx.commit();
		} catch (RuntimeException e) {
			getCache().clear();
			throw e;
		} finally {
			setDefaultTransactionMode();
		}

		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onAfterTxCommit(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error after tx commit", t);
			}

		return this;
	}

	public ODatabaseRecord rollback() {
		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onBeforeTxRollback(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error before tx rollback", t);
			}

		try {
			currentTx.rollback();
		} finally {
			setDefaultTransactionMode();
		}

		// WAKE UP LISTENERS
		for (ODatabaseListener listener : underlying.getListeners())
			try {
				listener.onAfterTxRollback(underlying);
			} catch (Throwable t) {
				OLogManager.instance().error(this, "Error after tx rollback", t);
			}

		return this;
	}

	public OTransaction getTransaction() {
		return currentTx;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <RET extends ORecordInternal<?>> RET load(final ORecordInternal<?> iRecord, final String iFetchPlan) {
		return (RET) currentTx.load(iRecord.getIdentity(), iRecord, iFetchPlan);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <RET extends ORecordInternal<?>> RET load(ORecordInternal<?> iRecord) {
		return (RET) currentTx.load(iRecord.getIdentity(), iRecord, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <RET extends ORecordInternal<?>> RET load(ORID iRecordId) {
		return (RET) currentTx.load(iRecordId, null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <RET extends ORecordInternal<?>> RET load(ORID iRecordId, String iFetchPlan) {
		return (RET) currentTx.load(iRecordId, null, iFetchPlan);
	}

	@Override
	public ODatabaseRecord save(final ORecordInternal<?> iContent) {
		return save(iContent, null);
	}

	@Override
	public ODatabaseRecord save(final ORecordInternal<?> iContent, final String iClusterName) {
		currentTx.save(iContent, iClusterName);
		return this;
	}

	@Override
	public ODatabaseRecord delete(final ORecordInternal<?> iRecord) {
		currentTx.delete(iRecord);
		return this;
	}

	public void executeRollback(final OTransaction iTransaction) {
	}

	public void executeCommit() {
		getStorage().commit(getId(), currentTx);
	}

	protected void checkTransaction() {
		if (currentTx == null || currentTx.getStatus() == TXSTATUS.INVALID)
			throw new OTransactionException("Transaction not started");
	}

	private void init() {
		currentTx = new OTransactionNoTx(this, -1);
	}

	public ORecordInternal<?> getRecordByUserObject(final Object iUserObject, final boolean iIsMandatory) {
		return (ORecordInternal<?>) iUserObject;
	}

	public void registerPojo(final Object iObject, final ORecordInternal<?> iRecord) {
	}

	public Object getUserObjectByRecord(final ORecordInternal<?> record, final String iFetchPlan) {
		return record;
	}

	public boolean existsUserObjectByRID(final ORID iRID) {
		return true;
	}

	private void setDefaultTransactionMode() {
		if (!(currentTx instanceof OTransactionNoTx))
			currentTx = new OTransactionNoTx(this, txSerial++);
	}
}
