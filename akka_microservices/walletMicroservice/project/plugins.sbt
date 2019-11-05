resolvers ++= Seq(
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)

val artimaVersion = "1.1.2"
val nativePackagerVersion = "1.3.1"

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % artimaVersion)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % nativePackagerVersion)
