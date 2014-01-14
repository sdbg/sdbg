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
package com.google.dart.tools.debug.ui.internal.view;

import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.internal.ui.views.launch.LaunchView;

/**
 * A custom debugger view that combines the stack trace view and the variables view together.
 */
@SuppressWarnings("restriction")
public/*&&&*/abstract class DebuggerView extends LaunchView implements ILaunchesListener {
//&&& PRobably not really needed  
//  public static final String ID = "com.google.dart.tools.debug.debuggerView";
//
//  private static final String SASH_WEIGHTS = "sashWeights";
//
//  private ToolBar toolbar;
//
//  private SashForm sashForm;
//  private DartVariablesView variablesView;
//
//  private static Image CONNECTED_IMAGE;
//  private static Image NOT_CONNECTED_IMAGE;
//
//  private ShowBreakpointsAction showBreakpointsAction;
//  private ShowInspectorAction showInspectorAction;
//  private ShowExpressionsAction showExpressionsAction;
//
//  private TreeModelViewer treeViewer;
//  private IPreferenceStore preferences;
//  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
//    @Override
//    public void propertyChange(PropertyChangeEvent event) {
//      doPropertyChange(event);
//    }
//  };
//
//  /**
//   * Create a new DebuggerView instance.
//   */
//  public DebuggerView() {
//
//  }
//
//  @Override
//  public void createPartControl(Composite parent) {
//    Composite composite = new Composite(parent, SWT.NONE);
//    GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(composite);
//
//    toolbar = new ToolBar(composite, SWT.HORIZONTAL);
//    GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbar);
//
//    Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
//    GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);
//
//    sashForm = new SashForm(composite, SWT.VERTICAL);
//    GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);
//
//    super.createPartControl(sashForm);
//
//    ISelectionProvider selProvider = getViewSite().getSelectionProvider();
//
//    variablesView = new DartVariablesView();
//    try {
//      variablesView.init(getViewSite());
//    } catch (PartInitException ex) {
//      DartUtil.logError(ex);
//    }
//    variablesView.createPartControl(sashForm);
//
//    if (getViewSite().getSelectionProvider() != selProvider) {
//      getViewSite().setSelectionProvider(selProvider);
//    }
//
//    variablesView.becomesVisible();
//    showInspectorAction.setSelectionProvider(variablesView);
//
//    restoreSashWeights(getMemento());
//    updateSashOrientation(sashForm);
//    sashForm.addControlListener(new ControlAdapter() {
//      @Override
//      public void controlResized(ControlEvent e) {
//        updateSashOrientation(sashForm);
//      }
//    });
//
//    IActionBars actionBars = getViewSite().getActionBars();
//
//    actionBars.getMenuManager().removeAll();
//    configureViewToolBar(actionBars.getToolBarManager());
//
//    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
//
//    if (CONNECTED_IMAGE == null) {
//      NOT_CONNECTED_IMAGE = DartDebugUIPlugin.getImage("obj16/debug_exc.gif");
//
//      CONNECTED_IMAGE = DartDebugUIPlugin.getImage(new DecorationOverlayIcon(
//          NOT_CONNECTED_IMAGE,
//          DartDebugUIPlugin.getImageDescriptor("ovr16/play.png"),
//          IDecoration.BOTTOM_RIGHT));
//    }
//
//    updateConnectionStatus();
//  }
//
//  @Override
//  public void dispose() {
//    DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
//
//    variablesView.dispose();
//    if (propertyChangeListener != null) {
//      getPreferences().removePropertyChangeListener(propertyChangeListener);
//      propertyChangeListener = null;
//    }
//
//    super.dispose();
//  }
//
//  @Override
//  public void launchesAdded(ILaunch[] launches) {
//    updateConnectionStatusAsync();
//  }
//
//  @Override
//  public void launchesChanged(ILaunch[] launches) {
//    updateConnectionStatusAsync();
//  }
//
//  @Override
//  public void launchesRemoved(ILaunch[] launches) {
//    updateConnectionStatusAsync();
//  }
//
//  @Override
//  public void saveViewerState(IMemento memento) {
//    super.saveViewerState(memento);
//
//    // Save the sash weights.
//    int[] weights = sashForm.getWeights();
//    memento.putString(SASH_WEIGHTS, weights[0] + "," + weights[1]);
//  }
//
//  @Override
//  protected void configureToolBar(IToolBarManager viewToolBarManager) {
//    ToolBarManager manager = new ToolBarManager(toolbar);
//
//    manager.add(getAction("resume"));
//    manager.add(getAction("suspend"));
//    manager.add(new BlankSeparator());
//    manager.add(getAction("step_into"));
//    manager.add(getAction("step_over"));
//    manager.add(getAction("step_return"));
//    manager.add(new BlankSeparator());
//    manager.add(getAction("terminate"));
//
//    manager.update(true);
//    toolbar.pack();
//
//    toolbar.getParent().layout(true);
//  }
//
//  protected void configureViewToolBar(IToolBarManager manager) {
//    manager.removeAll();
//
//    manager.add(showBreakpointsAction);
//    manager.add(showInspectorAction);
//    manager.add(showExpressionsAction);
//    manager.add(new Separator());
//    manager.add(new ToggleLogicalStructureAction(variablesView));
//    manager.add(new ToggleUseSourceMapsAction(this));
//
//    manager.update(true);
//  }
//
//  @Override
//  protected void createActions() {
//    super.createActions();
//
//    showBreakpointsAction = new ShowBreakpointsAction();
//    setAction("showBreakpointsAction", showBreakpointsAction);
//
//    showInspectorAction = new ShowInspectorAction();
//    setAction("showInspectorAction", showInspectorAction);
//
//    showExpressionsAction = new ShowExpressionsAction();
//    setAction("showExpressionsAction", showExpressionsAction);
//  }
//
//  @Override
//  protected TreeModelViewer createViewer(Composite parent) {
//    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
//    final TreeModelViewer treeViewer = (TreeModelViewer) super.createViewer(parent);
//    this.treeViewer = treeViewer;
//    treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
//    treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
//      @Override
//      public void handleEvent(Event event) {
//        SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
//      }
//    });
//    SWTUtil.bindJFaceResourcesFontToControl(treeViewer.getTree());
//    updateColors();
//    return treeViewer;
//  }
//
//  /**
//   * This method is overridden to remove the context menu for the Debugger view.
//   */
//  @Override
//  protected void fillContextMenu(IMenuManager menu) {
//
//  }
//
//  protected void updateColors() {
//    SWTUtil.setColors(treeViewer.getTree(), getPreferences());
//  }
//
//  void updateSashOrientation(SashForm sash) {
//    Rectangle r = sash.getBounds();
//
//    int orientation = (r.height * 1.25) >= r.width ? SWT.VERTICAL : SWT.HORIZONTAL;
//
//    if (sash.getOrientation() != orientation) {
//      sash.setOrientation(orientation);
//    }
//  }
//
//  private void doPropertyChange(PropertyChangeEvent event) {
//    updateColors();
//    treeViewer.refresh(false);
//  }
//
//  private IPreferenceStore getPreferences() {
//    return preferences;
//  }
//
//  private boolean isConnected() {
//    for (IDebugTarget debugTarget : DebugPlugin.getDefault().getLaunchManager().getDebugTargets()) {
//      if (!debugTarget.isTerminated()) {
//        return true;
//      }
//    }
//
//    return false;
//  }
//
//  private void restoreSashWeights(IMemento memento) {
//    if (memento != null && memento.getString(SASH_WEIGHTS) != null) {
//      String[] strs = memento.getString(SASH_WEIGHTS).split(",");
//
//      if (strs.length == 2) {
//        try {
//          sashForm.setWeights(new int[] {Integer.parseInt(strs[0]), Integer.parseInt(strs[1])});
//          return;
//        } catch (NumberFormatException ex) {
//          DartUtil.logError(ex);
//        }
//      }
//    }
//
//    sashForm.setWeights(new int[] {40, 60});
//  }
//
//  private void updateConnectionStatus() {
//    if (isConnected()) {
//      setTitleImage(CONNECTED_IMAGE);
//    } else {
//      setTitleImage(NOT_CONNECTED_IMAGE);
//    }
//  }
//
//  private void updateConnectionStatusAsync() {
//    Display.getDefault().asyncExec(new Runnable() {
//      @Override
//      public void run() {
//        updateConnectionStatus();
//      }
//    });
//  }
//
}
