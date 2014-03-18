// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * Provides JavaScript content and sets up the document partitioner.
 */
public class JsDocumentProvider extends FileDocumentProvider {

  /**
   * Alternative implementation of the method that does not require file to be a physical file.
   */
  @Override
  public boolean isDeleted(Object element) {
    if (element instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) element;

      IProject project = input.getFile().getProject();
      if (project != null && !project.exists()) {
        return true;
      }

      return !input.getFile().exists();
    }
    return super.isDeleted(element);
  }

  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    IDocument doc = super.createDocument(element);
    if (doc != null) {
      IDocumentPartitioner partitioner = new FastPartitioner(
          new JsPartitionScanner(),
          JsPartitionScanner.PARTITION_TYPES);
      partitioner.connect(doc);
      doc.setDocumentPartitioner(partitioner);
    }
    return doc;
  }

}
