package xcb_app

import javax.inject._

import xcb_app.{LibraryClass => lc}
/**
  * Created by cameron.barclift on 5/5/2017.
  */
@Singleton
class xcb_app {
  var count = 0
  def hello: String = {"Hello xcb_app"}
  def nextCount: Int = {
    count += 1
    return count
  }

  def libHello: String = {
    lc.Hello
  }
}
