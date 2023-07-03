object Versions {
  lazy val akkaVersion    = "2.6.20"
  lazy val catsVersion    = "2.8.0"
  lazy val circeVersion   = "0.14.2"
  lazy val jacksonVersion = "2.11.4" // This cannot be updated yet because akka-serialization use 2.11.x version
  lazy val logbackVersion = "1.4.4"
  lazy val mongodbScalaDriverVersion = "4.7.2"
  lazy val munitVersion              = "0.7.29"
  lazy val scanamoVersion            = "1.0.0-M25"
  lazy val sttpVersion               = "4.0.0-M1"
  lazy val sttpModelVersion          = "1.5.5"
  lazy val scalaMockVersion            = "5.2.0"
  lazy val scalatestVersion            = "3.2.16"
}

object PagopaVersions {
  lazy val attributeRegistryManagementVersion = "1.0.12"
  lazy val attributeRegistryProcessVersion    = "1.0.2"
  lazy val catalogManagementVersion           = "1.0.13"
  lazy val commonsVersion                     = "1.0.18"
  lazy val partyRegistryProxyVersion          = "1.0.11"
  lazy val agreementManagementVersion         = "1.0.11"
  lazy val agreementProcessVersion            = "1.0.11"
  lazy val purposeManagementVersion           = "1.0.9"
  lazy val tenantManagementVersion            = "1.0.7"
  lazy val tenantProcessVersion               = "1.0.8"
  lazy val partyManagementClientVersion       = "1.0.7"
  lazy val certifiedMailSenderModelsVersion   = "1.0.x-SNAPSHOT"
  lazy val selfcareV2ClientVersion            = "1.0.7"
}
