
name := "bike-elevation-service"
organization := "com.mapquest.interview"

version := "0.1"
scalaVersion := "2.11.8"
resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "io.spray"            %%  "spray-routing-shapeless2" % "1.3.3",
  "io.spray"            %%  "spray-routing"            % "1.3.3",
  "io.spray"            %%  "spray-can"                % "1.3.3",
  "io.spray"            %%  "spray-testkit"            % "1.3.3" % "test",
  "com.typesafe.akka"   %%  "akka-actor"               % "2.3.9",
  "com.typesafe.akka"   %%  "akka-testkit"             % "2.3.9" % "test",
  "org.scaldi"          %%  "scaldi"                   % "0.5.7",
  "org.specs2"          %%  "specs2-core"              % "3.7" % "test",
  "io.spray"            %%  "spray-client"             % "1.3.3",
  "io.spray"            %%  "spray-json"               % "1.3.2",
  "org.scalaj"          % "scalaj-http_2.8.1"          % "0.3.0"
)
