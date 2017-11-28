name := "walletMicroservice"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion = "2.5.6"
val jodaTimeWrapper = "2.18.0"
val scalaTestVersion = "3.0.4"
val logbackVersion = "1.2.3"
val typeSafeLogging = "3.7.2"

libraryDependencies ++= Seq(
  //Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

  //Jodatime
  "com.github.nscala-time" %% "nscala-time" % jodaTimeWrapper,

  //Scalatest
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,

  //Logback
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime,

  //Typesafe logging
  "com.typesafe.scala-logging" %% "scala-logging" % typeSafeLogging
)

resolvers ++= Seq(
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)

enablePlugins(JavaAppPackaging)
