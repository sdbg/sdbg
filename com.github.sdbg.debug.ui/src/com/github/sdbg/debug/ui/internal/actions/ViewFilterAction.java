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
/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package com.github.sdbg.debug.ui.internal.actions;

import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Abstract action that can be used to update an {@link IDebugView}
 * 
 * @since 1.0
 */
public abstract class ViewFilterAction extends ViewerFilter implements IViewActionDelegate,
    IActionDelegate2 {
  protected IViewPart view;
  protected IAction action;

  protected ViewFilterAction() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void init(IAction action) {
    this.action = action;
  }

  @Override
  public void init(IViewPart view) {
    this.view = view;
  }

  @Override
  public void run(IAction action) {
    StructuredViewer viewer = getStructuredViewer();
    ViewerFilter[] filters = viewer.getFilters();
    ViewerFilter filter = null;
    for (int i = 0; i < filters.length; i++) {
      if (filters[i] == this) {
        filter = filters[i];
        break;
      }
    }
    if (filter == null) {
      viewer.addFilter(this);
    } else {
      // only refresh is removing - adding will refresh automatically
      viewer.refresh();
    }
  }

  @Override
  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
  }

  /**
   * @return the backing {@link StructuredViewer}
   */
  protected StructuredViewer getStructuredViewer() {
    IDebugView view = (IDebugView) getView().getAdapter(IDebugView.class);
    if (view != null) {
      Viewer viewer = view.getViewer();
      if (viewer instanceof StructuredViewer) {
        return (StructuredViewer) viewer;
      }
    }
    return null;
  }

  /**
   * Returns whether this action is selected/checked.
   * 
   * @return whether this action is selected/checked
   */
  protected boolean getValue() {
    return action.isChecked();
  }

  /**
   * @return the {@link IViewPart} handle
   */
  protected IViewPart getView() {
    return view;
  }
}
