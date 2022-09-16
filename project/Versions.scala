object Versions {
  lazy val akkaVersion    = "2.6.17"
  lazy val catsVersion    = "2.8.0"
  lazy val jacksonVersion = "2.11.4" // This cannot be updated yet because akka-serialization use 2.11.x version
  lazy val logbackVersion = "1.2.11"
  lazy val mongodbScalaDriverVersion = "4.6.0"
}

object PagopaVersions {
  lazy val attributeRegistryManagementVersion = "1.0.3"
  lazy val commonsVersion                     = "1.0.7"
  lazy val partyRegistryProxyVersion          = "1.0.2"
  lazy val tenantManagementVersion            = "1.0.0"
  lazy val tenantProcessVersion               = "1.0.0"
}
