package com.github.sdbg.utilities;

/**
 * Utilities to determine the OS.
 */
public class OSUtilities
{
    public static boolean isLinux()
    {
        return !isMac() && !isWindows();
    }

    public static boolean isMac()
    {
        // Look for the "Mac" OS name.
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public static boolean isWindows()
    {
        // Look for the "Windows" OS name.
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    private OSUtilities()
    {
    }
}
