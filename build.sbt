import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "io.github.sstadick"
ThisBuild / organizationName := "sstadick"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/sstadick/scivs"),
    "https://github.com/sstadick/scivs.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "sstadick",
    name = "Seth Stadick",
    email = "sstadick@gmail.com",
    url = url("https://github.com/sstadick")
  )
)

// Sonatype build
useGpg := true
ThisBuild / description := "Collection of datastructures for working with genomic intervals"
ThisBuild / homepage := Some(url("https://github.com/sstadick/scivs"))
ThisBuild / pomIncludeRepository := { _ =>
  false
}

ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

lazy val root = (project in file("."))
  .settings(
    name := "scivs",
    libraryDependencies += scalaTest % Test,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
