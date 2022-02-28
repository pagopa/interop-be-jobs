package it.pagopa.interop.attributesloader.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  lazy val attributeRegistryManagementURL: String = config.getString("services.attribute-registry-management")

  lazy val rsaPrivatePath: String = config.getString("interop-be-attributes-loader.rsa-private-path")

}
