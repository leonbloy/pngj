organization := "ar.com.hjg"

name := "PNGJ"

version := "2.2.0-SNAPSHOT"

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "UTF8")

crossPaths := false

autoScalaLibrary := false

/*
scalaVersion := "2.13.1"
scalacOptions ++= Seq("-deprecation", "-encoding", "UTF8")
libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "test"
*/

githubOwner := "alexdupre"
githubRepository := "pngj"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN") || TokenSource.GitConfig("github.token")

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)
