// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * A simplistic JavaScript editor which supports its own key binding scope.
 */
public class JsEditor extends TextEditor {

  /** The ID of this editor as defined in plugin.xml */
  public static final String EDITOR_ID = "com.github.sdbg.debug.ui.internal.editors.JsEditor"; //$NON-NLS-1$

  /** The ID of the editor context menu */
  public static final String EDITOR_CONTEXT = EDITOR_ID + ".context"; //$NON-NLS-1$

  /** The ID of the editor ruler context menu */
  public static final String RULER_CONTEXT = EDITOR_ID + ".ruler"; //$NON-NLS-1$

  private boolean scalabilityModeEnabled;

  public JsEditor() {
    setSourceViewerConfiguration(new JsSourceViewerScalableConfiguration(this));
    setKeyBindingScopes(new String[] {"org.eclipse.ui.textEditorScope", //$NON-NLS-1$
        "com.github.sdbg.debug.ui.internal.editors.JsEditor.context"}); //$NON-NLS-1$
  }

  public boolean isScalabilityModeEnabled() {
    return scalabilityModeEnabled;
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
    updateScalabilityMode(input);
  }

  @Override
  protected void initializeEditor() {
    super.initializeEditor();
    setEditorContextMenuId(EDITOR_CONTEXT);
    setRulerContextMenuId(RULER_CONTEXT);
  }

  private void updateScalabilityMode(IEditorInput input) {
    scalabilityModeEnabled = getDocumentProvider().getDocument(input) != null
        && getDocumentProvider().getDocument(input).getLength() > 1024 * 1024;
  }

//&&&  
//  @Override
//  protected void setPartName(String partName) {
//    super.setPartName(PluginUtil.stripChromiumExtension(partName, true));
//  }
}
