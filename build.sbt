name := """crypto-utility"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "crypto-utility" %% "crypto-utility-protocol" % "0.1.0" % Compile,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.10" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
)

libraryDependencies ++= Seq(
  "org.pegdown" % "pegdown" % "1.6.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

testOptions in Test ++= Seq(
  Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports/html"),
  Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/html")
)