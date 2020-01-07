/** This is an implementation of an AIList, but with a dynamic scaling for the
  *  number of sublists. It is consistantly fast, regardless of nested intervals.
  *  Please see the original [[https://www.biorxiv.org/content/10.1101/593657v1 paper]]
  *  for more details.
  *
  *  ScAIList is composed of four primary parts. A main interval list, which holds
  *  all the intervals after they have been decomposed. A component index's list,
  *  which holds the start index of each sublist post-decomposition. A component
  *  lengths list, which holds the length of each component. And finally a maxEnds
  *  list, which holds the max end relative to a sublist up to a given point for
  *  each interval.
  *
  *  The decomposition step is achieved by walking the list of intervals and
  *  recursively (with a cap) extracting intervals that overlap a given number of
  *  other intervals with a certain distance from it. The unique development in
  *  this implementation is to make the cap dynamic.
  */
package ivstore.scailist
import scala.util.control.Breaks._
import scala.collection.AbstractIterator
import scala.collection.mutable.ArrayBuffer
import scala.math
import ivstore.util.Cursor
import ivstore.ivstore.IVStore
import ivstore.interval.Interval

/** Companion object for the ScAIList class. Implements the primary constructor */
object ScAIList {
  def apply[T](
      ivs: Seq[Interval[T]],
      minCovLen: Int = 20,
      overrideMinCompLen: Boolean = false
  ): ScAIList[T] = {
    val maxComps = math.floor(math.log(ivs.length.toFloat) / math.log(2))
    val minCov = minCovLen / 2

    var numComps = 0
    var compLens = ArrayBuffer[Int]()
    var compIdxs = ArrayBuffer[Int]()
    var maxEnds = ArrayBuffer[Int]()

    val minCompLen =
      if (!overrideMinCompLen) math.max(64, minCovLen) else minCovLen

    val inputLen = ivs.length
    var inputIntervals = ivs.sorted.toBuffer

    var decomposed = ArrayBuffer[Interval[T]]()

    if (inputLen <= minCompLen) {
      numComps = 1
      compLens += inputLen
      compIdxs += 0
      decomposed ++= inputIntervals
    } else {
      var currComp = 0
      while (currComp < maxComps && inputLen - decomposed.length > minCompLen) {
        var list1 = ArrayBuffer[Interval[T]]()
        var list2 = ArrayBuffer[Interval[T]]()
        for (i <- 0 until inputIntervals.length) {
          val interval = inputIntervals(i)
          var j = 1
          var cov = 1
          while (j < minCompLen && cov < minCov && j + i < inputIntervals.length) {
            if (inputIntervals(i + j).stop >= interval.stop)
              cov += 1
            j += 1
          }
          if (cov < minCov)
            list1 += inputIntervals(i)
          else
            list2 += inputIntervals(i)
        }

        compIdxs += decomposed.length
        compLens += list1.length
        currComp += 1

        if (list2.length <= minCompLen || currComp == maxComps - 2) {
          if (!list2.isEmpty) {
            decomposed ++= list1
            compIdxs += decomposed.length
            compLens += list2.length
            decomposed ++= list2
            currComp += 1
          }
        } else {
          decomposed ++= list1
          inputIntervals = list2
        }
      }
      numComps = currComp
    }

    for (j <- 0 until numComps) {
      val compStart = compIdxs(j)
      val compEnd = compStart + compLens(j)
      var maxEnd = decomposed(compStart).stop
      maxEnds += maxEnd
      for (iv <- decomposed.slice(compStart + 1, compEnd)) {
        if (iv.stop > maxEnd)
          maxEnd = iv.stop
        maxEnds += maxEnd
      }
    }
    new ScAIList(
      intervals = decomposed,
      numComps = numComps,
      compIdxs = compIdxs,
      compLens = compLens,
      maxEnds = maxEnds
    )
  }
}

/** ScAIList represents a decomposed list of intervals ordered by start position.
  *  @example
  *  {{{
  *  import ivstore.scailist.ScAIList
  *  import ivstore.interval.Interval
  *  val lapper = ScAIList((0 to 20 by 5).map(Interval(_, _ + 2, 0)).toList))
  *  assert(lapper.find(6, 11).toList(0), Interval(5, 7, 0))
  *  }}}
  */
case class ScAIList[T](
    intervals: ArrayBuffer[Interval[T]],
    numComps: Int,
    compLens: ArrayBuffer[Int],
    compIdxs: ArrayBuffer[Int],
    maxEnds: ArrayBuffer[Int]
) extends IVStore[T, ScAIListIter[T]] {

  def length(): Int = this.intervals.length
  def isEmpty(): Boolean = this.intervals.isEmpty

  /**
    *  Binary search to find the right most index where interval.start < query.stop
    */
  def upperBound(
      stop: Int,
      intervals: ArrayBuffer[Interval[T]]
  ): Option[Int] = {
    var right = intervals.length
    var left = 0

    if (intervals(right - 1).start < stop)
      return Some(right - 1)
    else if (intervals(left).start >= stop)
      return None

    while (right > 0) {
      val half = right / 2
      val otherHalf = right - half
      val probe = left + half
      val otherLeft = left + otherHalf
      val v = intervals(probe)
      right = half
      left = if (v.start < stop) otherLeft else left
    }

    // Guarded at the top from ending on either extreme
    if (intervals(left).start >= stop)
      Some(left - 1)
    else
      Some(left)
  }

  /** Returns an iterator over [[ivstore.interval.Interval]] that overlap the
    *  query `start until end`
    */
  def find(start: Int, stop: Int): ScAIListIter[T] = {
    new ScAIListIter(
      scailist = this,
      start = start,
      stop = stop,
      foundAt = -1,
      cursor = new Cursor(0),
      compNum = new Cursor(0),
      findOffset = true,
      breakNow = false
    )
  }
}

class ScAIListIter[T](
    scailist: ScAIList[T],
    start: Int,
    stop: Int,
    var foundAt: Int,
    cursor: Cursor,
    compNum: Cursor,
    var findOffset: Boolean,
    var breakNow: Boolean
) extends AbstractIterator[Interval[T]] {
  override def hasNext(): Boolean = {

    while (this.compNum.index < this.scailist.numComps) {
      breakable {

        val compStart = this.scailist.compIdxs(this.compNum.index)
        val compEnd = compStart + this.scailist.compLens(this.compNum.index)
        if (this.scailist.compLens(this.compNum.index) > 15) {
          if (this.findOffset) {
            this.cursor.index = this.scailist.upperBound(
              this.stop,
              this.scailist.intervals.slice(compStart, compEnd)
            ) match {
              case Some(n) => n
              case None => {
                this.compNum.index += 1
                this.findOffset = true
                break
              }
            }
            this.cursor.index += compStart
            this.findOffset = false
          }

          while (this.cursor.index >= compStart && this.scailist.maxEnds(
                   this.cursor.index
                 ) > this.start && !this.breakNow) {
            val interval = this.scailist.intervals(this.cursor.index)
            this.foundAt = this.cursor.index
            val tmp_offset = this.cursor.index - 1
            if (tmp_offset >= 0) {
              this.cursor.index = tmp_offset
            } else {
              this.breakNow = true
              this.cursor.index = 0
            }

            if (interval.stop > this.start)
              return true
          }
        } else { // straight line search
          while (this.cursor.index < compEnd) {
            val interval = this.scailist.intervals(this.cursor.index)
            this.foundAt = this.cursor.index
            this.cursor.index += 1
            if (interval.start < this.stop && interval.stop > this.start) {
              return true
            }
          }
        }
        this.breakNow = false
        this.findOffset = true
        this.compNum.index += 1
      }
    }
    false
  }

  override def next(): Interval[T] = {
    this.scailist.intervals(this.foundAt)
  }
}
