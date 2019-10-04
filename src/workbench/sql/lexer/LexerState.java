/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.sql.lexer;

/**
 *
 * @author Thomas Kellerer
 */
public class LexerState
{
  private int parenthesesCount;

  public LexerState()
  {
  }

  public LexerState(int initialCount)
  {
    this.parenthesesCount = initialCount;
  }

  public void visit(SQLToken token)
  {
    if (token == null) return;
    String text = token.getText();
    if (text.equals("("))
    {
      parenthesesCount ++;
    }
    else if (text.equals(")"))
    {
      parenthesesCount --;
    }
  }

  public int getParenthesesCount()
  {
    return parenthesesCount;
  }

  public boolean inParentheses()
  {
    return parenthesesCount > 0;
  }
  
}
