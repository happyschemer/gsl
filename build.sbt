import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import org.scalajs.sbtplugin.cross.CrossType.Full

val clientProjId = "web"
val serverProjId = "service"
val akkaVer = "2.4.7"

lazy val MyCrossType = new CrossType {
  override def projectDir(crossBase: File, projectType: String) = Full.projectDir(crossBase,
    projectType match {
      case "js" => clientProjId
      case "jvm" => serverProjId
    }
  )
  override def sharedSrcDir(projectBase: File, conf: String) = Full.sharedSrcDir(projectBase, conf)
}

lazy val fu = CrossProject(serverProjId, clientProjId, file("."), MyCrossType).
  settings(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.4.3",
      "com.lihaoyi" %%% "utest" % "0.4.3" % "test"  // todo - if this is good for service side as well?
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  ).
  jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVer,
      "com.typesafe.akka" %% "akka-persistence" % akkaVer,
      "com.typesafe.akka" %% "akka-http-core" % akkaVer,
      "com.typesafe.akka" %%% "akka-http-experimental" % akkaVer,
      "org.iq80.leveldb" % "leveldb" % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
    )
  ).
  jsSettings(
    unmanagedResourceDirectories in Compile += (sourceDirectory in Compile).value / "js",
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    jsEnv := PhantomJSEnv().value,
    skip in packageJSDependencies := false,
    jsDependencies ++= Seq(
      RuntimeDOM,
      ProvidedJS / "jquery-3.2.1.js"
    ),
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
    )
  )

lazy val service = fu.jvm.settings(

  (managedResources in Compile) ++= {
    val sjs = (fastOptJS in (web, Compile)).value.data
    Seq(
      (packageJSDependencies in (web, Compile)).value,
      sjs,
      file(sjs + ".map"),
      (packageScalaJSLauncher in (web, Compile)).value.data
    )
  },

  (unmanagedResourceDirectories in Compile) ++=
    (unmanagedResourceDirectories in (web, Compile)).value,

  (excludeFilter in (Compile, unmanagedResources)) ~= {
    _ || "*.js"
  }

)

lazy val web = fu.js



