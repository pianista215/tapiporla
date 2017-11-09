name := "ibex35Microservice"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion = "2.4.19" //TODO: Upgrade to 2.5.6 where dependency conflicts are solved
val akkaHttpVersion = "10.0.10"
val elastic4sVersion = "5.6.0"
val jodaTimeWrapper = "2.18.0"

libraryDependencies ++= Seq(
  //Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

  //AkkaHttp
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  //Elastic4s
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-tcp" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-xpack-security" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test",
  "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion % "test",

  //Jodatime
  "com.github.nscala-time" %% "nscala-time" % jodaTimeWrapper
)

resolvers += "elasticsearch-releases" at "https://artifacts.elastic.co/maven"