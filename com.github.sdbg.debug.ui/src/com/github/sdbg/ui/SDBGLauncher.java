package com.github.sdbg.ui;

import com.github.sdbg.debug.ui.internal.chrome.ChromeLaunchShortcut;

import org.eclipse.core.resources.IResource;

public class SDBGLauncher implements ISDBGLauncher {

  /**
   * Launch Chrome with the Javascript debugger.
   * 
   * @param resource the project
   * @param mode debug mode
   * @param url the url to load
   */
  @Override
  public void launchChrome(IResource resource, String mode, String url) {
    // Create or load existing launch config.
    new ChromeLaunchShortcut().launch(resource, mode, url);
  }

}
