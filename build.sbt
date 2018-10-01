packageArchetype.java_application

name := "TimeAdmin"

version := "0.0.1"

organization := "hu.advancedweb"

scalaVersion := "2.11.7"

parallelExecution in Test := false

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                  "staging"       at "http://oss.sonatype.org/content/repositories/staging",
                  "releases"      at "http://oss.sonatype.org/content/repositories/releases")

unmanagedResourceDirectories in Test <+= baseDirectory { _ / "src/main/webapp" }

Seq(webSettings :_*)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Ywarn-unused-import")

libraryDependencies ++= {
  val liftVersion = "3.0.1"
  val cukesVersion = "1.2.2"
  Seq(
    "net.liftweb"                 %% "lift-webkit"              % liftVersion           % "compile",
    "net.liftweb"                 %% "lift-mapper"              % liftVersion           % "compile",
    "net.liftweb"                 %% "lift-testkit"             % liftVersion           % "compile,test",
    "net.liftmodules"             %% "lift-jquery-module_3.0"   % "2.9",
    "org.eclipse.jetty"           % "jetty-webapp"              % "8.1.7.v20120910"     % "compile,container,test,it,e2e",
    "org.eclipse.jetty"           % "jetty-plus"                % "8.1.7.v20120910"     % "compile,container,test,it,e2e",
    "org.eclipse.jetty.orbit"     % "javax.servlet"             % "3.0.0.v201112011016" % "compile,container,test,e2e" artifacts Artifact("javax.servlet", "jar", "jar"),
    "log4j"                       % "log4j"                     % "1.2.17",
    "org.slf4j"                   % "slf4j-log4j12"             % "1.7.21",
    "org.specs2"                  %% "specs2"                   % "3.7"                 % "test,it,e2e",
    "org.scalatest"               %% "scalatest"                % "2.2.4"               % "test,it,e2e",
    "org.seleniumhq.selenium"     % "selenium-firefox-driver"   % "3.14.0"              % "test,it,e2e",
    "info.cukes"                  %% "cucumber-scala"           % cukesVersion          % "e2e",
    "info.cukes"                  % "cucumber-junit"            % cukesVersion          % "e2e",
    "info.cukes"                  % "cucumber-picocontainer"    % cukesVersion          % "e2e",
    "junit"                       % "junit"                     % "4.12"                % "e2e",
    "com.novocode"                % "junit-interface"           % "0.10"                % "e2e",
    "com.h2database"              % "h2"                        % "1.3.167",
    "postgresql"                  % "postgresql"                % "8.4-701.jdbc4",
    "com.github.nscala-time"      %% "nscala-time"              % "2.14.0",
    "com.norbitltd"               %% "spoiwo"                   % "1.1.1",
    "org.apache.poi"              % "ooxml-schemas"             % "1.3",
    "com.ibm.icu"                 % "icu4j"                     % "58.2"
  )
}
