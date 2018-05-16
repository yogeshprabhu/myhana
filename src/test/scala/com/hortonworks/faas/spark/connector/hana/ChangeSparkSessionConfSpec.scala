// scalastyle:off magic.number file.size.limit regex
package com.hortonworks.faas.spark.connector.hana

import com.hortonworks.faas.spark.connector._
import com.hortonworks.faas.spark.connector.util.JDBCImplicits._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{FlatSpec, Matchers}

/**
  * Test that changing the SparkSession RuntimeConfig settings for spark.hanadb.host etc
  * affects the next attempt to connect to hanadb.
  */
class ChangeSparkSessionConfSpec extends FlatSpec with SharedHanaDbContext with Matchers {
  override def beforeAll(): Unit = {
    super.beforeAll()
    this.withConnection(conn => {
      conn.withStatement(stmt => {
        stmt.execute("""
          CREATE TABLE t
          (id INT PRIMARY KEY, data VARCHAR(200), key(data))
        """)
      })
    })
  }

  "Changing the hanadb settings in the SparkSession RuntimeConfig" should "be reflected in the next attempt to connect to hanadb" in {
    val df = ss
      .read
      .format("com.hortonworks.faas.spark.connector")
      .options(Map( "path" -> ("t")))
      .load()

    df.collect()

    // Change the configuration settings

    val newconf = new SparkConf()
      .setAppName("hanadb Spark Connector Example")
      .set("spark.hanadb.host", "fakehost")
      .set("spark.hanadb.user", "fakeuser")
      .set("spark.hanadb.password", "fakepassword")
      .set("spark.hanadb.defaultDatabase", "fakedatabase")

    ss = SparkSession.builder().config(newconf).getOrCreate()

    assert(ss.conf.get("spark.hanadb.host") == "fakehost")

    try {
      ss.getHanaDbCluster.withHanaConn[Boolean](conn => {
        conn.withStatement(stmt => {
          stmt.execute("""
          CREATE TABLE t
          (id INT PRIMARY KEY, data VARCHAR(200), key(data))
        """)
        })
      })
      assert(false, "The connection should have failed when using the new config settings")
    } catch {
      case e: java.sql.SQLException => {
        assert(e.getMessage.contains("Cannot create PoolableConnectionFactory"))
      }
    }

    try {
      val df2 = ss
        .read
        .format("com.hortonworks.faas.spark.connector")
        .options(Map( "path" -> ("t")))
        .load()
      assert(false, "The connection should have failed when using the new config settings")
    } catch {
      case e: java.sql.SQLException => {
        assert(e.getMessage.contains("Cannot create PoolableConnectionFactory"))
      }
    }
  }
}
