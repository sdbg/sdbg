package com.github.sdbg.integration.jdt.gwt.build;

import com.github.sdbg.debug.core.model.IDOMResourceReference;
import com.github.sdbg.debug.core.model.IDOMResourceTracker;
import com.github.sdbg.debug.core.model.IDOMResources;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

public class GWTSDMDOMResourceTracker implements IDOMResourceTracker {
  private static Collection<GWTSDMDOMResourceTracker> trackers = new HashSet<GWTSDMDOMResourceTracker>();

  private IProject project;
  private IDOMResources domResources;

  public static Collection<GWTSDMDOMResourceTracker> getInitialized() {
    return new HashSet<GWTSDMDOMResourceTracker>(trackers);
  }

  public GWTSDMDOMResourceTracker() {
  }

  @Override
  public void dispose() {
    project = null;
    domResources = null;
    trackers.remove(this);
  }

  public IProject getProject() {
    return project;
  }

  @Override
  public void initialize(IProject project, IDOMResources domResources) {
    this.project = project;
    this.domResources = domResources;
    trackers.add(this);
  }

  public void uploadLatestScript(GWTSDMProperties properties, GWTSDMCodeServerAPI codeServerAPI)
      throws MalformedURLException, IOException, JSONException, CoreException {

    String modulePath = codeServerAPI.getModule() + "-0.js";
    for (IDOMResourceReference ref : domResources.getResources()) {
      if (ref.getType() == IDOMResourceReference.Type.SCRIPT && ref.getUrl().equals(modulePath)) {
        switch (properties.getHotCodeReplacePolicy()) {
          case DISABLED:
            // Do nothing
            break;
          case RELOAD_PAGE:
            domResources.reload();
            break;
          case CHROME_LIVE_EDIT: {
            Reader script = codeServerAPI.getCompiledScript();

            if (script != null) {
              try {
                domResources.uploadNewSource(ref, script);
              } finally {
                script.close();
              }
            }

            break;
          }
          default:
            throw new IllegalArgumentException("Unknown hot code replace policy: "
                + properties.getHotCodeReplacePolicy());
        }
      }
    }
  }
}
