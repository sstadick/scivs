// build.sc
import mill._, scalalib._, publish._

trait CommonModule extends ScalaModule {
  def scalaVersion = "2.13.1"
  object test extends Tests {
    def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:3.1.2",
        ivy"org.scalactic::scalactic:3.1.2"
      )
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}

object interval_tools extends ScalaModule with CommonModule {
  def publishVersion = "0.1.0"
  def moduleDeps = Seq(scivs)
}

object scivs extends ScalaModule with PublishModule with CommonModule {
  def publishVersion = "0.1.1"

  def pomSettings = PomSettings(
    description =
      "Collection of datastructures for working with genomic intervals",
    organization = "io.github.sstadick",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("sstadick", "scivs"),
    developers = Seq(
      Developer("sstadick", "Seth Stadick", "https://github.com/sstadick")
    )
  )

}
