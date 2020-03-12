name := "ei-backend"

version := "0.0.0"
scalacOptions ++= Seq("-deprecation", "-feature")

lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion = "2.6.3"
lazy val circeVersion = "0.12.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.jacobbishopxy",
      scalaVersion := "2.13.1"
    )),
    name := "scala-ei-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )
  )

enablePlugins(PackPlugin)
packGenerateWindowsBatFile := false
