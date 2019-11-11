val Deps = new {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
}

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-distage",
    libraryDependencies += Deps.scalaTest % Test
  )
