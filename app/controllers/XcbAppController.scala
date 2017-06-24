package controllers

import java.io.FileWriter
import java.io.FileInputStream
import javax.inject._

import xcb_app._
import play.api.mvc._
import play.api.libs.json._
import _root_.xcb_app.hurricane._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class XcbAppController @Inject()(app:xcb_app) extends Controller {

  def nextCount = Action {
    Ok("Next Count: " + app.nextCount.toString)
  }

  def helloApp = Action {
    Ok(app.hello)
  }

  def helloLib = Action {
    Ok(app.libHello)
  }

  def SampleInputTest = Action {
    println("Sample Input Test")
    val reader = new FileInputStream("SampleInput.txt")
    val json =  Json.parse(reader)

    val trackPoints = (json \ "track").validate[Seq[TrackPoint]].get//.asOpt.get
    val bBoxJson = (json \ "BBox").get
    val bBox = new BoundingBox((bBoxJson \ "topLatY").as[Double], (bBoxJson \ "botLatY").as[Double], (bBoxJson\ "leftLonX").as[Double], (bBoxJson \ "rightLonX").as[Double])
    val fSpeed_kts = (json \ "fspeed").as[Double]
    val rMax_nmi = (json \ "rmax").as[Double]

    Ok(app.hurrTest(trackPoints, bBox, Option[Double](fSpeed_kts), rMax_nmi))
  }

  def hurTest = Action(parse.json) { request =>
    println("here now")
    val trackPoints = (request.body \ "track").validate[Seq[TrackPoint]].get//.asOpt.get
    val bBoxJson = (request.body \ "BBox").get
    val bBox = new BoundingBox((bBoxJson \ "topLatY").as[Double], (bBoxJson \ "botLatY").as[Double], (bBoxJson\ "leftLonX").as[Double], (bBoxJson \ "rightLonX").as[Double])
    val fSpeed_kts = (request.body \ "fspeed").as[Double]
    val rMax_nmi = (request.body \ "rmax").as[Double]

    println(bBox.leftLonX)
    //for (tp in trackPoints)

//    val writer = new FileWriter("SampleInput.txt")
//    writer.write(request.body.toString())
//    writer.close()

    Ok(app.hurrTest(trackPoints, bBox, Option[Double](fSpeed_kts), rMax_nmi))

    //val ls = user.map(x => x.split('|').toList.map)

    //val trackPoints = (json.get \ "track").get.as[]

  }
}
