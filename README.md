# SDBG: Sourcemap Debugger

This is an eclipse plugin designed to expose IDE debugging support for compiled javascript applications using sourcemaps.

It has been adapted from the [Dart](http://dartlang.org) project, and modified for use in the [GWT](http://gwtproject.org) project.

The current implementation is a proof of concept;  
you can set a breakpoint in java, launch an instance of chrome, and hit that breakpoint in Eclipse.

Eclipse will display the Variables tab with your javascript objects in them (not yet translated back to java),  
the stack trace will be the same stack as javascript (not yet translated back to java),  
and stepping into or over code will, you guessed it, be the same as if you'd pressed the button in chrome debugger.

## Install Instructions

git clone git@github.com:sdbg/sdbg.git
(forgive the large download; we have retained all Dart history for posterity and access via eGit)

For development, you are recommended to simply import the root of the source as a maven project.  
Inside the com.github.sdbg.debug.ui project, you will see a launch configuration called SDBG.launch.

Running this will open a new instance of eclipse with the debugger plugin installed.  
If you encounter any issues, please post to the [SDBG Google Group](https://groups.google.com/d/forum/sdbg)

You may also build a p2 repository and zip by running mvn install from the root of the project ($SDBG_DIRECTORY),  
and you can have tycho maven plugin create a fresh install of eclipse, with sdbg installed by running:  
cd $SDBG_DIRECTORY/com.github.sdbg.releng.install; mvn install

You can run new eclipse installation with ./target/eclipse/eclipse from the installer directory.

## Usage Instructions

Once you are running the plugin, you can test it using the gwtproject.zip file found in the root of the repository.  
Simply unzip this project into your workspace, and import it.  
Next, create a new "Chrome launch" configuration with project = gwtproject and URL = http://gwtproject.org;  
Put a breakpoint in the method called "toggleMenu", in the (only) Java file in the gwtproject project - GWTProjectEntryPoint.java;  
Click on "Articles" or "Documentation" in the "Table of contents" tree at http://gwtproject.org;  
Observe the breakpoint being hit

Any comments, bugs or issues should be directed to the [SDBG Google Group](https://groups.google.com/d/forum/sdbg).

## Contributions

Any coding or testing you can contribute would be greatly appreciated!  
However, we do ask that if you want to commit code, please drop us a message on the Google Group;  
if you want to work on a feature, we can may already have a branch under way,  
or can at help you get caught up quickly.

## License

This project is released under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html), with copyright attribution to the Dart project authors.
