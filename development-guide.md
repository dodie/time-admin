Getting started
---------------

Timeadmin is based on the [Lift web framework](http://liftweb.net/).

First, download SBT [Simple Build Tool (SBT) version 0.13.18](http://www.scala-sbt.org/).

Various build tasks can be issued from the *SBT shell*. To start that, run `sbt` in the
project root directory.

To start the application with an embedded HTTP server issue the ```container:start```
command in the *SBT prompt*. Timeadmin will be published to port 8080, and be available
until the shell is closed or the ```container:stop``` command is issued.

Before the start of the application, the dependency lookup and the compilation of the
modified sources will automatically happen. To trigger the compilation manually, issue
the ```compile``` command.


IDE Support, Interactive Console
--------------------------------

For IDE support, you can generate Eclipse project files with the ```eclipse``` command
in the *SBT shell*. To start an interactive Scala shell, enter ```console```. In this
console you can import Lift modules and your own code to experiment with. Booting the
Lift application is required if you’re using core Lift infrastructures such as Mapper
or accessing property files:

```
scala> new bootstrap.liftweb.Boot().boot
```


Test suite
----------

Timeadmin has a multitude of automated tests. Unit tests (*test scope*) check functions
and independent classes or small clusters of classes without initializing Lift or accessing
the database. Integration tests (*it scope*) check whole services and modules, so these
tests are using the database and check parts of the rendering by running snippet code.
End-to-End (*e2e scope*) tests exercises the whole application through a real browser.

The tests can be started with the following goals in the SBT shell:

- To execute **all automated tests**, use ```test-all```.
- To execute **unit tests**, use ```test```.
- To execute **integration tests**, use ```it:test```.
- To execute **e2e tests**, use ```e2e:test```.


Packaging
---------
To build the WAR, use *clean compile package*. You can find the assembled package in the
target directory.


Running
-------
Issue the following command to create a WAR file:

```
sbt clean compile package
```

You can deploy it to a standard Web container. Timeadmin is tested with the latest
versions of Tomcat.


Application structure in nutshell
---------------------------------

- scala/bootstrap.liftweb.Boot:
Configures the Timeadmin, eg. sets DB access based on the configuration and initializes the
sitemap and available URLs. Responsible for defining authorization rules at the page level.
- scala/code.model._:
Data model classes. Timeadmin uses Schemifier.
- scala/code.service._:
Place of utilities, complex calculations, and data queries and derived data representation.
- scala/code.snippet._:
Snippets are rendering components that provide dynamic data on the pages. Most of the
snippets are stateless.
- webapp/admin, webapp/client, webapp/index.html and freshuser.html:
Page templates. Defines layout and snippets for each page.
- webapp/templates-hidden:
Page base templates and common templates, localization files.

Authorization rules are defined at page and URL level granularity,
services and snippets generally do not check for further permissions.
