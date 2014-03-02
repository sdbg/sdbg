# SDBG: Sourcemap Debugger

This is an Eclipse plugin designed to expose IDE debugging support for compiled JavaScript applications using sourcemaps.

It has been adapted from the [Dart](http://dartlang.org) project, and modified for use in the [GWT](http://gwtproject.org) project.

The current implementation can be considered alpha quality.

Any comments, bugs or issues should be directed to the [SDBG Google Group](https://groups.google.com/d/forum/sdbg).

## Installation

Eclipse 3.7 or later is required.

To install SDBG, use the following sequence of Eclipse commands: Help menu -> Install New Software... -> Add...
- (The "Add Repository" dialog will appear)
- For "Name", enter "SDBG" or something similar
- For "Location", fill-in the following URL: [https://github.com/sdbg/sdbg/raw/master/p2repo](https://github.com/sdbg/sdbg/raw/master/p2repo)

Alternatively, you can download the latest [SDBG P2 repository ZIP](https://github.com/sdbg/sdbg/releases) and install it by using the following sequence of Eclipse commands:
Help menu -> Install New Software... -> Add... -> Archive...

## Testing

Once you are running the plugin, you can test it using the gwtproject.zip file found in the root of the repository: 
- Import the project into your Eclipse workspace by using the following commands: File -> Import... -> Existing Projects into Workspace -> Next -> Select Archive file;
- Next, open the "Debug Configurations" dialog and invoke the "gwtproject" launch configuration which will be available under "Chrome launch" in the launchers' tree;
- Put a breakpoint in the method called "toggleMenu", in the (only) Java file in the gwtproject project - GWTProjectEntryPoint.java;  
- Click on "Articles" or "Documentation" in the "Table of contents" tree at http://gwtproject.org;
- Observe the breakpoint being hit

## Status

What is working:
- Launching an instance of Chrome for debugging
- Setting breakpoints in Java, including in JAR libraries
- Opening the Java (or other source language) source files instead of the obfuscated JavaScript

What is mostly working:
- Deobfuscating the stacktrace. Works but does not look 100% like e.g. the JDT Debugger stacktrace. For example, the methods are not deobfuscated to their Java equivalents
- Connecting to a running Chrome instance; works but is well hidden. Go in Eclipse Preferences and assign a shortcut to the Chrome Remote Connection Dialog

Future work:
- Prio A: Deobfuscating the Variable tabs. Currently, Eclipse will display the Variables tab with your JavaScript objects in them (not yet translated back to Java);
- Prio A-B: Support for other source languages besides Java (e.g. CoffeeScript, TypeScript etc.); should require relatively little effort, as the interfaces for implementing such support are now qwell defined;
- Prio A-B: Support for Safari, Opera and Mobile Safari; should be easy as all these currently use the same Remote Debugging protocol as Chrome;
- Prio B: Support for Internet Explorer and Firefox;

## Build Instructions

git clone git@github.com:sdbg/sdbg.git
(forgive the large download; we have retained all Dart history for posterity and access via eGit)

For development, you are recommended to simply import the root of the source as a maven project.  
Inside the com.github.sdbg.debug.ui project, you will see a launch configuration called SDBG.launch.

Running this will open a new instance of Eclipse with the debugger plugin installed.  
If you encounter any issues, please post to the [SDBG Google Group](https://groups.google.com/d/forum/sdbg)

You may also build a p2 repository and zip by running mvn install from the root of the project ($SDBG_DIRECTORY),  
and you can have tycho maven plugin create a fresh install of Eclipse, with sdbg installed by running:  
cd $SDBG_DIRECTORY/com.github.sdbg.releng.install; mvn install

You can run new Eclipse installation with ./target/eclipse/eclipse from the installer directory.

## Contributions

Any coding or testing you can contribute would be greatly appreciated!  
However, we do ask that if you want to commit code, please drop us a message on the Google Group;  
if you want to work on a feature, we can may already have a branch under way,  
or can at help you get caught up quickly.

## License

This project is released under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html), with copyright attribution to the Dart project authors.
