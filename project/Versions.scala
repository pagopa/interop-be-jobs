object Versions {
  lazy val akkaVersion    = "2.6.17"
  lazy val jacksonVersion = "2.11.4" // This cannot be updated yet because akka-serialization use 2.11.x version
  lazy val logbackVersion = "1.2.11"
  lazy val mongodbScalaDriverVersion = "4.6.0"
}

object PagopaVersions {
  lazy val attributeRegistryManagementVersion = "1.0.x-SNAPSHOT"
  lazy val commonsVersion                     = "1.0.x-SNAPSHOT"
  lazy val partyRegistryProxyVersion          = "1.0.x-SNAPSHOT"
  lazy val tenantManagementVersion            = "1.0.x-SNAPSHOT"
  lazy val tenantProcessVersion               = "1.0.x-SNAPSHOT"
}
