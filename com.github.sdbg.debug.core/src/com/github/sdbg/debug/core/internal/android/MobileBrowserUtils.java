package com.github.sdbg.debug.core.internal.android;

import org.eclipse.core.runtime.CoreException;

public class MobileBrowserUtils {
  public static void addChromiumForward(ADBManager manager, String deviceId, int localTCPPort)
      throws CoreException {
    manager.addForward(
        deviceId,
        "tcp:" + Integer.toString(localTCPPort),
        "localabstract:chrome_devtools_remote");
  }

  /**
   * Force stop (close) the Chrome browser on the connected phone
   * <p>
   * adb shell am force-stop org.chromium.content_shell_apk
   * </p>
   * 
   * @throws CoreException
   */
  public static void forceStopChromeBrowser(ADBManager manager, String deviceId)
      throws CoreException {
    manager.shell("am", "force-stop", "com.android.chrome/com.google.android.apps.chrome.Main");
  }

  /**
   * Open the url in the chrome browser on the device
   * <p>
   * adb shell am start com.android.chrome/com.google.android.apps.chrome.Main -d url
   * </p>
   * 
   * @throws CoreException
   */
  public static void launchChromeBrowser(ADBManager manager, String deviceId) throws CoreException {
    manager.shell(
        deviceId,
        "am",
        "start",
        "-n",
        "com.android.chrome/com.google.android.apps.chrome.Main");
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

  private MobileBrowserUtils() {
  }
}
