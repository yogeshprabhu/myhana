package com.hortonworks.faas.spark.connector.hana

import org.scalatest.{FlatSpec, Matchers}

class NullValueSpec extends FlatSpec with SharedHanaDbContext with Matchers {

  override def beforeAll(): Unit = {
    super.beforeAll()
    withStatement(stmt => {
      stmt.execute("CREATE TABLE t(a BIGINT PRIMARY KEY, t_FLOAT FLOAT, t_REAL REAL, t_DOUBLE DOUBLE)")
      stmt.execute("INSERT INTO t values (1, NULL, NULL, NULL)")
    })
  }

  // TestTypes.scala checks that all types round-trip "null", i.e. scala null <--> SQL NULL.
  // Floating-point types can end up as NaN in some cases; this test double-checks their behavior.
  it should "read NULL values for FLOAT, REAL, and DOUBLE types" in {

    val result = ss
      .read
      .format("com.hortonworks.faas.spark.connector")
      .options(Map( "path" -> (dbName + ".t")))
      .load()
      .collect()(0)

    assert(result.isNullAt(1))
    assert(result.isNullAt(2))
    assert(result.isNullAt(3))

  }
}
