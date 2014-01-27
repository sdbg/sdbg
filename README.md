* SDBG: Sourcemap Debugger *

This is an eclipse plugin designed to expose IDE debugging support for compiled javascript applications using sourcemaps.

It has been adapted from the [Dart](http://dartlang.org) project, and modified for use in the [Gwt](http://gwtproject.org) project.

The current implementation is a proof of concept;  
you can set a breakpoint in java, launch an instance of chrome, and hit that breakpoint in Eclipse.

Eclipse will display the Variables tab with your javascript objects in them (not yet translated back to java),  
the stack trace will be the same stack as javascript (not yet translated back to java),  
and stepping into or over code will, you guessed it, be the same as if you'd pressed the button in chrome debugger.

** Instructions **

git clone git@github.com:sdbg/sdbg.git
(forgive the large download; we have retained all Dart history for posterity and access via eGit)

For development, you are recommended to simply import the root of the source as a maven project.  
Inside the com.github.sdbg.debug.ui project, you will see a launch configuration called SDBG.launch.

Running this will open a new instance of eclipse with the debugger plugin installed.  
If you encounter any issues, please post to the [SDBG Google Group](https://groups.google.com/d/forum/sdbg)

You may also build a p2 repository and zip by running mvn install from the root of the project,  
and you can have tycho maven plugin create a fresh install of eclipse, with sdbg installed by running:  
cd $SDBG_DIRECTORY/com.github.sdbg.releng.install; mvn install

** Contributions **

Any coding or testing you can contribute would be greatly appreciated!  
However, we do ask that if you want to commit code, please drop us a message on the Google Group;  
if you want to work on a feature, we can may already have a branch under way,  
or can at help you get caught up quickly.

** License **

This project is released under the [Dart License](https://code.google.com/p/dart/source/browse/trunk/LICENSE), which is a New BSD License.
