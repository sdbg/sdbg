/*
 * Copyright (c) 2012, the Dart project authors.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

import com.github.sdbg.debug.ui.internal.DebugErrorHandler;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;
import com.github.sdbg.debug.ui.internal.util.LaunchUtils;
import com.github.sdbg.ui.instrumentation.UIInstrumentationBuilder;

/**
 * Launch in Chrome
 */
public class RunInChromeAction extends RunAbstractAction {

  public RunInChromeAction() {
    this(null);
  }

  public RunInChromeAction(IWorkbenchWindow window) {
    this(window, false);
  }

  public RunInChromeAction(IWorkbenchWindow window, boolean noMenu) {
    super(window, "Run in Chrome", noMenu ? IAction.AS_PUSH_BUTTON : IAction.AS_DROP_DOWN_MENU);

    setActionDefinitionId("com.github.sdbg.tools.debug.ui.run.chrome");
    setImageDescriptor(SDBGDebugUIPlugin.getImageDescriptor("obj16/run_exc.png"));
  }

  @Override
  protected void doLaunch(UIInstrumentationBuilder instrumentation) {
    IResource resource = LaunchUtils.getSelectedResource(window);
    if (resource == null) {
      return;
    }

    try {
      instrumentation.metric("Resource-Class", resource.getClass().toString());
      instrumentation.data("Resource-Name", resource.getName());

      // new launch config
      ILaunchShortcut shortcut = LaunchUtils.getChromeLaunchShortcut();
      ISelection selection = new StructuredSelection(resource);
      launch(shortcut, selection, instrumentation);
    } catch (Exception exception) {
      instrumentation.metric("Problem", "Exception launching " + exception.getClass().toString());
      instrumentation.data("Problem", "Exception launching " + exception.toString());

      DebugErrorHandler.errorDialog(window.getShell(), "Error Launching", "Unable to launch "
          + resource.getName() + ".", exception);
    }
  }

}
