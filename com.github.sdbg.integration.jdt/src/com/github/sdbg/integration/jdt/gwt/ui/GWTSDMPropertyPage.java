package com.github.sdbg.integration.jdt.gwt.ui;

import com.github.sdbg.integration.jdt.gwt.GWTSDMProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class GWTSDMPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
  private Button recompileEnabledCheck;
  private Text codeServerHostText;
  private Text codeServerPortText;
  private Text moduleNameText;
  private Button chromeLiveEditEnabledCheck;

  public GWTSDMPropertyPage() {
  }

  @Override
  public boolean performOk() {
    applyChanges();
    return super.performOk();
  }

  @Override
  protected Control createContents(Composite parent) {
    try {
      Composite contents = new Composite(parent, SWT.NONE);
      contents.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

      GWTSDMProperties properties = getProperties();

      recompileEnabledCheck = new Button(contents, SWT.CHECK);
      recompileEnabledCheck.setText("Enable automatic GWT SDM recompilation during project build");
      recompileEnabledCheck.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
      recompileEnabledCheck.setSelection(properties.isRecompileEnabled());
      recompileEnabledCheck.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateControls();
        }
      });
      Label label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Code Server host: ");

      codeServerHostText = new Text(contents, SWT.BORDER);
      codeServerHostText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(
          true,
          false).create());
      codeServerHostText.setText(properties.getCodeServerHost());

      label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Code Server port: ");

      codeServerPortText = new Text(contents, SWT.BORDER);
      codeServerPortText.setLayoutData(GridDataFactory.swtDefaults().hint(60, SWT.DEFAULT).create());
      codeServerPortText.setText(Integer.toString(properties.getCodeServerPort()));

      label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("GWT Module name: ");

      moduleNameText = new Text(contents, SWT.BORDER);
      moduleNameText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(
          true,
          false).create());
      moduleNameText.setText(properties.getModuleName());

      chromeLiveEditEnabledCheck = new Button(contents, SWT.CHECK);
      chromeLiveEditEnabledCheck.setText("Enable Chrome Live Edit updates on successful rebuild");
      chromeLiveEditEnabledCheck.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
      chromeLiveEditEnabledCheck.setSelection(properties.isRecompileEnabled());

      updateControls();

      return contents;
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void performApply() {
    applyChanges();
    super.performApply();
  }

  @Override
  protected void performDefaults() {
    recompileEnabledCheck.setSelection(GWTSDMProperties.DEFVALUE_RECOMPILE_ENABLED);
    codeServerHostText.setText(GWTSDMProperties.DEFVALUE_CODE_SERVER_HOST);
    codeServerPortText.setText(Integer.toString(GWTSDMProperties.DEFVALUE_CODE_SERVER_PORT));
    moduleNameText.setText(GWTSDMProperties.DEFVALUE_MODULE_NAME);
    chromeLiveEditEnabledCheck.setSelection(GWTSDMProperties.DEFVALUE_CHROME_LIVE_EDIT_ENABLED);

    updateControls();

    super.performDefaults();
  }

  private void applyChanges() {
    try {
      GWTSDMProperties properties = getProperties();
      properties.setRecompileEnabled(recompileEnabledCheck.getSelection());
      properties.setCodeServerHost(codeServerHostText.getText());
      properties.setCodeServerPort(Integer.parseInt(codeServerPortText.getText()));
      properties.setModuleName(moduleNameText.getText());
      properties.setChromeLiveEditEnabled(chromeLiveEditEnabledCheck.getSelection());
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  private GWTSDMProperties getProperties() {
    return new GWTSDMProperties((IProject) getElement());
  }

  private void updateControls() {
    boolean enabled = recompileEnabledCheck.getSelection();
    codeServerHostText.setEnabled(enabled);
    codeServerPortText.setEnabled(enabled);
    moduleNameText.setEnabled(enabled);
    chromeLiveEditEnabledCheck.setEnabled(enabled);
  }
}
