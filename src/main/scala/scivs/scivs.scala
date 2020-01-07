/** This is the root level object of the [[scivs]] package. */
package scivs.scivs

import scivs.interval.Interval
import scivs.util.Cursor

/** scivs is the abstract class that must be implemented to be compatable with
  *  all the helper functions
  */
abstract class Scivs[T, I <: Iterator[Interval[T]]]() {
  def find(start: Int, stop: Int): I
  def length(): Int
  def isEmpty(): Boolean
}
