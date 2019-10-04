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
package workbench.util.function;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts a single input argument and returns no result.
 * <p>
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object)}.
 * <p>
 * This is a copy of {@link Consumer} that supports checked exceptions
 *
 * @param <T>
 *
 * @see Consumer
 * @author Andreas Krist
 */
@FunctionalInterface
public interface WbConsumer<T>
{
  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   */
  void accept(T t)
    throws Exception;

  /**
   * Returns a composed {@code Consumer} that performs, in sequence, this
   * operation followed by the {@code after} operation. If performing either
   * operation throws an exception, it is relayed to the caller of the
   * composed operation. If performing this operation throws an exception,
   * the {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   *
   * @return a composed {@code Consumer} that performs in sequence this
   *         operation followed by the {@code after} operation
   *
   * @throws NullPointerException if {@code after} is null
   */
  default WbConsumer<T> andThen(WbConsumer<? super T> after)
  {
    Objects.requireNonNull(after);
    return (T t) ->
    {
      accept(t);
      after.accept(t);
    };
  }
}
