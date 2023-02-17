package it.pagopa.interop.dashboardmetricsreportgenerator

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object Graph {

  def getGraphPoints(n: Int)(xs: Seq[OffsetDateTime]): List[GraphElement] = {
    val groupedDateValues: Seq[(OffsetDateTime, Int)] = groupValuesByDay(xs.sorted)
    val cumulativeValues: List[(OffsetDateTime, Int)] = cumulative(groupedDateValues.toList)
    takeEvenlyDistributedValues[Int](n)(cumulativeValues).map(Function.tupled(GraphElement.apply))
  }

  def groupValuesByDay(xs: Seq[OffsetDateTime]): Seq[(OffsetDateTime, Int)] = xs
    .map(_.truncatedTo(ChronoUnit.DAYS)) // * Smooths the curve using 1 value per day
    .foldLeft(Map.empty[OffsetDateTime, Int]) { case (map, time) =>
      map.updatedWith(time) {
        case Some(x) => Some(x + 1)
        case None    => Some(1)
      }
    }
    .toSeq

  // * Chunks a List creating always n sublist of ALMOST the same length
  private def chunk[T](xs: List[T])(n: Int): List[List[T]] = {
    val (k, m) = (xs.length / n, xs.length % n)
    0.until(n).toList.map(i => xs.slice(i * k + m.min(i), (i + 1) * k + m.min(i + 1)))
  }

  private def takeEvenlyDistributedValues[T](n: Int)(xs: List[(OffsetDateTime, T)]): List[(OffsetDateTime, T)] =
    if (xs.size <= n) xs
    else chunk(xs)(n - 1).foldLeft(xs.headOption.toList) { case (acc, l) => l.lastOption.toList ++ acc }

  private def cumulative(xs: List[(OffsetDateTime, Int)]): List[(OffsetDateTime, Int)] =
    xs.sortBy(_._1)
      .scan((OffsetDateTime.MIN, 0)) { case ((_, t1), (post, t2)) =>
        (post, t1 + t2)
      }
      .drop(1)
}
