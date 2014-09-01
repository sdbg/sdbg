package com.github.sdbg.integration.jdt.gwt.ui;

import com.github.sdbg.integration.jdt.gwt.GWTSDMProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class GWTSDMPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
  private Text codeServerHostText;
  private Text codeServerPortText;
  private Text moduleNameText;

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
      GridLayout mylayout = new GridLayout();
      mylayout.marginHeight = 1;
      mylayout.marginWidth = 1;
      contents.setLayout(mylayout);

      GWTSDMProperties properties = getProperties();

      Label label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Code Server host: ");

      codeServerHostText = new Text(contents, SWT.BORDER);
      codeServerHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      codeServerHostText.setText(properties.getCodeServerHost());

      label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("Code Server port: ");

      codeServerPortText = new Text(contents, SWT.BORDER);
      codeServerPortText.setLayoutData(new GridData(40, SWT.DEFAULT));
      codeServerPortText.setText(Integer.toString(properties.getCodeServerPort()));

      label = new Label(contents, SWT.NONE);
      label.setLayoutData(new GridData());
      label.setText("GWT Module name: ");

      moduleNameText = new Text(contents, SWT.BORDER);
      moduleNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      moduleNameText.setText(properties.getModuleName());

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
    codeServerHostText.setText(GWTSDMProperties.DEFVALUE_CODE_SERVER_HOST);
    codeServerPortText.setText(GWTSDMProperties.DEFVALUE_CODE_SERVER_PORT);
    moduleNameText.setText(GWTSDMProperties.DEFVALUE_MODULE_NAME);

    super.performDefaults();
  }

  private void applyChanges() {
    try {
      GWTSDMProperties properties = getProperties();
      properties.setCodeServerHost(codeServerHostText.getText());
      properties.setCodeServerPort(Integer.parseInt(codeServerPortText.getText()));
      properties.setModuleName(moduleNameText.getText());
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  private GWTSDMProperties getProperties() {
    return new GWTSDMProperties((IProject) getElement());
  }
}
