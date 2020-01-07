package scivs.util

/**
  *  A simple wrapper object over an integer to allow for passing a reference to
  *  an Int
  */
sealed case class Cursor(var index: Int)
