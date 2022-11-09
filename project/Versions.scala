object Versions {
  lazy val akkaVersion    = "2.6.20"
  lazy val catsVersion    = "2.8.0"
  lazy val jacksonVersion = "2.11.4" // This cannot be updated yet because akka-serialization use 2.11.x version
  lazy val logbackVersion = "1.4.4"
  lazy val mongodbScalaDriverVersion = "4.7.2"
  lazy val munitVersion              = "0.7.29"
}

object PagopaVersions {
  lazy val attributeRegistryManagementVersion = "1.0.4"
  lazy val commonsVersion = "1.0.8"
  lazy val partyRegistryProxyVersion = "1.0.3"
  lazy val tenantManagementVersion = "1.0.1"
  lazy val tenantProcessVersion = "1.0.1"
}
