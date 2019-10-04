/*
 * ScrollAnnotation.java
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
package workbench.sql;

import java.util.Set;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ScrollAnnotation
	extends WbAnnotation
{
	public static final String ANNOTATION = "WbScrollTo";
	public static final String END_KEYWORD = "end";

	private static final Set<String> END_KEYWORDS = CollectionUtil.caseInsensitiveSet("bottom", "last", END_KEYWORD);

	public ScrollAnnotation()
	{
		super(ANNOTATION);
	}

	public static String getScrollToEndAnnotation()
	{
		return "-- @" + ANNOTATION + " " + END_KEYWORD;
	}

	public static boolean scrollToEnd(String value)
	{
		return END_KEYWORDS.contains(value);
	}

	public static int scrollToLine(String value)
	{
		if (StringUtil.isNonBlank(value))
		{
			if (value.startsWith("#"))
			{
				return StringUtil.getIntValue(value.substring(1), -1);
			}
		}
		return -1;
	}
}
