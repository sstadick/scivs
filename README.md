# scivs 

This is a library containing Classes and helpers for working with
genomic intervals in Scala. Currently `Lapper` and `ScAIList` are
implemented, which are best in class in their respective niche's.

scivs = SCala InterVal Stores

## Lapper

This is a Scala port of the
[nim-lapper](https://github.com/brentp/nim-lapper). It is also inspired
by the rust port [rust-lapper](https://docs.rs/rust-lapper). 

```scala
import scivs.scailist.ScAIList
import scivs.interval.Interval
val lapper = new Lapper((0 to 20 by 5).map(Interval(_, _ + 2, 0)).toList))
assert(lapper.find(6, 11).toList(0), Interval(5, 7, 0))
```

### Performance Characteristics

Fantastic for 'normal' genomic data where intervals are 'short' and
there isn't much nesting. Think Illumina PE reads. The `seek` method in
particular is very fast if you know that your queries will be in order.

## ScAIList

This is an implementation of the code from this
[paper](https://www.biorxiv.org/content/10.1101/593657v1). The major
change is that the number of component parts a list is broken into is
dynamic and not hardcoded. 

### Performance Characteristics

This datastructure is good for nested intervals where long intervals
engulf many shorter intervals. 

```scala
import scivs.scailist.ScAIList
import scivs.interval.Interval
val scailist = ScAIList((0 to 20 by 5).map(Interval(_, _ + 2, 0)).toList))
assert(scailist.find(6, 11).toList(0), Interval(5, 7, 0))
```


## Todo's

- Add benchmarks / substantiate the performance characteristics
- Compare against other libs out there?
- Add some of the helper methods for things like coverage etc
