name := "interlude"

version := "1.0"

scalaVersion := "2.9.2"

resolvers ++= Seq("jboss repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/")

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

//libraryDependencies += "org.scala-lang" % "scala-actors" % "2.9.2"

//libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.9.2"

libraryDependencies ++= Seq("org.jboss.netty" % "netty" % "3.2.7.Final")
