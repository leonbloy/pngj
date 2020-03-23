organization := "ar.com.hjg"

name := "PNGJ"

version := "2.2.0-SNAPSHOT"

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "UTF8")

crossPaths := false

autoScalaLibrary := false

/*
scalaVersion := "2.12.8"
scalacOptions ++= Seq("-deprecation", "-encoding", "UTF8")
libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "test"
*/

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
)
