package it.pagopa.interop.dashboardmetricsreportgenerator

import java.time.OffsetDateTime

object Graph {

  def getGraphPoints(n: Int)(xs: Seq[(OffsetDateTime, Int)]): List[GraphElement] =
    takeEvenlyDistributedValues[Int](n)(cumulative(xs.toList)).map(Function.tupled(GraphElement.apply))

  private def takeEvenlyDistributedValues[T](n: Int)(xs: List[(OffsetDateTime, T)]): List[(OffsetDateTime, T)] =
    if (xs.size <= n) xs
    else {
      // * In this way the first range might be doubled, but we'll cover the whole timespan
      val nPlusOneValues: List[(OffsetDateTime, T)] = xs.grouped(xs.size / n).map(_.last).toList
      nPlusOneValues.head :: nPlusOneValues.drop(2)
    }

  private def cumulative(xs: List[(OffsetDateTime, Int)]): List[(OffsetDateTime, Int)] =
    xs.sortBy(_._1)
      .scan((OffsetDateTime.MIN, 0)) { case ((_, t1), (post, t2)) =>
        (post, t1 + t2)
      }
      .drop(1)
}
