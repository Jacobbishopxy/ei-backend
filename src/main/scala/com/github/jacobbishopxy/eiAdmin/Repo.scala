package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoLoader
import com.github.jacobbishopxy.Utilities._

/**
 * Created by Jacob Xie on 3/23/2020
 */
object Repo {

  val url: String = getMongoConfig("mongo-dev")
    .getOrElse("dev", throw new RuntimeException("database dev not found!"))

  val mongoLoader = new MongoLoader(url, "dev")

}
