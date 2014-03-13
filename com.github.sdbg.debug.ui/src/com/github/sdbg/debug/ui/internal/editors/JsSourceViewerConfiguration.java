// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * A JavaScript source viewer configuration.
 */
public class JsSourceViewerConfiguration extends TextSourceViewerConfiguration {

  private static class MultilineCommentScanner extends BufferedRuleBasedScanner {
    public MultilineCommentScanner(TextAttribute attr) {
      setDefaultReturnToken(new Token(attr));
    }
  }

  private static final String[] CONTENT_TYPES = new String[] {
      IDocument.DEFAULT_CONTENT_TYPE, JsPartitionScanner.JSDOC,
      JsPartitionScanner.MULTILINE_COMMENT};

  private final JsCodeScanner scanner = new JsCodeScanner();

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return CONTENT_TYPES;
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler pr = new PresentationReconciler();
    pr.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
    setDamagerRepairer(pr, new DefaultDamagerRepairer(scanner), IDocument.DEFAULT_CONTENT_TYPE);
    setDamagerRepairer(
        pr,
        new DefaultDamagerRepairer(new MultilineCommentScanner(scanner.getCommentAttribute())),
        JsPartitionScanner.MULTILINE_COMMENT);
    setDamagerRepairer(
        pr,
        new DefaultDamagerRepairer(new MultilineCommentScanner(scanner.getJsDocAttribute())),
        JsPartitionScanner.JSDOC);
    return pr;
  }

//&&&  
//  @Override
//  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
//    return new JsDebugTextHover();
//  }

  private void setDamagerRepairer(PresentationReconciler pr,
      DefaultDamagerRepairer damagerRepairer, String tokenType) {
    pr.setDamager(damagerRepairer, tokenType);
    pr.setRepairer(damagerRepairer, tokenType);
  }

}
