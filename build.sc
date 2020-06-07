// build.sc
import mill._, scalalib._, publish._

object scivs extends ScalaModule with PublishModule {
  def scalaVersion = "2.13.1"
  def publishVersion = "0.1.1"

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.1.2")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  } 
  def pomSettings = PomSettings(
    description = "Collection of datastructures for working with genomic intervals",
    organization = "io.github.sstadick",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("sstadick", "scivs"),
    developers = Seq(
      Developer("sstadick", "Seth Stadick","https://github.com/sstadick")
    )
  )
}