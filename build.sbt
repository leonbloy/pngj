organization := "ar.com.hjg"

name := "PNGJ"

version := "2.1.2-SNAPSHOT"

scalaVersion := "2.12.8"

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "UTF8")

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF8", "-feature", "-language:implicitConversions,postfixOps")

crossPaths := false

autoScalaLibrary := false

//libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "test"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
)
