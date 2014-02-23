package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.breakpoints.IBreakpointPathResolver;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;

/**
 * This class implements support for Java breakpoints which are set on classes that are not
 * resources in the workspace. For example - classes in the JRE or other JAR libraries.
 */
public class JDTBreakpointPathResolver implements IBreakpointPathResolver {
  public JDTBreakpointPathResolver() {
  }

  @Override
  public String getPath(IBreakpoint breakpoint) throws CoreException {
    IJavaBreakpoint bp = (IJavaBreakpoint) breakpoint;

    String type = bp.getTypeName();
    int innerClassIndex = type.indexOf('$');
    if (innerClassIndex > 0) {
      // For inner classes, return the outermost class name, 
      // as all inner classes are contained in the file of the outermost class
      type = type.substring(0, innerClassIndex);
    }

    return type.replace('.', '/') + ".java";
  }

  @Override
  public boolean isSupported(IBreakpoint breakpoint) {
    return breakpoint instanceof IJavaBreakpoint;
  }
}
