package com.github.sdbg.integration.jdt.gwt;

import com.github.sdbg.integration.jdt.SDBGJDTIntegrationPlugin;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class GWTSDMNature implements IProjectNature {
  public static final String NATURE_ID = SDBGJDTIntegrationPlugin.PLUGIN_ID + ".gwtsdmnature";

  private IProject project;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#configure()
   */
  @Override
  public void configure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    ICommand[] commands = desc.getBuildSpec();

    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(GWTSDMBuilder.BUILDER_ID)) {
        return;
      }
    }

    ICommand[] newCommands = new ICommand[commands.length + 1];
    System.arraycopy(commands, 0, newCommands, 0, commands.length);
    ICommand command = desc.newCommand();
    command.setBuilderName(GWTSDMBuilder.BUILDER_ID);
    newCommands[newCommands.length - 1] = command;
    desc.setBuildSpec(newCommands);
    project.setDescription(desc, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#deconfigure()
   */
  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription description = getProject().getDescription();
    ICommand[] commands = description.getBuildSpec();
    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(GWTSDMBuilder.BUILDER_ID)) {
        ICommand[] newCommands = new ICommand[commands.length - 1];
        System.arraycopy(commands, 0, newCommands, 0, i);
        System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
        description.setBuildSpec(newCommands);
        project.setDescription(description, null);
        return;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#getProject()
   */
  @Override
  public IProject getProject() {
    return project;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
   */
  @Override
  public void setProject(IProject project) {
    this.project = project;
  }
}
