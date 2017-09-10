package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.breakpoints.IBreakpointPathResolver;

import org.eclipse.core.resources.IResource;
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

    String path = getLocalPath(bp);

    if (path != null) {
      return path;
    }

    String type = bp.getTypeName();
    if (type != null) {
      int innerClassIndex = type.indexOf('$');
      if (innerClassIndex > 0) {
        // For inner classes, return the outermost class name, 
        // as all inner classes are contained in the file of the outermost class
        type = type.substring(0, innerClassIndex);
      }

      return type.replace('.', '/') + ".java";
    } else {
      return null;
    }
  }

  public String getLocalPath(IJavaBreakpoint bp) throws CoreException {
    if (bp.getMarker() != null 
        && bp.getMarker().getResource() != null
        && bp.getMarker().getResource().getType() == IResource.FILE) {      
      String type = bp.getTypeName();
      if (type != null) {
        String pkg = (type.lastIndexOf('.') >= 0 ? (type.substring(0, type.lastIndexOf('.')) + "/") : "").replace('.', '/');
        return pkg + bp.getMarker().getResource().getName();
      }
    }
    return null;
  }  

  @Override
  public boolean isSupported(IBreakpoint breakpoint) {
    return breakpoint instanceof IJavaBreakpoint;
  }
}
