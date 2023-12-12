package it.pagopa.interop.metricsreportgenerator.util

object Errors {
  final case object InterfacePathNotFound extends Exception(s"Interface path not found in active descriptor")
  final case class ChecksumNotFound(descriptorId: String)
      extends Exception(s"Interface checksum not found in active descriptor $descriptorId")
}
