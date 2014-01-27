package com.github.sdbg.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(
    defaultPhase=LifecyclePhase.PREPARE_PACKAGE, 
    name = "uninstall"
)
public class UninstallMojo extends AbstractMojo {

  @Parameter(property="eclipse.destination")
  String eclipseDestination;
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    // Lookup the name of our feature from the p2 generated folder,
    // so we can extract the version number and de-install it.
  }

}
