package lapper
import scala.math.{min, max}
import scala.util.control.Breaks._
import scala.collection.AbstractIterator

sealed case class Interval[T](start: Int, stop: Int, label: T)
    extends Ordered[Interval[T]] {
  def intersect(other: Interval[T]): Int = {
    max(min(this.stop, other.stop) - max(this.start, other.start), 0)
  }

  def overlap(other: Interval[T]): Boolean =
    this.start < other.stop && this.stop > other.start

  def overlap(start: Int, stop: Int): Boolean =
    this.start < stop && this.stop > start

  def compare(other: Interval[T]) = {
    if (this.start < other.start) {
      -1
    } else if (this.start > other.start) {
      1
    } else {
      this.stop compare other.stop
    }
  }
}

class Lapper[T](ivs: Seq[Interval[T]]) {
  val intervals = ivs.sorted
  private val starts = intervals.map(_.start)
  private val stops = intervals.map(_.stop)
  private val maxLen = intervals.map(iv => iv.stop - iv.start).max
  private val cursor = 0

  def length(): Int = this.intervals.length
  def isEmpty(): Boolean = this.intervals.isEmpty

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

  def find(start: Int, stop: Int): FindIter[T] = {
    var cursor = new Cursor(this.lowerBound(start - this.maxLen))
    new FindIter(this, start, stop, cursor)
  }

  def seek(start: Int, stop: Int, cursor: Cursor): FindIter[T] = {
    if (cursor.index == 0 || (cursor.index < this
          .length() && this.intervals(cursor.index).start > start)) {
      cursor.index = this.lowerBound(start - this.maxLen)
    }

    while (cursor.index + 1 < this.length() && this
             .intervals(cursor.index + 1)
             .start < start - this.maxLen) {
      cursor.index += 1
    }

    new FindIter(this, start, stop, cursor)
  }
}

sealed case class Cursor(var index: Int)

class FindIter[A](lapper: Lapper[A], start: Int, stop: Int, var cursor: Cursor)
    extends AbstractIterator[Interval[A]] {
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
