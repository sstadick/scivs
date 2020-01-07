/** This is a scala port of [[https://github.com/brentp/nim-lapper nim-lapper]]
  *  There are a few notable differences such as the find and seek methods
  *  returing iterators.
  *
  *  Lapper works well for most interval data that does not include very long
  *  long intervals that engulf a majority of other intervals. In typical genomic
  *  data it is very fast. If you have lots of nested intervals see [[scivs.scailist]].
  */
package scivs.lapper
//import scala.math.{min, max}
import scala.util.control.Breaks._
import scala.collection.AbstractIterator
import scivs.interval.Interval
import scivs.scivs.Scivs
import scivs.util.Cursor

/** Lapper represents a list of intervals ordered by start position.
  *  @example
  *  {{{
  *  import scivs.lapper.Lapper
  *  import scivs.interval.Interval
  *  val lapper = new Lapper((0 to 20 by 5).map(Interval(_, _ + 2, 0)).toList))
  *  assert(lapper.find(6, 11).toList(0), Interval(5, 7, 0))
  *  }}}
  */
class Lapper[T](ivs: Seq[Interval[T]]) extends Scivs[T, LapperIter[T]] {
  val intervals = ivs.sorted
  private val starts = intervals.map(_.start)
  private val stops = intervals.map(_.stop)
  private val maxLen = intervals.map(iv => iv.stop - iv.start).max
  private val cursor = 0

  def length(): Int = this.intervals.length
  def isEmpty(): Boolean = this.intervals.isEmpty

  /** Return the furthest left index to begin search at based on the start - maxLen */
  def lowerBound(start: Int): Int = {
    var size = this.length
    var low = 0

    while (size > 0) {
      val half = size / 2
      val other_half = size - half
      val probe = low + half
      val other_low = low + other_half
      val v = this.intervals(probe)
      size = half
      low = if (v.start < start) other_low else low
    }
    low
  }

  /** Return an iterator of [[scivs.interval.Interval]] that overlap the query
    *  `start until stop`.
    */
  def find(start: Int, stop: Int): LapperIter[T] = {
    var cursor = new Cursor(this.lowerBound(start - this.maxLen))
    new LapperIter(this, start, stop, cursor)
  }

  /** Return an iterator of [[scivs.interval.Interval]] that overlap the query
    *  `start until stop`. Accepts an [[scivs.util.Cursor]] to allow for fast
    *  sequential queries
    */
  def seek(start: Int, stop: Int, cursor: Cursor): LapperIter[T] = {
    if (cursor.index == 0 || (cursor.index < this
          .length() && this.intervals(cursor.index).start > start)) {
      cursor.index = this.lowerBound(start - this.maxLen)
    }

    while (cursor.index + 1 < this.length() && this
             .intervals(cursor.index + 1)
             .start < start - this.maxLen) {
      cursor.index += 1
    }

    new LapperIter(this, start, stop, cursor)
  }
}

class LapperIter[A](
    lapper: Lapper[A],
    start: Int,
    stop: Int,
    var cursor: Cursor
) extends AbstractIterator[Interval[A]] {
  override def hasNext(): Boolean = {
    while (cursor.index < lapper.length()) {
      val interval = lapper.intervals(cursor.index)
      if (interval.overlap(start, stop)) {
        return true
      } else if (interval.start >= stop) {
        return false
      }
      cursor.index += 1
    }
    false
  }
  override def next(): Interval[A] = {
    val curr = cursor.index
    cursor.index += 1
    lapper.intervals(curr)
  }
}
