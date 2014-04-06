// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

public class JsEditorDocumentSetup implements IDocumentSetupParticipant {

  @Override
  public void setup(IDocument document) {
    IDocumentPartitioner partitioner = new FastPartitioner(
        new JsPartitionScanner(),
        JsPartitionScanner.PARTITION_TYPES);
    document.setDocumentPartitioner(partitioner);
    partitioner.connect(document);
  }
}
