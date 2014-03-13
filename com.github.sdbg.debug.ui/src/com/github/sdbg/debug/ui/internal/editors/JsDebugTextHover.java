// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//Use of this source code is governed by a BSD-style license that can be
//found in the LICENSE file.

package com.github.sdbg.debug.ui.internal.editors;

//&&&package org.chromium.debug.ui.editors;


/**
 * Supplies a hover for JavaScript expressions while on a breakpoint.
 */
public class JsDebugTextHover /*&&&implements ITextHover*/{
//
//  private static final JsValueStringifier STRINGIFIER = new JsValueStringifier();
//
//  @Override
//  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
//    IDocument doc = textViewer.getDocument();
//    String expression = JavascriptUtil.extractSurroundingJsIdentifier(doc, hoverRegion.getOffset());
//    if (expression == null) {
//      return null;
//    }
//
//    IAdaptable context = DebugUITools.getDebugContext();
//    if (context == null) { // debugger not active
//      return null;
//    }
//
//    EvaluateContext evaluateContext = (EvaluateContext) context.getAdapter(EvaluateContext.class);
//    if (evaluateContext == null) {
//      return null;
//    }
//
//    final JsValue[] result = new JsValue[1];
//    evaluateContext.getJsEvaluateContext().evaluateSync(
//        expression,
//        null,
//        new JsEvaluateContext.EvaluateCallback() {
//          public void failure(Exception cause) {
//          }
//
//          @Override
//          public void success(ResultOrException valueOrException) {
//            result[0] = valueOrException.accept(new ResultOrException.Visitor<JsValue>() {
//              @Override
//              public JsValue visitException(JsValue exception) {
//                return null;
//              }
//
//              @Override
//              public JsValue visitResult(JsValue value) {
//                return value;
//              }
//            });
//          }
//        });
//    if (result[0] == null) {
//      return null;
//    }
//
//    return STRINGIFIER.render(result[0]);
//  }
//
//  @Override
//  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
//    IDocument doc = textViewer.getDocument();
//    return JavascriptUtil.getSurroundingIdentifierRegion(doc, offset, false);
//  }
//
}
