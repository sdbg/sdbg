// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;

import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * In case the editor wants to, syntax highlighting is disabled. Useful for opening very large .js
 * documents.
 */
public class JsSourceViewerScalableConfiguration extends JsSourceViewerConfiguration {
  private JsEditor editor;

  public JsSourceViewerScalableConfiguration(JsEditor editor) {
    this.editor = editor;
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    if (getEditor().isScalabilityModeEnabled()) {
      return null;
    } else {
      return super.getPresentationReconciler(sourceViewer);
    }
  }

  @Override
  public IReconciler getReconciler(ISourceViewer sourceViewer) {
    if (getEditor().isScalabilityModeEnabled()) {
      return null;
    } else {
      return super.getReconciler(sourceViewer);
    }
  }

  private JsEditor getEditor() {
    return editor;
  }
}
