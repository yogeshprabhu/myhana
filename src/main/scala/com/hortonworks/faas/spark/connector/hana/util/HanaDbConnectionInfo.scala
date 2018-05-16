package com.hortonworks.faas.spark.connector.hana.util

import java.net.InetAddress

case class HanaDbConnectionInfo(dbHost: String,
                                dbPort: Int,
                                user: String,
                                password: String,
                                dbName: String) {

  def toJDBCAddress: String = {
    var address = s"jdbc:sap://$dbHost:$dbPort"
    //var address = s"jdbc:mysql://$dbHost:$dbPort"
    if (dbName.length > 0) {
      address += "/" + dbName
    }
    address
  }

  /**
    * Determine if the HanaDb node referred to by this object is colocated
    * with the machine you run this function on.
    *
    * @note The method used in this function is currently VERY brittle
    *       and will not work in plenty of valid cases.
    */
  def isColocated: Boolean = {
    InetAddress.getLocalHost.getHostName == dbHost
  }
}
