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
package com.github.sdbg.debug.ui.internal.launch;

import org.eclipse.core.expressions.PropertyTester;

/**
 * A {@link PropertyTester} for checking whether the resource can be launched in Chrome. It is used
 * to contribute the Run in Chrome context menu in the Files view. Defines the property
 * "canLaunchChrome"
 */
public class RunInChromePropertyTester extends PropertyTester {

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
//&&&    
//    if ("canLaunchChrome".equalsIgnoreCase(property)) {
//      if (receiver instanceof IStructuredSelection) {
//        Object o = ((IStructuredSelection) receiver).getFirstElement();
//        if (o instanceof IFile) {
//          IFile file = (IFile) o;
//          if (DartCore.isHtmlLikeFileName(((IFile) o).getName())) {
//            return true;
//          }
//
//          ProjectManager manager = DartCore.getProjectManager();
//          if (manager.getSourceKind(file) == SourceKind.LIBRARY
//              && manager.isClientLibrary(manager.getSource(file))) {
//            return true;
//          }
//        }
//      }
//    }
    return false;
  }

}
