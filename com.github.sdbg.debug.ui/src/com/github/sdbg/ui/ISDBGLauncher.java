package com.github.sdbg.ui;

import org.eclipse.core.resources.IResource;

public interface ISDBGLauncher {

  String EXTENSION_ID = "com.github.sdbg.debug.ui.sdbgLauncher";

  public void launchChrome(IResource resource, String mode, String url);

}
