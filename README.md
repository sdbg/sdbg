# Eclipse debugger for GWT SuperDevMode

**(and for JavaScript with Sourcemaps in general)**
 
This is an Eclipse plugin designed to expose IDE debugging support for compiled JavaScript applications using [Sourcemaps](http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/).

The primary target is to support debugging of [GWT](http://gwtproject.org) apps running in [SuperDevMode](http://www.gwtproject.org/articles/superdevmode.html), but the plugin core is language-agnostic and can support other compile-to-JavaScript languages in future.
 
For downloads, installation, status updates and other usage information, please visit the SDBG [website](http://sdbg.github.io).   

Any comments, bugs or issues should be directed to the SDBG [Forums](https://groups.google.com/d/forum/sdbg).

## Build Instructions

git clone git@github.com:sdbg/sdbg.git
(forgive the large download; we have retained all Dart history for posterity and access via eGit)

For development, you are recommended to simply import the root of the source as a maven project.  
Inside the com.github.sdbg.debug.ui project, you will see a launch configuration called SDBG.launch.

Running this will open a new instance of Eclipse with the debugger plugin installed.  
If you encounter any issues, please post to the SDBG [Forum](https://groups.google.com/d/forum/sdbg)

You may also build a p2 repository and zip by running mvn install from the root of the project ($SDBG_DIRECTORY),  
and you can have tycho maven plugin create a fresh install of Eclipse, with sdbg installed by running:  
cd $SDBG_DIRECTORY/com.github.sdbg.releng.install; mvn install

You can run new Eclipse installation with ./target/eclipse/eclipse from the installer directory.

## Contributions

Any coding or testing you can contribute would be greatly appreciated!  
However, we do ask that if you want to commit code, please drop us a message on the Google Group;  
if you want to work on a feature, we may already have a branch under way, or can at help you get caught up quickly.

## License

This project is released under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html), with copyright attribution to the [Dart](http://dartlang.org) project authors.

The attribution is necessary, because the original codebase was forked-off from the Dart project. Many thanks to the Dart guys for the splendid code quality and their support!
