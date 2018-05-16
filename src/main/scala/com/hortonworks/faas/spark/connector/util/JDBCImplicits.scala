package com.hortonworks.faas.spark.connector.util

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}

import com.hortonworks.faas.spark.connector.hana.dataframe.TypeConversions
import org.apache.spark.sql.Row

object JDBCImplicits {
  implicit class ResultSetHelpers(val rs: ResultSet) {
    def toIterator: Iterator[ResultSet] = new Iterator[ResultSet] {
      def hasNext = rs.next()
      def next() = rs
    }

    def hasColumn(columnName: String): Boolean = {
      val metadata = rs.getMetaData
      Range(0, metadata.getColumnCount)
        .exists(i => columnName == metadata.getColumnName(i + 1))
    }

    def toRow: Row = {
      val columnCount = rs.getMetaData.getColumnCount

      Row.fromSeq(Range(0, columnCount).map(i => {
        val columnType = rs.getMetaData.getColumnType(i + 1)
        val isSigned = rs.getMetaData.isSigned(i + 1)
        TypeConversions.GetJDBCValue(columnType,rs, i + 1)
      }))
    }

    def toArray: Array[Any] = {
      Array.tabulate(rs.getMetaData.getColumnCount)({
        i => rs.getObject(i + 1).asInstanceOf[Any]
      })
    }
  }

  implicit class PreparedStatementHelpers(val stmt: PreparedStatement) {
    def fillParams(sqlParams: Seq[Any]): Statement = {
      for (i <- sqlParams.indices) {
        stmt.setObject(i + 1, sqlParams(i))
      }
      stmt
    }
  }

  implicit class ConnectionHelpers(val conn: Connection) {
    def withStatement[T](handle: Statement => T): T =
      Loan[Statement](conn.createStatement).to(handle)

    def withPreparedStatement[T](query: String, handle: PreparedStatement => T): T =
      Loan[PreparedStatement](conn.prepareStatement(query)).to(handle)
  }
}
