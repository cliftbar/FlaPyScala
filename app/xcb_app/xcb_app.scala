package xcb_app

import javax.inject._

import xcb_app.{LibraryClass => lc}
import xcb_app.{hurricane => hur}
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

  def hurrTest():String = {
    println("In Function")
    return "hurrTestWorked"
//    val tps = List.empty
//    val grid = new hur.LatLonGrid(10, 15, 10, 15, 10, 10)
//    var event = new hur.HurricaneEvent(grid, tps, 15)
//    event.DoCalcs()
  }
}
