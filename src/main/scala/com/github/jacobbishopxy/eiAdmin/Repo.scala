package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoLoader
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by Jacob Xie on 3/23/2020
 */
object Repo {

  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")
  val databaseName = "dev"

  val mongoLoader = new MongoLoader(mongoUrl, databaseName)

}
