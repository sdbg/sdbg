# Eclipse debugger for GWT SuperDevMode

**(and for JavaScript with Sourcemaps in general)**
 
This is an Eclipse plugin designed to expose IDE debugging support for compiled JavaScript applications using [Sourcemaps](http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/).

The primary target is to support debugging of [GWT](http://gwtproject.org) apps running in [SuperDevMode](http://www.gwtproject.org/articles/superdevmode.html), but the plugin core is language-agnostic and can support other compile-to-JavaScript languages in future.
 
For downloads, installation, status updates and other usage information, please visit the SDBG [website](http://sdbg.github.io).   

Any comments should be directed to the SDBG [Discussions](https://github.com/sdbg/sdbg/discussions)
Any bugs or issues should be directed to the SDBG [Issues](https://github.com/sdbg/sdbg/issues).


## Build Instructions

```
git clone git@github.com:sdbg/sdbg.git
```

(forgive the large download; we have retained all Dart history for posterity and access via eGit)

You may use the following parameters to reduce the size drastically:
```
git clone --filter=blob:none egit@github.com:sdbg/sdbg.git
```

For development, you are recommended to import the root of the source as a Maven (Eclipse M4E) project.

Inside the **com.github.sdbg.debug.ui project**, you will see a launch configuration called **SDBG.launch**.
Running this will open a new instance of Eclipse with the debugger plugin installed.

To make a full build and create an update-site use the following command:

```
on Linux: sh nobuto.sh
```

```
on Windows: nobuto.bat
```

The above command will put the update site into the folder **update-site**.

Using maven to build may still work, but is no longer supported. Any bugs reported because of maven fails will be ignored.

## Contributions

Any coding or testing you can contribute would be greatly appreciated!

## License

This project is released under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html), with copyright attribution to the [Dart](http://dartlang.org) project authors.

The attribution is necessary, because the original codebase was forked-off from the Dart project. Many thanks to the Dart guys for the splendid code quality and their support!
