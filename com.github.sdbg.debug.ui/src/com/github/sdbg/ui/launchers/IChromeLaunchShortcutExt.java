package com.github.sdbg.ui.launchers;

import org.eclipse.core.resources.IResource;

public interface IChromeLaunchShortcutExt {

  String EXTENSION_ID = "com.github.sdbg.debug.ui.launchChrome";

  public void launch(IResource resource, String mode, String url);

}
