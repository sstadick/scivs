package ivstore

import org.scalatest._
import interval.Interval
import scailist.ScAIList
import util.Cursor

object SetupScAIList {
  def nonOverlapping(): ScAIList[Int] = {
    val data = (0 to 1000 by 20).map(x => Interval(x, x + 10, 0))
    ScAIList(data, 16, true)
  }

  def overlapping(): ScAIList[Int] = {
    val data = (0 to 1000 by 10).map(x => Interval(x, x + 15, 0))
    ScAIList(data, 16, true)
  }

  def badScAIList(): ScAIList[Int] = {
    val data = List(
      Interval(70, 120, 0), // maxLen = 50
      Interval(10, 15, 0),
      Interval(10, 15, 0), // exact overlap
      Interval(12, 15, 0), // inner overlap
      Interval(14, 16, 0), // overlap end
      Interval(40, 45, 0),
      Interval(50, 55, 0),
      Interval(60, 65, 0),
      Interval(68, 71, 0), // overlap start
      Interval(70, 75, 0)
    )
    ScAIList(data)
  }

  def single(): ScAIList[Int] = {
    val data = List(Interval(10, 35, 0))
    ScAIList(data, 5)
  }
}

class ScAIListSpec extends FlatSpec with Matchers {

  "Intervals" should "overlap" in {
    val iv1 = new Interval(0, 5, true)
    val iv2 = new Interval(3, 6, true)
    iv1.overlap(iv2) shouldEqual true
  }

  "Intervals" should "intersect" in {
    val i1 = Interval(70, 120, 0) // maxLen = 50
    val i2 = Interval(10, 15, 0)
    val i3 = Interval(10, 15, 0) // exact overlap
    val i4 = Interval(12, 15, 0) // inner overlap
    val i5 = Interval(14, 16, 0) // overlap end
    val i6 = Interval(40, 45, 0)
    val i7 = Interval(50, 55, 0)
    val i8 = Interval(60, 65, 0)
    val i9 = Interval(68, 71, 0) // overlap start
    val i10 = Interval(70, 75, 0)

    i2.intersect(i3) shouldEqual 5 // exact match
    i2.intersect(i4) shouldEqual 3 // inner intersect
    i2.intersect(i5) shouldEqual 1 // end intersect
    i9.intersect(i10) shouldEqual 1 // start intersect
    i7.intersect(i8) shouldEqual 0 // no intersect
    i6.intersect(i7) shouldEqual 0 // no intersect stop = start
    i1.intersect(i10) shouldEqual 5 // inner intersect at start
  }

  "Query stop that hits an interval start" should "return no interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    lapper.find(15, 20).hasNext() shouldEqual false
  }

  "Query start that hits an inteval end" should "return no interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    lapper.find(30, 35).hasNext() shouldEqual false
  }

  "Query that overlaps the start of an interval" should "return that interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    val expected = Interval(20, 30, 0)
    val iter = lapper.find(15, 25)
    iter.hasNext() shouldEqual true
    iter.next() shouldEqual expected
  }

  "Query that overlaps the stop of an interval" should "returns that interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    val expected = Interval(20, 30, 0)
    val iter = lapper.find(25, 35)
    iter.hasNext() shouldEqual true
    iter.next() shouldEqual expected
  }

  "Query that is enveloped by interval" should "return interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    val expected = Interval(20, 30, 0)
    val iter = lapper.find(22, 27)
    iter.hasNext() shouldEqual true
    iter.next() shouldEqual expected
  }

  "Query that envelopes an interval" should "return that interval" in {
    val lapper = SetupScAIList.nonOverlapping()
    val expected = Interval(20, 30, 0)
    val iter = lapper.find(15, 35)
    iter.hasNext() shouldEqual true
    iter.next() shouldEqual expected
  }

  "Query that overlaps multiple intervals" should "return multiple intervals" in {
    println("Missing intervals")
    val lapper = SetupScAIList.overlapping()
    val e1 = Interval(0, 15, 0)
    val e2 = Interval(10, 25, 0)
    List(e1, e2).sorted shouldEqual lapper.find(8, 20).toList.sorted
  }

  "Query overlaps in large intervals" should "return overlapped intervals" in {
    val data = List(
      Interval(0, 8, 0),
      Interval(1, 10, 0),
      Interval(2, 5, 0),
      Interval(3, 8, 0),
      Interval(4, 7, 0),
      Interval(5, 8, 0),
      Interval(8, 8, 0),
      Interval(9, 11, 0),
      Interval(10, 13, 0),
      Interval(100, 200, 0),
      Interval(110, 120, 0),
      Interval(110, 124, 0),
      Interval(111, 160, 0),
      Interval(150, 200, 0)
    )

    val lapper = ScAIList(data)
    val found1 = lapper.find(8, 11).toList
    found1 shouldEqual List(
      Interval(1, 10, 0),
      Interval(9, 11, 0),
      Interval(10, 13, 0)
    )

    val found2 = lapper.find(145, 151).toList
    found2 shouldEqual List(
      Interval(100, 200, 0),
      Interval(111, 160, 0),
      Interval(150, 200, 0)
    )
  }

  "Query that is pre first match" should "still return the match" in {
    val lapper = SetupScAIList.badScAIList()
    val e1 = Interval(50, 55, 0)
    val iter = lapper.find(50, 55)
    iter.hasNext() // hasNext MUST be called to advance the cursor to the first match
    val found = iter.next()
    found shouldEqual e1
  }

  "When there is a very long interval that spans many little intervals, the little intervals" should "still be returned" in {
    val data = List(
      Interval(25264912, 25264986, 0),
      Interval(27273024, 27273065, 0),
      Interval(27440273, 27440318, 0),
      Interval(27488033, 27488125, 0),
      Interval(27938410, 27938470, 0),
      Interval(27959118, 27959171, 0),
      Interval(28866309, 33141404, 0)
    )
    val lapper = ScAIList(data)
    val found = lapper.find(28974798, 33141355).toList
    found shouldEqual List(Interval(28866309, 33141404, 0))
  }
}
