package xcb_app

import javax.inject._

import xcb_app.hurricane.BoundingBox
import xcb_app.{LibraryClass => lc}
import xcb_app.{hurricane => hur}
import xcb_app.{hurricaneNws23 => nws}

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
    //val calcs = trackPoints.map(x => )
    return "hurrTestWorked"
  }
}
