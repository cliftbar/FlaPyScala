package xcb_app

import java.io.FileWriter
import javax.inject._

import xcb_app.hurricane.BoundingBox
import xcb_app.{LibraryClass => lc}
import xcb_app.{hurricane => hur}
import java.time

import play.api.libs.json._

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

  def hurrTest(trackPoints:Seq[hur.TrackPoint], bBox:BoundingBox, fSpeed_kts:Option[Double], rMax_nmi:Double, pxPerDegree:(Int, Int), maxDist:Int, par:Int = -1):String = {
    println("In Function")
    println(rMax_nmi)
    println(fSpeed_kts)
    println(bBox.leftLonX)
    println(trackPoints(0))
    println(time.LocalDateTime.now())
    val grid = new hur.LatLonGrid(bBox.topLatY, bBox.botLatY, bBox.leftLonX, bBox.rightLonX, pxPerDegree._1, pxPerDegree._2)
    val event = new hur.HurricaneEvent(grid, trackPoints.toList, rMax_nmi)
    //event.CalcTrackpointHeadings()
    event.DoCalcs(maxDist, par)

    val writer = new FileWriter("testTrack.txt")
    writer.write("LatY\tLonX\tfspeed_kts\theading\n")

    for (x <- event.trackPoints) {
      writer.write(s"${x.eyeLat_y}\t${x.eyeLon_x}\t${x.fSpeed_kts}\t${x.heading}\n")
    }

    writer.close
    println(time.LocalDateTime.now())
    println("Did test")
    case class RetObject(imageUri: String)
    implicit val RetObjectWrites = new Writes[RetObject] {
      def writes(ret: RetObject) = Json.obj(
        "imageUri" -> ret.imageUri
      )
    }

    val ret = RetObject(System.getProperty("user.dir") + "\\OutImage.png")
    return Json.toJson(ret).toString()
  }
}
