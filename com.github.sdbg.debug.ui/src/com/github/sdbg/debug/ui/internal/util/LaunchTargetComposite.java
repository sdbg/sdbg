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
package com.github.sdbg.debug.ui.internal.util;

import com.github.sdbg.debug.ui.internal.chrome.ChromeLaunchMessages;
import com.github.sdbg.debug.ui.internal.util.AppSelectionDialog.HtmlResourceFilter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * A composite that creates a group to enter html and url information for chrome/browser launch
 */
public class LaunchTargetComposite extends Composite {

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  private Button htmlButton;
  private Text htmlText;
  private Button htmlBrowseButton;
  private Button urlButton;
  private Text urlText;
  private Text projectText;
  private Button projectBrowseButton;

  private int widthHint;

  private Label projectLabel;

  public LaunchTargetComposite(Composite parent, int style, boolean allowHtmlFile) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    setLayout(layout);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(this);

    Group group = new Group(this, SWT.NONE);
    group.setText(ChromeLaunchMessages.ChromeMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    if (allowHtmlFile) {
      createHtmlField(group);
    }

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).hint(-1, 4).applyTo(filler);

    createUrlField(group, allowHtmlFile);
  }

  public int getButtonWidthHint() {
    return widthHint;
  }

  public String getErrorMessage() {
    if (htmlButton != null && htmlButton.getSelection() && htmlText.getText().length() == 0) {
      return ChromeLaunchMessages.ChromeMainTab_NoHtmlFile;
    }

    if (urlButton == null || urlButton.getSelection()) {
      String url = urlText.getText();

      if (urlButton != null) {
        if (url.length() == 0) {
          return ChromeLaunchMessages.ChromeMainTab_NoUrl;
        }

        if (!isValidUrl(url)) {
          return ChromeLaunchMessages.ChromeMainTab_InvalidURL;
        }
      }

      if (projectText.getText().length() == 0) {
        return ChromeLaunchMessages.ChromeMainTab_NoProject;
      }

      try {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
            projectText.getText());
        if (!project.exists()) {
          return ChromeLaunchMessages.ChromeMainTab_InvalidProject;
        }
      } catch (IllegalArgumentException e) {
        return ChromeLaunchMessages.ChromeMainTab_InvalidProject + ": " + e.getMessage();
      }
    }

    return null;
  }

  public boolean getHtmlButtonSelection() {
    return htmlButton != null ? htmlButton.getSelection() : false;
  }

  public String getHtmlFileName() {
    return htmlText != null ? htmlText.getText().trim() : "";
  }

  public int getLabelColumnWidth() {
    projectLabel.pack();
    return projectLabel.getSize().x;
  }

  public String getProject() {
    return projectText.getText().trim();
  }

  public String getUrlString() {
    return urlText.getText().trim();
  }

  public void setHtmlButtonSelection(boolean state) {
    if (htmlButton != null) {
      htmlButton.setSelection(state);
      urlButton.setSelection(!state);
      updateEnablements(state);
    }
  }

  public void setHtmlTextValue(String string) {
    if (htmlText != null) {
      htmlText.setText(string);
    }
  }

  public void setProjectTextValue(String sourceDirectoryName) {
    projectText.setText(sourceDirectoryName);

  }

  public void setUrlTextValue(String string) {
    urlText.setText(string);
  }

  protected void createHtmlField(Composite composite) {
    htmlButton = new Button(composite, SWT.RADIO);
    htmlButton.setText(ChromeLaunchMessages.ChromeMainTab_HtmlFileLabel);
    htmlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(true);
        notifyPanelChanged();
      }
    });

    htmlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    htmlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(400, SWT.DEFAULT).grab(
        true,
        false).applyTo(htmlText);

    htmlBrowseButton = new Button(composite, SWT.PUSH);
    htmlBrowseButton.setText(ChromeLaunchMessages.ChromeMainTab_SelectHtmlFile);
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        htmlBrowseButton);
    htmlBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });
  }

  protected void createUrlField(Composite composite, boolean allowHtmlFile) {
    if (allowHtmlFile) {
      urlButton = new Button(composite, SWT.RADIO);
      urlButton.setText(ChromeLaunchMessages.ChromeMainTab_UrlFilterLabel);
      urlButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnablements(false);
          notifyPanelChanged();
        }
      });
    } else {
      Label urlLabel = new Label(composite, SWT.NONE);
      urlLabel.setText(ChromeLaunchMessages.ChromeMainTab_UrlLabel);
      GridDataFactory.swtDefaults().applyTo(urlLabel);
    }

    urlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    urlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);

    // spacer
    new Label(composite, SWT.NONE);

    projectLabel = new Label(composite, SWT.NONE);
    projectLabel.setText(ChromeLaunchMessages.ChromeMainTab_ProjectLabel);
    if (allowHtmlFile) {
      GridDataFactory.swtDefaults().indent(20, 0).applyTo(projectLabel);
    } else {
      GridDataFactory.swtDefaults().applyTo(projectLabel);
    }

    projectText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    projectText.addModifyListener(textModifyListener);
    projectText.setCursor(composite.getShell().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectText);

    projectBrowseButton = new Button(composite, SWT.PUSH);
    projectBrowseButton.setText(ChromeLaunchMessages.ChromeMainTab_SelectProject);
    PixelConverter converter = new PixelConverter(projectBrowseButton);
    widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        projectBrowseButton);
    projectBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleProjectBrowseButton();
      }
    });
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(
        getShell(),
        workspace.getRoot(),
        new HtmlResourceFilter());
    dialog.setTitle(ChromeLaunchMessages.ChromeMainTab_SelectHtml);
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION); //$NON-NLS-1$
    IPath path = new Path(htmlText.getText());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();

    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      IFile file = (IFile) results[0];
      String pathStr = file.getFullPath().toPortableString();

      htmlText.setText(pathStr);

      notifyPanelChanged();
    }
  }

  protected void handleProjectBrowseButton() {
//    ContainerSelectionDialog dialog = new ContainerSelectionDialog(
//        getShell(),
//        null,
//        false,
//        ChromeLaunchMessages.ChromeMainTab_SelectProject);
//
//    dialog.open();
//
//    Object[] results = dialog.getResult();
//
//    if ((results != null) && (results.length > 0)) {
//      String pathStr = ((IPath) results[0]).toString();
//      projectText.setText(pathStr);
//      notifyPanelChanged();
//    }

    IProject project = chooseProject();
    if (project != null) {
      projectText.setText(project.getName());
      notifyPanelChanged();
    }
  }

  protected void updateEnablements(boolean isFile) {
    if (isFile) {
      htmlText.setEnabled(true);
      htmlBrowseButton.setEnabled(true);
      urlText.setEnabled(false);
      projectText.setEnabled(false);
      projectBrowseButton.setEnabled(false);
    } else {
      htmlText.setEnabled(false);
      htmlBrowseButton.setEnabled(false);
      urlText.setEnabled(true);
      projectText.setEnabled(true);
      projectBrowseButton.setEnabled(true);
    }
  }

  private IProject chooseProject() {
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(),
        new LabelProvider() {
          @Override
          public String getText(Object element) {
            return ((IProject) element).getName();
          }
        });
    dialog.setTitle(ChromeLaunchMessages.ChromeMainTab_SelectProjectTitle);
    dialog.setMessage(ChromeLaunchMessages.ChromeMainTab_SelectProjectMessage);

    dialog.setElements(ResourcesPlugin.getWorkspace().getRoot().getProjects());

    IProject project = null;

    try {
      project = ResourcesPlugin.getWorkspace().getRoot().getProject(getProject());
    } catch (IllegalArgumentException e) {
      // Best effort
    }

    if (project != null && project.exists()) {
      dialog.setInitialSelections(new Object[] {project});
    }
    if (dialog.open() == Window.OK) {
      return (IProject) dialog.getFirstResult();
    }
    return null;
  }

  private boolean isValidUrl(String url) {
    final String[] validSchemes = new String[] {"file:", "http:", "https:"};

    for (String scheme : validSchemes) {
      if (url.startsWith(scheme)) {
        return true;
      }
    }

    return false;
  }

  private void notifyPanelChanged() {
    Event event = new Event();
    event.type = SWT.Modify;
    event.widget = this;
    notifyListeners(SWT.Modify, event);
  }
}
