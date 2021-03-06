/*
 * Copyright (C) 2015 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.zerobuilder.compiler.test_util;

import net.zerobuilder.compiler.Messages;
import net.zerobuilder.compiler.ZeroProcessor;

public final class GeneratedLines {
  public static final String GENERATED_ANNOTATION =
      "@Generated("
          + "value = \""
          + ZeroProcessor.class.getName()
          + "\", "
          + "comments = \""
          + Messages.JavadocMessages.GENERATED_COMMENTS
          + "\")";

  private GeneratedLines() {
    throw new UnsupportedOperationException("no instances");
  }
}
