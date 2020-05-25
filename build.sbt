organization := "com.alexdupre"

name := "PNGJ"

version := "2.1.2.1"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:deprecation", "-encoding", "UTF8")
javacOptions in (Compile, doc) := Seq("-encoding", "UTF8")

crossPaths := false

autoScalaLibrary := false

/*
scalaVersion := "2.13.2"
scalacOptions ++= Seq("-deprecation", "-encoding", "UTF8")
libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "test"
*/

/*
githubOwner := "alexdupre"
githubRepository := "pngj"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN") || TokenSource.GitConfig("github.token")
*/

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)

publishTo := sonatypePublishToBundle.value

licenses := Seq("Apache License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting("alexdupre", "pngj", "Alex Dupre", "ale@FreeBSD.org"))
