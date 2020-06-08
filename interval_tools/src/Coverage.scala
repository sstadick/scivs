package interval_tools.coverage

import interval_tools.Tool
import java.io._
import scivs.scivs.Scivs
import scivs.interval.Interval
import scivs.lapper.Lapper
import scala.io.Source

object Coverage extends Tool {
  val usage = "Usage: ivtools Coverage -a bedfile -b bedfile"
  override def execute(args: Array[String]): Unit = {
    if (args.length != 4) {
      println(usage)
      System.exit(1)
    }
    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

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
    val out = new BufferedWriter(new OutputStreamWriter(System.out))

    // Read file A into a lapper
    val lappers = Source
      .fromFile(options('fileA))
      .getLines
      .map(line => {
        val Array(chr: String, start, stop, _*) = line.split("\t")
        (chr, Interval(start.toInt, stop.toInt, None))
      })
      .toList
      .groupBy(_._1)
      .map { case (k, v) => (k, new Lapper(v.map(_._2))) }

    // Stream file B
    Source
      .fromFile(options('fileB))
      .getLines
      .foreach(line => {
        val Array(chr: String, start, stop, _*) = line.split("\t")
        lappers.get(chr) match {
          case Some(lapper) => {
            var (cov, cov_st, cov_en, n) = (0, 0, 0, 0)
            var (st1, en1) = (start.toInt, stop.toInt)
            lapper
              .find(st1, en1)
              .foreach(iv => {
                n += 1
                // calculate the overlap length / coverage
                var (st0, en0) = (iv.start, iv.stop)
                if (st0 < st1) st0 = st1
                if (en0 > en1) en0 = en1
                if (st0 > cov_en) { // no overlap with previous found intervals
                  // set coverage to current interval
                  cov += cov_en - cov_st
                  cov_st = st0
                  cov_en = en0
                } else if (cov_en < en0) {
                  cov_en = en0 // overlap with previous found intervals
                }
              })
            cov += cov_en - cov_st
            out.write(s"$chr\t$start\t$stop\t$n\t$cov\n")
          }
          case None => out.write(s"$chr\t$start\t$stop\t0\t0\n")
        }
      })
    out.flush()
  }

  def calcCov(
      lapper: Lapper[None.type],
      chr: String,
      start: Int,
      stop: Int
  ): Unit = {
    var (cov, cov_st, cov_en, n) = (0, 0, 0, 0)
    lapper
      .find(start, stop)
      .foreach(iv => {
        n += 1
        // calculate the overlap length / coverage
        var (st0, en0) = (iv.start, iv.stop)
        if (st0 < start) st0 = start
        if (en0 > stop) en0 = stop
        if (st0 > cov_en) { // no overlap with previous found intervals
          // set coverage to current interval
          cov += cov_en - cov_st
          cov_st = st0
          cov_en = en0
        } else if (cov_en < en0) {
          cov_en = en0 // overlap with previous found intervals
        }
      })
    cov += cov_en - cov_st
    println(s"$chr\t$start\t$stop\t$n\t$cov")
  }
}
