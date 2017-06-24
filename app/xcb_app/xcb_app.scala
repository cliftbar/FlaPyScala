package xcb_app

import javax.inject._

import xcb_app.hurricane.BoundingBox
import xcb_app.{LibraryClass => lc}
import xcb_app.{hurricane => hur}
import java.time

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

  def hurrTest(trackPoints:Seq[hur.TrackPoint], bBox:BoundingBox, fSpeed_kts:Option[Double], rMax_nmi:Double):String = {
    println("In Function")
    println(rMax_nmi)
    println(fSpeed_kts)
    println(bBox.leftLonX)
    println(trackPoints(0))
    println(time.LocalDateTime.now())
    val grid = new hur.LatLonGrid(bBox.topLatY, bBox.botLatY, bBox.leftLonX, bBox.rightLonX, 10, 10)
    val event = new hur.HurricaneEvent(grid, trackPoints.toList, rMax_nmi)
    event.DoCalcs()
    println(time.LocalDateTime.now())
    println("Did test")
    return "hurrTestWorked"
  }
}
