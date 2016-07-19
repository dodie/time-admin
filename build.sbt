import NativePackagerKeys._

packageArchetype.java_application

name := "TimeAdmin"

version := "0.0.1"

organization := "net.liftweb"

scalaVersion := "2.11.2"

parallelExecution in Test := false

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                  "staging"       at "http://oss.sonatype.org/content/repositories/staging",
                  "releases"      at "http://oss.sonatype.org/content/repositories/releases")

unmanagedResourceDirectories in Test <+= baseDirectory { _ / "src/main/webapp" }

seq(webSettings :_*)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Ywarn-unused-import")

libraryDependencies ++= {
  val liftVersion = "2.6.2"
  Seq(
    "net.liftweb"                 %% "lift-webkit"              % liftVersion           % "compile",
    "net.liftweb"                 %% "lift-mapper"              % liftVersion           % "compile",
    "net.liftweb"                 %% "lift-testkit"             % liftVersion           % "compile,test",
    "net.liftmodules"             %% "lift-jquery-module_2.6"   % "2.8",
    "org.eclipse.jetty"           % "jetty-webapp"              % "8.1.7.v20120910"     % "compile,container,test,it,e2e",
    "org.eclipse.jetty"           % "jetty-plus"                % "8.1.7.v20120910"     % "compile,container,test,it,e2e",
    "org.eclipse.jetty.orbit"     % "javax.servlet"             % "3.0.0.v201112011016" % "compile,container,test,e2e" artifacts Artifact("javax.servlet", "jar", "jar"),
    "ch.qos.logback"              % "logback-classic"           % "1.0.6",
    "org.specs2"                  %% "specs2"                   % "2.3.12"              % "test,it,e2e",
    "org.scalatest"               % "scalatest_2.11"            % "2.2.4"               % "test,it,e2e",
    "org.seleniumhq.selenium"     % "selenium-firefox-driver"   % "2.53.1"              % "test,it,e2e",
    "info.cukes"                  % "cucumber-scala_2.11"       % "1.2.2"               % "e2e",
    "info.cukes"                  % "cucumber-junit"            % "1.2.2"               % "e2e",
    "info.cukes"                  % "cucumber-picocontainer"    % "1.2.2"               % "e2e",
    "junit"                       % "junit"                     % "4.11"                % "e2e",
    "com.novocode"                % "junit-interface"           % "0.10"                % "e2e",
    "com.h2database"              % "h2"                        % "1.3.167",
    "org.apache.poi"              % "poi"                       % "3.9",
    "postgresql"                  % "postgresql"                % "8.4-701.jdbc4"
  )
}
