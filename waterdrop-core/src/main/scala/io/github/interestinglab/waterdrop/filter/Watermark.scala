/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.github.interestinglab.waterdrop.filter

import io.github.interestinglab.waterdrop.config.{Config, ConfigFactory}
import io.github.interestinglab.waterdrop.apis.BaseFilter
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.apache.spark.sql.functions._
import scala.collection.JavaConversions._

class Watermark extends BaseFilter {

  var config: Config = ConfigFactory.empty()
  var timeField: String = _
  var waterMarkField: String = _

  override def setConfig(config: Config): Unit = this.config = config

  override def getConfig(): Config = config

  override def checkConfig(): (Boolean, String) = {
    config.hasPath("time_field") && config.hasPath("delay_threshold")&& config.hasPath("watermark_field") &&
      config.hasPath("time_type") match {
      case true => (true, "")
      case false => (false, "please specify [time_field] and [delay_threshold] and [watermark_field] and [time_type]")
    }
  }

  override def prepare(spark: SparkSession): Unit = {
    super.prepare(spark)
    timeField = config.getString("time_field")
    waterMarkField = config.getString("watermark_field")
    val defaultConfig = ConfigFactory.parseMap(
      Map(
        "time_pattern" -> "yyyy-MM-dd HH:mm:ss",
        "time_type" -> "UNIX"
      )
    )
    config = config.withFallback(defaultConfig)
  }

  override def process(spark: SparkSession, df: Dataset[Row]): Dataset[Row] = {

    val realDf = config.hasPath("source_table_name") match {
      case true => spark.table(config.getString("source_table_name"))
      case false => df
    }
    val pattern = config.getString("time_pattern")
    val newDf = config.getString("time_type") match {
      case "UNIX_MS" => realDf.withColumn(waterMarkField, expr(s"to_timestamp(from_unixtime($timeField /1000),'$pattern')"))
      case "UNIX" => realDf.withColumn(waterMarkField, expr(s"to_timestamp(from_unixtime($timeField),'$pattern')"))
      case "string" => realDf.withColumn(waterMarkField, expr(s"to_timestamp($timeField,'$pattern')"))
    }
    val waterMarkDf = newDf.withWatermark(waterMarkField,config.getString("delay_threshold"))
    if (config.hasPath("result_table_name")){
      waterMarkDf.createOrReplaceTempView(config.getString("result_table_name"))
    }
    waterMarkDf
  }
}
