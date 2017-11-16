resolvers ++= Seq(
  "elasticsearch-releases" at "https://artifacts.elastic.co/maven",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.2")