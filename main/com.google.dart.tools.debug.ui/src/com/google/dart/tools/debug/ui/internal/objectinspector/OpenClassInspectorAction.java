/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.debug.ui.internal.objectinspector;

import org.eclipse.ui.IObjectActionDelegate;

/**
 * Open an object inspector on the given object's class.
 */
public/*&&&*/abstract class OpenClassInspectorAction implements IObjectActionDelegate {
//&&& PRobably not really needed  
//  private ISelection selection;
//
//  public OpenClassInspectorAction() {
//
//  }
//
//  @Override
//  public void run(IAction action) {
//    if (selection != null) {
//      UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(getClass());
//
//      Object obj = SelectionUtil.getSingleElement(selection);
//
//      try {
//        instrumentation.record(selection);
//
//        if (obj instanceof IVariable) {
//          IVariable variable = (IVariable) obj;
//
//          obj = variable.getValue();
//        }
//
//        if (obj instanceof DartiumDebugValue) {
//          DartiumDebugValue value = (DartiumDebugValue) obj;
//
//          IValue classValue = value.getClassValue();
//
//          if (classValue != null) {
//            ObjectInspectorView.inspect(classValue);
//          }
//        } else if (obj instanceof ServerDebugValue) {
//          ServerDebugValue value = (ServerDebugValue) obj;
//
//          IValue classValue = value.getClassValue();
//
//          if (classValue != null) {
//            ObjectInspectorView.inspect(classValue);
//          }
//        }
//
//        instrumentation.metric("Inspect", "Completed");
//      } catch (DebugException e) {
//        DartDebugUIPlugin.logError(e);
//      } finally {
//        instrumentation.log();
//      }
//    }
//  }
//
//  @Override
//  public void selectionChanged(IAction action, ISelection selection) {
//    this.selection = selection;
//  }
//
//  @Override
//  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
//
//  }
//
}
