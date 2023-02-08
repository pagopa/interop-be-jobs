package it.pagopa.interop.dashboardmetricsreportgenerator

import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import scala.concurrent.Future
import scala.annotation.nowarn

@nowarn
object Jobs {

  def getDescriptorData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[DescriptorsData] =
    Future {

      DescriptorsData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getTenantsData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[TenantsData] =
    Future {

      TenantsData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getAgreementsData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[AgreementsData] =
    Future {

      AgreementsData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getPurposesData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[PurposesData] =
    Future {

      PurposesData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getTokensData(readModel: FileManager, config: TokensBucketConfiguration): Future[TokensData] =
    Future {

      TokensData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)
}
