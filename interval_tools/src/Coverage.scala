package interval_tools.coverage

import interval_tools.Tool
import scivs._

object Coverage extends Tool {
  val usage = "Usage: ivtools Coverage -a bedfile -b bedfile"
  override def execute(args: Array[String]): Unit = {
    if (args.length != 4) {
      println(usage)
      System.exit(1)
    }
    val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "-a" :: value :: tail =>
          nextOption(map ++ Map('fileA -> value), tail)
        case "-b" :: value :: tail =>
          nextOption(map ++ Map('fileB -> value), tail)
        case _ => println(usage); System.exit(1); map;
      }
    }

    val options = nextOption(Map(), arglist)
    println(options)
  }
}
