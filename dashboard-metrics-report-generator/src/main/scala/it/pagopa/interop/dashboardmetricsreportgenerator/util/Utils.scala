package it.pagopa.interop.dashboardmetricsreportgenerator.util

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.dashboardmetricsreportgenerator.models.{Agreement, Purpose}

import scala.concurrent.{ExecutionContext, Future}

object Utils {

  final val maxLimit = 100

  def retrieveAllActiveAgreements(
    agreementsRetriever: (Int, Int) => Future[Seq[Agreement]],
    offset: Int,
    acc: Seq[Agreement]
  )(implicit logger: Logger, ex: ExecutionContext): Future[Seq[Agreement]] =
    agreementsRetriever(offset, maxLimit).flatMap(agreements =>
      if (agreements.isEmpty) {
        logger.info(s"Active agreements load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllActiveAgreements(agreementsRetriever, offset + maxLimit, acc ++ agreements)
    )

  def retrieveAllPurposes(
    purposesRetriever: (Int, Int) => Future[Seq[Purpose]],
    offset: Int,
    acc: Seq[Purpose]
  )(implicit logger: Logger, ex: ExecutionContext): Future[Seq[Purpose]] =
    purposesRetriever(offset, maxLimit).flatMap(purposes =>
      if (purposes.isEmpty) {
        logger.info(s"Purposes load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllPurposes(purposesRetriever, offset + maxLimit, acc ++ purposes)
    )
}
