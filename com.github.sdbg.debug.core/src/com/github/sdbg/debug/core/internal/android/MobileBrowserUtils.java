package com.github.sdbg.debug.core.internal.android;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

public class MobileBrowserUtils {
  public static void addChromiumForward(ADBManager manager, String deviceId, int localTCPPort)
      throws CoreException {
    manager.addForward(
        deviceId,
        "tcp:" + Integer.toString(localTCPPort),
        "localabstract:chrome_devtools_remote");
  }

  public static void addContentShellForward(ADBManager manager, String deviceId, int localTCPPort)
      throws CoreException {
    manager.addForward(
        deviceId,
        "tcp:" + Integer.toString(localTCPPort),
        "localabstract:content_shell_devtools_remote");
  }

  /**
   * Force stop (close) the content shell on the connected phone
   * <p>
   * adb shell am force-stop org.chromium.content_shell_apk
   * </p>
   * 
   * @throws CoreException
   */
  public static void forceStopContentShell(ADBManager manager, String deviceId)
      throws CoreException {
    manager.shell("am", "force-stop", "org.chromium.content_shell_apk");
  }

  /**
   * Install the apk for the content shell onto the connected phone
   * <p>
   * adb install path/to/apk
   * </p>
   * 
   * @throws CoreException
   */
  public static void installContentShellApk(ADBManager manager, String deviceId)
      throws CoreException {
    File contentShellLocation = null;
    manager.install(deviceId, contentShellLocation);
  }

  /**
   * Open the url in the chrome browser on the device
   * <p>
   * adb shell am start com.android.chrome/com.google.android.apps.chrome.Main -d url
   * </p>
   * 
   * @throws CoreException
   */
  public static void launchChromeBrowser(ADBManager manager, String deviceId, String url)
      throws CoreException {
    manager.shell(
        deviceId,
        "am",
        "start",
        "-n",
        "com.android.chrome/com.google.android.apps.chrome.Main",
        "-d",
        url);
  }

  /**
   * Launch the browser on the phone and open url
   * <p>
   * adb shell am start -n org.chromium.content_shell_apk/.ContentShellActivity -d
   * http://www.cheese.com
   * </p>
   * 
   * @throws CoreException
   */
  public static void launchContentShell(ADBManager manager, String deviceId, String url)
      throws CoreException {
    manager.shell(
        deviceId,
        "am",
        "start",
        "-n",
        "org.chromium.content_shell_apk/.ContentShellActivity",
        "-d",
        url);
  }

  /**
   * Uninstall the content shell apk from the device
   * 
   * @throws CoreException
   */
  public static void uninstallContentShellApk(ADBManager manager, String deviceId)
      throws CoreException {
    manager.uninstall(deviceId, "org.chromium.content_shell_apk");
  }

  private MobileBrowserUtils() {
  }
}
