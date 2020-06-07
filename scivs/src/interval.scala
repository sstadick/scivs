/** Provides class for representing an Interval
  *  ==Overview==
  *  The main class to use is [[scivs.interval.Interval]]
  */
package scivs.interval
import scala.math.{min, max}

/** An Interval that is start inclusive
  */
sealed case class Interval[T](start: Int, stop: Int, label: T)
    extends Ordered[Interval[T]] {

  /** Returns the number of 'bases' that overlap between two intervals */
  def intersect(other: Interval[T]): Int = {
    max(min(this.stop, other.stop) - max(this.start, other.start), 0)
  }

  /** Returns true if `this` interval overlas the `other` interval */
  def overlap(other: Interval[T]): Boolean =
    this.start < other.stop && this.stop > other.start

  /** Returns true if `this` interval overlaps the range `start until stop` */
  def overlap(start: Int, stop: Int): Boolean =
    this.start < stop && this.stop > start

  /** Make intervals compareable */
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
