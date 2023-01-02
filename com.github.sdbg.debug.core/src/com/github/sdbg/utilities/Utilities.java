package com.github.sdbg.utilities;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

/**
 * Other Utilities that are not otherwise handled.
 */
public class Utilities
{
    /**
     * Cached extensions for HTML files.
     */
    static final String[] HTML_FILE_EXTENSIONS = { "html", "htm" };

    /**
     * Cached extension for JavaScript files.
     */
    static final String JS_FILE_EXTENSION = "js";

    public static File getEclipseInstallationDirectory()
    {
        return new File(Platform.getInstallLocation().getURL().getFile());
    }

    /**
     * Returns the current value of the string-valued user-defined property with
     * the given name. Returns <code>null</code> if there is no user-defined
     * property with the given name.
     * <p>
     * User-defined properties are defined in the <code>editor.properties</code>
     * file located in the eclipse installation directory.
     * 
     * @see getEclipseInstallationDirectory
     * @param name
     *            the name of the property
     * @return the string-valued property
     */
    public static String getUserDefinedProperty(String key)
    {

        Properties properties = new Properties();

        File installDirectory = getEclipseInstallationDirectory();
        File file = new File(installDirectory, "editor.properties");

        if (file.exists())
        {
            try
            {
                properties.load(new FileReader(file));
            }
            catch (FileNotFoundException e)
            {
                // &&&logError(e);
                SDBGDebugCorePlugin.logError(e);
            }
            catch (IOException e)
            {
                // &&&logError(e);
                SDBGDebugCorePlugin.logError(e);
            }
        }

        return properties.getProperty(key);
    }

    /**
     * Return <code>true</code> if the given file name's extension is an
     * HTML-like extension.
     * 
     * @param fileName
     *            the file name being tested
     * @return <code>true</code> if the given file name's extension is an
     *         HTML-like extension
     */
    public static boolean isHtmlLikeFileName(String fileName)
    {
        return Utilities.isLikeFileName(fileName, HTML_FILE_EXTENSIONS);
    }

    /**
     * Return <code>true</code> if the given file name's extension is an
     * HTML-like extension.
     * 
     * @param fileName
     *            the file name being tested
     * @return <code>true</code> if the given file name's extension is an
     *         HTML-like extension
     */
    public static boolean isJSLikeFileName(String fileName)
    {
        return Utilities.isLikeFileName(fileName, JS_FILE_EXTENSION);
    }

    /**
     * Return <code>true</code> if the given file name's extension matches one
     * of the passed extensions.
     * 
     * @param fileName
     *            the file name being tested
     * @param extensions
     *            an array of file extensions to test against
     * @return <code>true</code> if the given file name's extension matches one
     *         of the passed extensions
     */
    static boolean isLikeFileName(String fileName, String... extensions)
    {
        if (fileName == null || fileName.length() == 0)
        {
            return false;
        }
        for (String extension : extensions)
        {
            if (StringUtilities.endsWithIgnoreCase(fileName, '.' + extension))
            {
                return true;
            }
        }
        return false;
    }

}
