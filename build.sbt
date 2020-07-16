name := "ei-backend"

version := "0.0.5"
scalacOptions ++= Seq("-deprecation", "-feature")


val scalaV = "2.13.1"

val akkaHttpVersion = "10.1.11"
val akkaVersion = "2.6.3"
val lbV = "1.2.3"
val h2V = "1.4.196"
val pgV = "42.2.9"
val slickV = "3.3.2"
val mongoV = "2.9.0"
val corsV = "0.4.2"
val bfV = "3.9.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.jacobbishopxy",
      scalaVersion := scalaV
    )),
    name := "scala-ei-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % lbV,

      "com.h2database" % "h2" % h2V,
      "org.postgresql" % "postgresql" % pgV,
      "com.typesafe.slick" %% "slick" % slickV,
      "org.mongodb.scala" %% "mongo-scala-driver" % mongoV,

      "ch.megard" %% "akka-http-cors" % corsV,

      "com.github.pathikrit" %% "better-files-akka" % bfV,
    )
  )

enablePlugins(PackPlugin)
packGenerateWindowsBatFile := false
