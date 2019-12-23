/*
 * Copyright (c) 2019 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.core.cobol.preprocessor.sub.line.transformer;

import com.ca.lsp.core.cobol.parser.listener.PreprocessorListener;
import com.ca.lsp.core.cobol.preprocessor.sub.CobolLine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This preprocessor transforms the lines containing the compiler directives is order to make them
 * processable with grammar.
 *
 * The CBL (PROCESS) statement can be preceded by a sequence number in columns 1 through 6. The
 * first character of the sequence number must be numeric, and CBL or PROCESS can begin in column 8
 * or after; if a sequence number is not specified, CBL or PROCESS can begin in column 1 or after.
 *
 * The CBL (PROCESS) statement must end before or at column 72, and options cannot be continued
 * across multiple CBL (PROCESS) statements. However, you can use more than one CBL (PROCESS)
 * statement. Multiple CBL (PROCESS) statements must follow one another with no intervening
 * statements of any other type.
 *
 * The CBL (PROCESS) statement must be placed before any comment lines or other
 * compiler-directing statements.
 */
public class CobolCompilerDirectives implements CobolLinesTransformation {

  private static final Pattern COMPILER_DIRECTIVE_LINE =
      Pattern.compile("(?i)(\\d.{0,6} +|\\s*)(CBL|PROCESS) .+");

  private PreprocessorListener listener;

  public CobolCompilerDirectives(PreprocessorListener listener) {
    this.listener = listener;
  }

  @Override
  public List<CobolLine> transformLines(List<CobolLine> lines) {
    return removeLinesWithCompilerDirectives(lines);
  }

  private List<CobolLine> removeLinesWithCompilerDirectives(List<CobolLine> lines) {
    for (int i = 0; i < lines.size(); i++) {
      CobolLine line = lines.get(i);
      Matcher matcher = COMPILER_DIRECTIVE_LINE.matcher(line.toString());
      if (matcher.matches()) {
        String content =
            line.serializeWithoutCommentArea().replaceAll("(?i)^.*(CBL|PROCESS)", "       CBL");
        lines.set(i, CobolLine.copyCobolLineWithContentArea(content, line));

        listener.unregisterError(i + 1);
      } else break;
    }
    return lines;
  }
}
