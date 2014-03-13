// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;

import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;

import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A utility for handling JavaScript-related data.
 */
public class JavascriptUtil {

  private static final String OPEN_BRACKET = "["; //$NON-NLS-1$

  private static final String CLOSE_BRACKET = "]"; //$NON-NLS-1$

  private static final String ID_CHARS_REGEX = "\\p{L}_$\\d"; //$NON-NLS-1$

  private static final String QUALIFIED_ID_CHARS_REGEX = ID_CHARS_REGEX + "\\.\\[\\]"; //$NON-NLS-1$

  /**
   * Contains chars acceptable as part of expression to inspect to the right of the cursor.
   */
  public static final Pattern ID_PATTERN = Pattern.compile(OPEN_BRACKET + ID_CHARS_REGEX
      + CLOSE_BRACKET);

  /**
   * Contains chars acceptable as part of expression to inspect to the left of the cursor.
   */
  public static final Pattern QUALIFIED_ID_PATTERN = Pattern.compile(OPEN_BRACKET
      + QUALIFIED_ID_CHARS_REGEX + CLOSE_BRACKET);

  /**
   * Returns a JavaScript qualified identifier surrounding the character at {@code offset} position
   * in the given {@code document}.
   * 
   * @param document to extract an identifier from
   * @param offset of the pivot character (before, in, or after the identifier)
   * @return JavaScript identifier, or {@code null} if none found
   */
  public static String extractSurroundingJsIdentifier(IDocument document, int offset) {
    IRegion region = getSurroundingIdentifierRegion(document, offset, true);
    try {
      return region == null ? null : document.get(region.getOffset(), region.getLength());
    } catch (BadLocationException e) {
      SDBGDebugUIPlugin.logError(e);
      return null;
    }
  }

  /**
   * Returns a region enclosing a JavaScript identifier found in {@code doc} at the {@code offset}
   * position. If {@code qualified == true}, all leading qualifying names will be included into the
   * region, otherwise the member operator (".") will be considered as an identifier terminator.
   * 
   * @param doc the document to extract an identifier region from
   * @param offset of the pivot character (before, in, or after the identifier)
   * @param qualified whether to read qualified identifiers rather than simple ones
   * @return an IRegion corresponding to the JavaScript identifier overlapping offset, or null if
   *         none
   */
  public static IRegion getSurroundingIdentifierRegion(IDocument doc, int offset, boolean qualified) {
    if (doc == null) {
      return null;
    }
    try {
      int squareBrackets = 0;
      char ch = doc.getChar(offset);
      if (!isJsIdentifierCharacter(ch, qualified) && offset > 0) {
        --offset; // cursor is AFTER the identifier
      }
      int start = offset;
      int end = offset;
      int goodStart = offset;
      while (start >= 0) {
        ch = doc.getChar(start);
        if (!isJsIdentifierCharacter(ch, qualified)) {
          break;
        }
        if (ch == '[') {
          squareBrackets--;
        } else if (ch == ']') {
          squareBrackets++;
        }
        if (squareBrackets < 0) {
          break;
        }
        goodStart = start;
        --start;
      }
      start = goodStart;

      int length = doc.getLength();
      while (end < length) {
        try {
          ch = doc.getChar(end);
          if (!isJsIdentifierCharacter(ch, false)) {
            // stop at the current name qualifier
            // rather than scan through the entire qualified id
            break;
          }
          ++end;
        } catch (BadLocationException e) {
          SDBGDebugUIPlugin.logError(e);
        }
      }
      if (start >= end) {
        return null;
      } else {
        return new Region(start, end - start);
      }
    } catch (BadLocationException e) {
      SDBGDebugUIPlugin.logError(e);
      return null;
    }
  }

  public static boolean isJsIdentifierCharacter(char ch, boolean qualified) {
    return qualified ? QUALIFIED_ID_PATTERN.matcher(String.valueOf(ch)).find()
        : ID_PATTERN.matcher(String.valueOf(ch)).find();
  }

  private JavascriptUtil() {
    // not instantiable
  }

}
