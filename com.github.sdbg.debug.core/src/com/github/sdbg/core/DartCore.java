package com.github.sdbg.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.github.sdbg.core.utilities.general.StringUtilities;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;

/***
 * &&& Just the minimum possible from the original DartCore. Most likely these functions should not
 * be part of DartCore anyway
 */
public class DartCore {
  /**
   * Cached extensions for HTML files.
   */
  private static final String[] HTML_FILE_EXTENSIONS = {"html", "htm"};

  public static File getEclipseInstallationDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  /**
   * Returns the current value of the string-valued user-defined property with the given name.
   * Returns <code>null</code> if there is no user-defined property with the given name.
   * <p>
   * User-defined properties are defined in the <code>editor.properties</code> file located in the
   * eclipse installation directory.
   * 
   * @see DartCore#getEclipseInstallationDirectory()
   * @param name the name of the property
   * @return the string-valued property
   */
  public static String getUserDefinedProperty(String key) {

    Properties properties = new Properties();

    File installDirectory = getEclipseInstallationDirectory();
    File file = new File(installDirectory, "editor.properties");

    if (file.exists()) {
      try {
        properties.load(new FileReader(file));
      } catch (FileNotFoundException e) {
        //&&&logError(e);
        SDBGDebugCorePlugin.logError(e);
      } catch (IOException e) {
        //&&&logError(e);
        SDBGDebugCorePlugin.logError(e);
      }
    }

    return properties.getProperty(key);
  }

  /**
   * Return <code>true</code> if the given file name's extension is an HTML-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an HTML-like extension
   */
  public static boolean isHtmlLikeFileName(String fileName) {
    return isLikeFileName(fileName, HTML_FILE_EXTENSIONS);
  }

  /**
   * Return <code>true</code> if the given file name's extension matches one of the passed
   * extensions.
   * 
   * @param fileName the file name being tested
   * @param extensions an array of file extensions to test against
   * @return <code>true</code> if the given file name's extension matches one of the passed
   *         extensions
   */
  private static boolean isLikeFileName(String fileName, String[] extensions) {
    if (fileName == null || fileName.length() == 0) {
      return false;
    }
    for (String extension : extensions) {
      if (StringUtilities.endsWithIgnoreCase(fileName, '.' + extension)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isLinux() {
    return !isMac() && !isWindows();
  }

  public static boolean isMac() {
    // Look for the "Mac" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("mac");
  }

  public static boolean isWindows() {
    // Look for the "Windows" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("win");
  }

  public static boolean isWindowsXp() {
    // Look for the "Windows XP" OS name.
    return System.getProperty("os.name").toLowerCase().equals("windows xp");
  }

  private DartCore() {
  }
}
