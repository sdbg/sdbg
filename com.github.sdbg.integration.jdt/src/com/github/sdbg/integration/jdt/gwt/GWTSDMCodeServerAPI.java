package com.github.sdbg.integration.jdt.gwt;

import com.github.sdbg.utilities.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class GWTSDMCodeServerAPI {
  private URI codeServerUri;
  private String module;

  public GWTSDMCodeServerAPI(URI codeServerUri, String module) {
    this.codeServerUri = codeServerUri;
    this.module = module;
  }

  public URI getCodeServerUri() {
    return codeServerUri;
  }

  public Reader getCompiledScript() throws MalformedURLException, IOException, JSONException {
    String compilationMappings = getString(URIUtil.append(
        URIUtil.append(codeServerUri, module),
        "compilation-mappings.txt"));
    if (compilationMappings != null) {
      String[] lines = compilationMappings.split("\\n");
      if (lines.length > 0) {
        String scriptName = lines[0].trim();
        return openReader(URIUtil.append(URIUtil.append(codeServerUri, module), scriptName));
      }
    }

    return null;
  }

  public String getLog() throws MalformedURLException, IOException, JSONException {
    return getString(URIUtil.append(codeServerUri, "/log"));
  }

  public String getModule() {
    return module;
  }

  public JSONObject progress() throws MalformedURLException, IOException, JSONException {
    return call(URIUtil.append(codeServerUri, "/progress"));
  }

  public JSONObject recompile(IProgressMonitor monitor) throws MalformedURLException, IOException,
      JSONException {
    final SubMonitor subMonitor = SubMonitor.convert(monitor);
    subMonitor.beginTask("Running GWT SDM Recompiler", 1);

    try {
      final boolean[] exitFlag = new boolean[] {false};
      final Object syncMonitor = new Object();

      Thread progressThread = new Thread("GWT SDM Recompiler Progress") {
        @Override
        public synchronized void run() {
          try {
            trackProgress(subMonitor, syncMonitor, exitFlag);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      };

      progressThread.start();

      try {
        return recompile(module);
      } finally {
        exitFlag[0] = true;

        try {
          progressThread.join();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    } finally {
      subMonitor.worked(1);
      subMonitor.done();
    }
  }

  public JSONObject recompile(String module) throws MalformedURLException, IOException,
      JSONException {
    return call(URIUtil.append(codeServerUri, "/recompile/" + module));
  }

  private JSONObject call(URI uri) throws MalformedURLException, IOException, JSONException {
    return new JSONObject(getString(uri));
  }

  private String getString(URI uri) throws MalformedURLException, IOException, JSONException {
    Reader reader = openReader(uri);
    if (reader != null) {
      try {
        return Streams.load(reader);
      } finally {
        reader.close();
      }
    } else {
      return null;
    }
  }

  private HttpURLConnection openConnection(URI uri) throws MalformedURLException, IOException,
      JSONException {
    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();

    con.setDoOutput(false);
    con.setDoInput(true);
    //con.setConnectTimeout(connectTimeout);
    //con.setReadTimeout(readTimeout);

    con.setRequestMethod("GET");

    con.connect();

    int retcode = con.getResponseCode();
    if (retcode < 200 || retcode > 299) {
      throw new IOException("Received HTTP Response " + retcode);
    }

    return con;
  }

  private Reader openReader(URI uri) throws MalformedURLException, IOException, JSONException {
    HttpURLConnection con = openConnection(uri);

    InputStream in = con.getInputStream();
    if (in != null) {
      String contentEncoding = con.getContentEncoding();
      return contentEncoding != null ? new InputStreamReader(con.getInputStream(), contentEncoding)
          : new InputStreamReader(con.getInputStream());
    } else {
      return null;
    }
  }

  private void trackProgress(SubMonitor progressMonitor, Object syncMonitor, boolean[] exitFlag)
      throws InterruptedException {
    synchronized (syncMonitor) {
      long time = System.currentTimeMillis();
      while (!exitFlag[0]) {
        long newTime = System.currentTimeMillis();
        if (newTime - time >= 500L) {
          time = newTime;

          try {
            JSONObject progress = progress();
            if ("compiling".equals(progress.getString("status"))) {
              String message = progress.getString("message");
              if (message == null) {
                message = "Compiling";
              }

              String module = progress.getString("inputModule");
              if (module == null) {
                module = getModule();
              }

              progressMonitor.subTask(message + " module " + module);
            } else {
              break;
            }
          } catch (IOException e) {
            break;
          } catch (JSONException e) {
            break;
          }
        }

        syncMonitor.wait(100);
      }
    }
  }
}
