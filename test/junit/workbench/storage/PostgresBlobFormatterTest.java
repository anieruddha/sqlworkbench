/*
 * PostgresBlobFormatterTest.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 *
 */
package workbench.storage;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;
import org.junit.Test;


/**
 * @author Thomas Kellerer
 */
public class PostgresBlobFormatterTest
{

	@Test
	public void testGetBlobLiteral()
		throws Exception
	{
		PostgresBlobFormatter formatter = new PostgresBlobFormatter(BlobLiteralType.pgEscape);
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		b.write(255);
		b.write(0);
		b.write(16);
		b.write(15);
		byte[] blob = b.toByteArray();
		String literal = formatter.getBlobLiteral(blob).toString();
		assertEquals("Wrong literal created", "E'\\\\377\\\\000\\\\020\\\\017'::bytea", literal);

		formatter = new PostgresBlobFormatter(BlobLiteralType.pgDecode);
		String decodeLiteral = formatter.getBlobLiteral(blob).toString();
		assertEquals("Wrong literal created", "decode('ff00100f', 'hex')", decodeLiteral);

		formatter = new PostgresBlobFormatter(BlobLiteralType.pgHex);
		decodeLiteral = formatter.getBlobLiteral(blob).toString();
		assertEquals("Wrong literal created", "\\\\xff00100f", decodeLiteral);
	}

}
