import sbt._
import Keys._

object FinagleIrc extends Build {
  val libVersion = "6.18.0"

  val baseSettings = Defaults.defaultSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % libVersion,
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      "junit" % "junit" % "4.8.1" % "test"
    )
  )

  lazy val buildSettings = Seq(
    organization := "com.github.finagle",
    version := libVersion,
    crossScalaVersions := Seq("2.9.2", "2.10.0")
  )

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishArtifact := true,
    publishTo := Some(Resolver.file("localDirectory", file(Path.userHome.absolutePath + "/workspace/mvn-repo"))),
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/finagle/finagle-irc")),
    pomExtra := (
      <scm>
        <url>git://github.com/finagle/finagle-irc.git</url>
        <connection>scm:git://github.com/finagle/finagle-irc.git</connection>
      </scm>
        <developers>
          <developer>
            <id>sprsquish</id>
            <name>Jeff Smick</name>
            <url>https://github.com/sprsquish</url>
          </developer>
        </developers>)
  )

  def finProject(n: String): Project = {
    val name = "finagle-" + n
    Project(
      id = name,
      base = file(name),
      settings =
        Defaults.itSettings ++
        baseSettings ++
        buildSettings ++
        publishSettings
    ).configs(IntegrationTest)
  }

  lazy val finagleIrcRoot = Project(
    id = "finagle-irc-root",
    base = file("."),
    settings = Project.defaultSettings
  ).aggregate(finagleIrc, finagleIrcServer)

  lazy val finagleIrc =
    finProject("irc")

  lazy val finagleIrcServer =
    finProject("irc-server").settings(
      resolvers += "twttr" at "http://maven.twttr.com/",
      libraryDependencies ++= Seq(
        "com.twitter" %% "twitter-server" % "1.6.1")
    ).dependsOn(finagleIrc)
}

