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
package com.orientechnologies.orient.test.database.auto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.hook.ORecordHook;

@Test(groups = "db")
public class DbImportExportTest implements OCommandOutputListener {
	private static final String	DB2_PATH	= "tests/target/test-import";
	private static final String	DB2_URL		= "local:tests/target/test-import/test-import";
	private String							url;

	@Parameters(value = "url")
	public DbImportExportTest(String iURL) {
		url = iURL;
		OProfiler.getInstance().startRecording();
	}

	@Test
	public void testDbExport() throws IOException {
		ODatabaseDocumentTx database = new ODatabaseDocumentTx(url);
		database.open("admin", "admin");

		ODatabaseExport export = new ODatabaseExport(database, "tests/target/db.export", this);
		export.exportDatabase();
		export.close();

		database.close();
	}

	@Test(dependsOnMethods = "testDbExport")
	public void testDbImport() throws IOException {
		final File importDir = new File(DB2_PATH);
		if (importDir.exists())
			for (File f : importDir.listFiles())
				f.delete();
		else
			importDir.mkdir();

		ODatabaseDocumentTx database = new ODatabaseDocumentTx(DB2_URL);
		database.create();

		ODatabaseImport impor = new ODatabaseImport(database, "tests/target/db.export", this);
		
		// UNREGISTER ALL THE HOOKS
		for (ORecordHook hook : new ArrayList<ORecordHook>(database.getHooks())) {
			database.unregisterHook(hook);
		}

		impor.importDatabase();
		impor.close();

		database.close();
	}

	@Test(enabled = false)
	public void onMessage(final String iText) {
		System.out.print(iText);
		System.out.flush();
	}
}