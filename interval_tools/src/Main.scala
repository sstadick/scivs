package interval_tools
import interval_tools.coverage.Coverage
// TODO: Switch to using `decline` for arg parsing

trait Tool {
  def execute(args: Array[String]): Unit
}

object Main {
  val usage = """
      Usage: ivtools [subcommand] [options]
    """
  def main(args: Array[String]) {
    if (args.length == 0) println(usage)
    args.head match {
      case "Coverage" => Coverage.execute(args.tail)
      case _          => ???
    }
  }
}
