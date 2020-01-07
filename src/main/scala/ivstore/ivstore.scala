/** This is the root level object of the [[ivstore]] package. */
package ivstore.ivstore

import ivstore.interval.Interval
import ivstore.util.Cursor

/** IVStore is the abstract class that must be implemented to be compatable with
  *  all the helper functions
  */
abstract class IVStore[T, I <: Iterator[Interval[T]]]() {
  def find(start: Int, stop: Int): I
  def length(): Int
  def isEmpty(): Boolean
}
