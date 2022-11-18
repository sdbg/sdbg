/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.breakpoints.IBreakpointPathResolver;
import com.github.sdbg.debug.core.breakpoints.SDBGBreakpoint;
import com.github.sdbg.debug.core.internal.ScriptDescriptor;
import com.github.sdbg.debug.core.internal.util.FirefoxBrowser;
import com.github.sdbg.debug.core.internal.webkit.model.SourceMapManager;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitScriptStorage;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitLocation;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.Trace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.json.JSONException;

import de.exware.remotefox.DebugConnector;
import de.exware.remotefox.SourceActor;
import de.exware.remotefox.TabActor;

/**
 * Handle adding a removing breakpoints to the WebKit connection for the
 * WebkitDebugTarget class.
 */
public class BreakpointManager implements IBreakpointListener
{
    private static Collection<IBreakpointPathResolver> breakpointPathResolvers;

    private DebugConnector debugTarget;
    private FirefoxBrowser browser;

    private Map<IBreakpoint, List<String>> breakpointToIdMap = new HashMap<IBreakpoint, List<String>>();

    private Map<String, IBreakpoint> breakpointsToUpdateMap = new HashMap<String, IBreakpoint>();

    private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();
    private SourceMapManager sourceMapManager;
    private TabActor tab;

    static synchronized Collection<IBreakpointPathResolver> getBreakpointPathResolvers()
    {
        if (breakpointPathResolvers == null)
        {
            breakpointPathResolvers = new ArrayList<IBreakpointPathResolver>();

            IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
                .getExtensionPoint(IBreakpointPathResolver.EXTENSION_ID);
            for (IConfigurationElement element : extensionPoint.getConfigurationElements())
            {
                try
                {
                    breakpointPathResolvers.add((IBreakpointPathResolver) element.createExecutableExtension("class"));
                }
                catch (CoreException e)
                {
                    SDBGDebugCorePlugin.logError(e);
                }
            }

        }

        return breakpointPathResolvers;
    }

    public BreakpointManager(DebugConnector debugTarget, FirefoxBrowser browser, TabActor tab, SourceMapManager sourceMapManager)
    {
        this.debugTarget = debugTarget;
        this.browser = browser;
        this.sourceMapManager = sourceMapManager;
        this.tab = tab;
    }

//    @Override
//    public void addBreakpointsConcerningScript(IStorage script)
//    {
//        SourceMapManager sourceMapManager = debugTarget.getSourceMapManager();
//        for (IBreakpoint breakpoint : new ArrayList<IBreakpoint>(breakpointToIdMap.keySet()))
//        {
//            if (!isJSBreakpoint(breakpoint) && sourceMapManager.isMapTarget(script, getBreakpointPath(breakpoint)))
//            {
//                breakpointAdded(breakpoint);
//            }
//        }
//    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint)
    {
        try
        {
            addBreakpoint(breakpoint);
        }
        catch (IOException exception)
        {
            SDBGDebugCorePlugin.logError(exception);
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        // TODO: This is happening frequently, therefore scan the delta for
        // changes concerning us and only then do the breakpoint remove+add
        // trick

        // We generate this change event in the handleBreakpointResolved()
        // method - ignore one
        // instance of the event.
        if (ignoredBreakpoints.contains(breakpoint))
        {
            ignoredBreakpoints.remove(breakpoint);
            return;
        }

        breakpointRemoved(breakpoint, delta);
        breakpointAdded(breakpoint);
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        List<String> breakpointIds = breakpointToIdMap.remove(breakpoint);

        if (breakpointIds != null)
        {
            for (String breakpointId : breakpointIds)
            {
                breakpointsToUpdateMap.remove(breakpointId);

//                try
//                {
//                    debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(breakpointId);
//                }
//                catch (IOException exception)
//                {
//                    SDBGDebugCorePlugin.logError(exception);
//                }
            }
        }
    }

    public void connect() throws IOException, JSONException
    {
        tab.interrupt();
        IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
        for (IBreakpoint breakpoint : breakpoints)
        {
            addBreakpoint(breakpoint);
        }
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
        tab.resume();
    }

//    @Override
//    public void dispose(boolean deleteAll)
//    {
//        // Null check for when the editor is shutting down.
//        if (DebugPlugin.getDefault() != null)
//        {
//            if (deleteAll)
//            {
//                try
//                {
//                    for (List<String> ids : breakpointToIdMap.values())
//                    {
//                        if (ids != null)
//                        {
//                            for (String id : ids)
//                            {
//                                debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(id);
//                            }
//                        }
//                    }
//                }
//                catch (IOException exception)
//                {
//                    SDBGDebugCorePlugin.logError(exception);
//                }
//            }
//            DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
//        }
//    }

//    @Override
//    public IBreakpoint getBreakpointFor(WebkitLocation location)
//    {
//        try
//        {
//            IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager()
//                .getBreakpoints(SDBGDebugCorePlugin.DEBUG_MODEL_ID);
//
//            ScriptDescriptor script = debugTarget.getWebkitConnection().getDebugger().getScript(location.getScriptId());
//
//            if (script == null)
//            {
//                return null;
//            }
//
//            String url = script.getUrl();
//            int line = WebkitLocation.webkitToElipseLine(location.getLineNumber());
//
//            for (IBreakpoint bp : breakpoints)
//            {
//                if (bp instanceof ILineBreakpoint)
//                {
//                    ILineBreakpoint breakpoint = (ILineBreakpoint) bp;
//
//                    if (breakpoint.getLineNumber() == line)
//                    {
//                        String bpUrl = null;
//                        if (breakpoint instanceof SDBGBreakpoint)
//                        {
//                            SDBGBreakpoint sdbgBreakpoint = (SDBGBreakpoint) breakpoint;
//                            IFile file = sdbgBreakpoint.getFile();
//                            if (file != null)
//                            {
//                                bpUrl = getResourceResolver().getUrlForResource(file);
//                            }
//                            else
//                            {
//                                bpUrl = sdbgBreakpoint.getFilePath();
//                            }
//                        }
//                        else
//                        {
//                            bpUrl = getResourceResolver().getUrlForResource(breakpoint.getMarker().getResource());
//                        }
//
//                        if (bpUrl != null && bpUrl.equals(url))
//                        {
//                            return breakpoint;
//                        }
//                    }
//                }
//            }
//
//            return null;
//        }
//        catch (CoreException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }

//    @Override
//    public void handleBreakpointResolved(WebkitBreakpoint webkitBreakpoint)
//    {
//        try
//        {
//            IBreakpoint bp = breakpointsToUpdateMap.get(webkitBreakpoint.getBreakpointId());
//
//            if (bp != null && bp instanceof ILineBreakpoint)
//            {
//                ILineBreakpoint breakpoint = (ILineBreakpoint) bp;
//
//                int eclipseLine = WebkitLocation.webkitToElipseLine(webkitBreakpoint.getLocation().getLineNumber());
//
//                if (breakpoint.getLineNumber() != eclipseLine)
//                {
//                    ignoredBreakpoints.add(breakpoint);
//
//                    String message = "[breakpoint in "
//                        + (breakpoint instanceof SDBGBreakpoint ? ((SDBGBreakpoint) breakpoint).getName()
//                            : breakpoint.getMarker().getResource().getName())
//                        + " moved from line " + breakpoint.getLineNumber() + " to " + eclipseLine + "]";
////                    debugTarget.writeToStdout(message);
//
//                    breakpoint.getMarker().setAttribute(IMarker.LINE_NUMBER, eclipseLine);
//                }
//            }
//        }
//        catch (CoreException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }

//    @Override
//    public void handleGlobalObjectCleared()
//    {
//        for (IBreakpoint breakpoint : new ArrayList<IBreakpoint>(breakpointToIdMap.keySet()))
//        {
//            if (!isJSBreakpoint(breakpoint))
//            {
//                // This excercise is necessary so that the V8 breakpoints are
//                // removed
//                // and re-added later when the sourcemaps are re-parsed
//                breakpointRemoved(breakpoint, null/* delta */);
//                breakpointAdded(breakpoint);
//            }
//        }
//    }

//    @Override
//    public void removeBreakpointsConcerningScript(IStorage script)
//    {
//        SourceMapManager sourceMapManager = debugTarget.getSourceMapManager();
//        for (IBreakpoint breakpoint : new ArrayList<IBreakpoint>(breakpointToIdMap.keySet()))
//        {
//            if (!isJSBreakpoint(breakpoint) && sourceMapManager.isMapTarget(script, getBreakpointPath(breakpoint)))
//            {
//                breakpointRemoved(breakpoint, null/* delta */);
//            }
//        }
//    }

    private void addBreakpoint(final IBreakpoint bp) throws IOException
    {
        try
        {
            if (bp.isEnabled() && bp instanceof ILineBreakpoint)
            {
                final ILineBreakpoint breakpoint = (ILineBreakpoint) bp;

                addToBreakpointMap(breakpoint, null/* id */,
                    true/* trackChanges */);

                String path = getBreakpointPath(breakpoint);
                if (path != null)
                {
                    int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLineNumber());

                    if (isJSBreakpoint(breakpoint))
                    {
                        // Handle pure JavaScript breakpoints
                        trace("Set breakpoint [" + path + "," + line + "]");

//                        debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(null, path, line, -1,
//                            new WebkitCallback<String>()
//                            {
//                                @Override
//                                public void handleResult(WebkitResult<String> result)
//                                {
//                                    if (!result.isError())
//                                    {
//                                        addToBreakpointMap(breakpoint, result.getResult(), true);
//                                    }
//                                }
//                            });
                    }
                    else
                    {
                        // Handle source mapped breakpoints
                        SourceMapManager sourceMapManager = getSourceMapManager();
                        if (sourceMapManager.isMapTarget(path))
                        {
                            List<SourceMapManager.SourceLocation> locations = sourceMapManager
                                .getReverseMappingsFor(path, line);

                            for (SourceMapManager.SourceLocation location : locations)
                            {
                                String mappedPath;
                                if (location.getStorage() instanceof IFile)
                                {
                                    mappedPath = getResourceResolver()
                                        .getUrlRegexForResource((IFile) location.getStorage());
                                }
                                else if (location.getStorage() != null)
                                {
                                    mappedPath = location.getStorage().getFullPath().toPortableString();
                                }
                                else
                                {
                                    mappedPath = location.getPath();
                                }

                                if (mappedPath != null)
                                {
                                    trace("Breakpoint [" + path + ","
                                        + (breakpoint instanceof ILineBreakpoint ? breakpoint.getLineNumber() : "")
                                        + ",-1] ==> mapped to [" + mappedPath + "," + location.getLine() + ","
                                        + location.getColumn() + "]");
                                    trace("Set breakpoint [" + mappedPath + "," + location.getLine() + "]");

//                                    debugTarget.setBreakpointByUrl(null, mappedPath,
//                                        location.getLine(), location.getColumn(), new WebkitCallback<String>()
//                                        {
//                                            @Override
//                                            public void handleResult(WebkitResult<String> result)
//                                            {
//                                                if (!result.isError())
//                                                {
//                                                    addToBreakpointMap(breakpoint, result.getResult(), false);
//                                                }
//                                            }
//                                        });
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (CoreException e)
        {
            throw new IOException(e);
        }
        catch (JSONException e)
        {
            throw new IOException(e);
        }
    }

    private SourceMapManager getSourceMapManager() throws JSONException, IOException
    {
        List<SourceActor> sources = tab.getSourceActors();
        for(int i=0;i<sources.size();i++)
        {
            SourceActor source = sources.get(i);
            ScriptDescriptor script = new ScriptDescriptor(source.getActorId(), source.getURL(), source.getSourceMapURL(), false, 0, 0, 0, 0);
            IStorage storage = new WebkitScriptStorage(script, script.getScriptSource());
            sourceMapManager.handleScriptParsed(storage, script.getUrl(), script.getSourceMapURL());
        }
        return sourceMapManager;
    }

    private void addToBreakpointMap(IBreakpoint breakpoint, String id, boolean trackChanges)
    {
        synchronized (breakpointToIdMap)
        {
            if (breakpointToIdMap.get(breakpoint) == null)
            {
                breakpointToIdMap.put(breakpoint, new ArrayList<String>());
            }

            if (id != null)
            {
                breakpointToIdMap.get(breakpoint).add(id);

                if (trackChanges)
                {
                    breakpointsToUpdateMap.put(id, breakpoint);
                }
            }
        }
    }

    private String getBreakpointPath(IBreakpoint bp)
    {
        String path = null;
        for (IBreakpointPathResolver resolver : getBreakpointPathResolvers())
        {
            if (resolver.isSupported(bp))
            {
                try
                {
                    path = resolver.getPath(bp);
                }
                catch (CoreException e)
                {
                }

                if (path != null)
                {
                    break;
                }
            }
        }

        if (path == null)
        {
            if (bp instanceof SDBGBreakpoint)
            {
                IResource file = ((SDBGBreakpoint) bp).getFile();
                if (file != null)
                {
                    path = getResourceResolver().getUrlRegexForResource(file);
                }
                else
                {
                    path = ((SDBGBreakpoint) bp).getFilePath();
                }
            }
            else
            {
                path = getResourceResolver().getUrlRegexForResource(bp.getMarker().getResource());
            }
        }

        return path;
    }

    private IResourceResolver getResourceResolver()
    {
        return browser.getResourceResolver();
    }

    private boolean isJSBreakpoint(IBreakpoint breakpoint)
    {
        return breakpoint instanceof SDBGBreakpoint; // TODO: Extend
                                                     // IBreakpointPathResolver
                                                     // so that it has a say on
                                                     // that as well
    }

    private void trace(String message)
    {
        Trace.trace(Trace.BREAKPOINTS, message);
    }
}
