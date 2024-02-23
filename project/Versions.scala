object Versions {
  lazy val akkaVersion    = "2.6.20"
  lazy val catsVersion    = "2.8.0"
  lazy val circeVersion   = "0.14.2"
  lazy val jacksonVersion = "2.11.4" // This cannot be updated yet because akka-serialization use 2.11.x version
  lazy val logbackVersion = "1.4.4"
  lazy val mongodbScalaDriverVersion = "4.7.2"
  lazy val munitVersion              = "0.7.29"
  lazy val scalaMockVersion          = "5.2.0"
  lazy val scalatestVersion          = "3.2.16"
  lazy val spoiwoVersion             = "2.2.1"
}

object PagopaVersions {
  lazy val attributeRegistryManagementVersion = "1.0.16"
  lazy val attributeRegistryProcessVersion    = "1.0.8"
  lazy val catalogManagementVersion           = "1.0.19"
  lazy val catalogProcessVersion              = "1.0.15"
  lazy val commonsVersion                     = "1.0.23"
  lazy val partyRegistryProxyVersion          = "1.0.15"
  lazy val agreementManagementVersion         = "1.0.15"
  lazy val agreementProcessVersion            = "1.0.16"
  lazy val purposeManagementVersion           = "1.0.14"
  lazy val purposeProcessVersion              = "1.0.20"
  lazy val tenantManagementVersion            = "1.0.11"
  lazy val tenantProcessVersion               = "1.0.16"
  lazy val certifiedMailSenderModelsVersion   = "1.0.23"
  lazy val selfcareV2ClientVersion            = "1.0.12"
}
