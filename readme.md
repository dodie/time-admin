*Timeadmin* is a simple web based tool for time tracking to measure the time spent
on various tasks by collaborators working on small or large projects.
From the collected data the tool can generate various reports to the user, such as
timeline of activities in a given day, or monthly summaries.

Generate monthly timesheets from the collected data, that can be personalized with
an Excel template.


## Getting started with development

Timeadmin based on the [Lift web framework](http://liftweb.net/) and the [Simple Build Tool (SBT)](http://www.scala-sbt.org/).
Various build tasks can be issued from the SBT shell (./sbt or sbt.bat in the project root directory).

To start the application with an embedded HTTP server issue the *container:start* command in the SBT prompt.
Timeadmin will be published to port 8080, and be available until the shell is closed or
the *container:stop* command is issued.

Before the start of the application, the dependency lookup and the compilation of the modified sources automatically happen.
To trigger the compilation manually, issue the *compile* command.

For IDE support, you can generate Eclipse project files with *eclipse*.
To start an interactive Scala shell, enter *console*. In this console
you can import Lift modules and your own code to experiment with.
Booting the Lift application is required if you’re using core Lift
infrastructures such as Mapper or accessing property files:
```
scala> new bootstrap.liftweb.Boot().boot
```

Timeadmin has a multitude of automated tests.
Unit tests check functions and independent classes or small clusters of classes.
Integration tests check the appropriate connection with frameworks and libraries.
End-to-End tests check the features overall, from browser to database.

- To execute all automated tests, use _*test-all*_.
- To execute just unit tests, use *test*.
- To execute just integration tests, use *it:test*.
- To execute just e2e tests, use *e2e:test*.

To build the WAR, use *clean compile package*.


### Application structure in nutshell
- scala/bootstrap.liftweb.Boot:
Configures the Timeadmin, eg. sets DB access based on the configuration and initializes the sitemap and available URLs.
Responsible for defining authorization rules at page level.
- scala/code.model._:
Data model classes. Timeadmin uses Schemifier.
- scala/code.service._:
Place of utilities, complex calculations and data queries and derived data representation.
- scala/code.snippet._:
Snippets are rendering components that provides dynamic data on the pages. Most of the snippets are stateless.
- webapp/admin, webapp/client, webapp/index.html and freshuser.html:
Page templates. Defines layout and snippets for each page.
- webapp/templates-hidden:
Page base templates and common templates, localization files.

Authorization rules are defined at page and URL level granularity,
services and snippets does not check for further permissions.
