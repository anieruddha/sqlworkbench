/*
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
package workbench.db.postgres;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import workbench.db.DataTypeResolver;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class PostgresDataTypeResolver
  implements DataTypeResolver
{

  private static final Map<String, String> TYPE_TO_DISPLAY = new HashMap<>();
  private static final Map<String, String> DISPLAY_TO_TYPE = new HashMap<>();

  private boolean fixTimestampTZ;

  static
  {
    TYPE_TO_DISPLAY.put("_int2","smallint[]");
    TYPE_TO_DISPLAY.put("_int4","integer[]");
    TYPE_TO_DISPLAY.put("_int8","bigint[]");
    TYPE_TO_DISPLAY.put("_varchar","varchar[]");
    TYPE_TO_DISPLAY.put("_float4","real[]");
    TYPE_TO_DISPLAY.put("_float8","double precision[]");
    TYPE_TO_DISPLAY.put("_bpchar","char[]");
    TYPE_TO_DISPLAY.put("_text","text[]");
    TYPE_TO_DISPLAY.put("_bool","boolean[]");
    TYPE_TO_DISPLAY.put("_numeric","numeric[]");
    TYPE_TO_DISPLAY.put("_date","date[]");
    TYPE_TO_DISPLAY.put("_time","time[]");
    TYPE_TO_DISPLAY.put("_timestamp","timestamp[]");
    TYPE_TO_DISPLAY.put("_timestamptz","timestamptz[]");
    TYPE_TO_DISPLAY.put("_timetz","timetz[]");

    for (Map.Entry<String, String> entry : TYPE_TO_DISPLAY.entrySet())
    {
      DISPLAY_TO_TYPE.put(entry.getValue(), entry.getKey());
    }
    DISPLAY_TO_TYPE.put("timestamp without time zone[]", "_timestamp");
    DISPLAY_TO_TYPE.put("timestamp with time zone[]", "_timestamptz");
    DISPLAY_TO_TYPE.put("time without time zone[]", "_time");
    DISPLAY_TO_TYPE.put("time with time zone[]", "_timetz");
  }

  public void setFixTimestampTZ(boolean flag)
  {
    this.fixTimestampTZ = flag;
  }

  @Override
  public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
  {
    if (sqlType == Types.VARCHAR && "text".equals(dbmsName)) return "text";
    if (sqlType == Types.VARCHAR && "character varying".equals(dbmsName))
    {
      dbmsName = "varchar";
    }
    if (sqlType == Types.SMALLINT && "int2".equals(dbmsName)) return "smallint";
    if (sqlType == Types.INTEGER && "int4".equals(dbmsName)) return "integer";
    if (sqlType == Types.BIGINT && "int8".equals(dbmsName)) return "bigint";
    if ((sqlType == Types.BIT || sqlType == Types.BOOLEAN) && "bool".equals(dbmsName)) return "boolean";

    if (sqlType == Types.CHAR && "bpchar".equals(dbmsName))
    {
      return "char(" + size + ")";
    }

    if (sqlType == Types.VARCHAR && size == Integer.MAX_VALUE)
    {
      // enums are returned as Types.VARCHAR and size == Integer.MAX_VALUE
      // in order to not change the underlying data type, we just use
      // the type name that the driver returned
      return dbmsName;
    }

    if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL)
    {
      if (size == 65535 || size == 131089) size = 0;
      if (digits == 65531) digits = 0;
    }

    if (sqlType == Types.OTHER && "varbit".equals(dbmsName))
    {
      return "bit varying(" + size + ")";
    }

    if (sqlType == Types.BIT && "bit".equals(dbmsName))
    {
      return "bit(" + size + ")";
    }

    if (sqlType == Types.ARRAY && dbmsName.charAt(0) == '_')
    {
      return mapInternaArrayToDisplay(dbmsName);
    }

    if ("varchar".equals(dbmsName) && size < 0) return "varchar";

    return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
  }

  @Override
  public String getColumnClassName(int type, String dbmsType)
  {
    if (fixTimestampTZ && type == Types.TIMESTAMP_WITH_TIMEZONE && "timestamptz".equals(dbmsType))
    {
      return "java.time.ZonedDateTime";
    }
    return null;
  }

  @Override
  public int fixColumnType(int type, String dbmsType)
  {
    if (type == Types.BIT && "bool".equals(dbmsType)) return Types.BOOLEAN;
    if (fixTimestampTZ && type == Types.TIMESTAMP && "timestamptz".equals(dbmsType))
    {
      return Types.TIMESTAMP_WITH_TIMEZONE;
    }
    if (type == Types.TIME && "timetz".equals(dbmsType))
    {
      return Types.TIME_WITH_TIMEZONE;
    }
    return type;
  }

  public static String mapArrayDisplayToInternal(String dbmsType)
  {
    String internal = DISPLAY_TO_TYPE.get(dbmsType);
    if (internal != null)
    {
      return internal;
    }
    return "_" + StringUtil.replace(dbmsType, "[]", "");
  }

  public static String mapInternaArrayToDisplay(String internal)
  {
    if (StringUtil.isEmptyString(internal)) return null;

    String display = TYPE_TO_DISPLAY.get(internal);
    if (display != null) return display;
    if (internal.charAt(0) == '_')
    {
      return internal.substring(1) + "[]";
    }
    return internal;
  }
}
