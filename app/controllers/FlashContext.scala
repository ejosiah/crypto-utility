package controllers

import play.api.mvc.Flash

/**
  * Created by jay on 18/09/2016.
  */
object FlashContext{

  private var flash = Option.empty[Flash]

  def set(some: Option[Flash]) = flash = some

  def clear = flash = None

  def get(key: String) = flash.get.get(key)

  def isDefined(key: String) = flash.isDefined && flash.get.get(key).isDefined

}
