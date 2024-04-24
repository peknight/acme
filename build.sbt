ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / organization := "com.peknight"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xfatal-warnings",
    "-language:strictEquality",
    "-Xmax-inlines:64"
  ),
)

lazy val acme = (project in file("."))
  .aggregate(
    acmeCore.jvm,
    acmeCore.js,
    acmeApp.jvm,
    acmeApp.js,
  )
  .settings(commonSettings)
  .settings(
    name := "acme",
  )

lazy val acmeCore = (crossProject(JSPlatform, JVMPlatform) in file("acme-core"))
  .settings(commonSettings)
  .settings(
    name := "acme-core",
    libraryDependencies ++= Seq(
    ),
  )

lazy val acmeApp = (crossProject(JSPlatform, JVMPlatform) in file("acme-app"))
  .dependsOn(acmeCore)
  .settings(commonSettings)
  .settings(
    name := "acme-app",
    libraryDependencies ++= Seq(
    ),
  )
