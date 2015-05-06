package com.github.sdbg.integration.jdt.gwt.build;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.integration.jdt.SDBGJDTIntegrationPlugin;
import com.github.sdbg.integration.jdt.gwt.build.GWTSDMProperties.HotCodeReplacePolicy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONException;
import org.json.JSONObject;

public class GWTSDMBuilder extends IncrementalProjectBuilder {
  public static final String BUILDER_ID = SDBGJDTIntegrationPlugin.PLUGIN_ID + ".gwtsdmbuilder";

  private static final String MARKER_TYPE = SDBGJDTIntegrationPlugin.PLUGIN_ID + ".gwtsdmmarker";

  public GWTSDMBuilder() {
  }

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
    if (kind == FULL_BUILD) {
      fullBuild(monitor);
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        fullBuild(monitor);
      } else {
        incrementalBuild(delta, monitor);
      }
    }
    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
  }

  protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
    GWTSDMProperties properties = new GWTSDMProperties(getProject());
    if (!properties.isRecompileEnabled()) {
      return;
    }

    getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);

    String[] modules = properties.getModuleNames().split("\\s\\,\\;");

    final SubMonitor subMonitor = SubMonitor.convert(monitor);
    subMonitor.beginTask("Running GWT SDM Recompiler", modules.length);

    try {
      for (int i = 0; i < modules.length; i++) {
        GWTSDMCodeServerAPI codeServerAPI = getCodeServerAPI(properties, modules[i].trim());

        try {
          JSONObject result = codeServerAPI.recompile(subMonitor.newChild(1));
          if (!"ok".equals(result.getString("status"))) {
            URI logUri = codeServerAPI.getLogUri();

            IMarker marker = getProject().createMarker(MARKER_TYPE);
            marker.setAttribute(
                IMarker.MESSAGE,
                "GWT SDM recompilation failed for module " + codeServerAPI.getModule()
                    + ". Check your Code Server log (" + logUri.toASCIIString() + ").");

            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
          } else {
            notifyBuildCompleted(properties, codeServerAPI);
          }
        } catch (ConnectException e) {
          // The code server is not running. That's OK - stay silent
          SDBGDebugCorePlugin.logInfo("Code Server " + codeServerAPI.getCodeServerUri().toString()
              + " seems to be down. Skipping GWT SDM recompilation for module "
              + codeServerAPI.getModule());
        } catch (JSONException e) {
          throw SDBGJDTIntegrationPlugin.wrapError(e);
        } catch (MalformedURLException e) {
          throw SDBGJDTIntegrationPlugin.wrapError(e);
        } catch (IOException e) {
          throw SDBGJDTIntegrationPlugin.wrapError(e);
        }
      }
    } finally {
      subMonitor.done();
    }
  }

  protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
      throws CoreException {
    fullBuild(monitor);
  }

  private GWTSDMCodeServerAPI getCodeServerAPI(GWTSDMProperties properties, String moduleName) {
    try {
      return new GWTSDMCodeServerAPI(new URI(
          "http",
          null,
          properties.getCodeServerHost(),
          properties.getCodeServerPort(),
          "/",
          null,
          null), moduleName);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void notifyBuildCompleted(GWTSDMProperties properties, GWTSDMCodeServerAPI codeServerAPI)
      throws MalformedURLException, IOException, JSONException, CoreException {
    if (properties.getHotCodeReplacePolicy() != HotCodeReplacePolicy.DISABLED) {
      for (GWTSDMDOMResourceTracker tracker : GWTSDMDOMResourceTracker.getInitialized()) {
        if (getProject() != null && tracker.getProject() != null
            && getProject().getName().equals(tracker.getProject().getName())) {
          tracker.uploadLatestScript(properties, codeServerAPI);
        }
      }
    }
  }
}
