val V = new {
  val distage = "0.9.11"
  val zio = "1.0.0-RC16"
  val scalatest = "3.0.8"
  val catsTaglessMacros = "0.10"
}

val Deps = new {
  val distageCore = "io.7mind.izumi" %% "distage-core" % V.distage
  val zio = "dev.zio" %% "zio" % V.zio
  val catsTaglessMacros = "org.typelevel" %% "cats-tagless-macros" % V.catsTaglessMacros
  val scalaTest = "org.scalatest" %% "scalatest" % V.scalatest
}

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val `distage-zio-contramapping-service` = (project in file("."))
  .settings(
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      Deps.distageCore,
      Deps.zio,
      Deps.catsTaglessMacros,
      Deps.scalaTest % Test,
    )
  )
