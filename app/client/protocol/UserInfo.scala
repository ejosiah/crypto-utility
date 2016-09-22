package client.protocol

/**
  * Created by jay on 21/09/2016.
  */
case class UserInfo(fname: String, lname: String, email: String, clientId: Option[String] = None)

