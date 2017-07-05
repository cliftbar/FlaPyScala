package xcb_app.hurricane

import java.time.format.DateTimeFormatter

import play.api.libs.json._
import play.api.libs.json.Reads._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._

/**
  * Created by cameron.barclift on 5/15/2017.
  */
case class TrackPoint(
                       catalogNumber:Option[Int]
                       , stormName:String
                       , basin:Option[String]
                       , timestamp:LocalDateTime
                       , eyeLat_y:Double
                       , eyeLon_x:Double
                       , maxWind_kts:Option[Double]
                       , minCp_mb:Option[Double]
                       , sequence:Double
                       , var fSpeed_kts:Double
                       , var isLandfallPoint:Boolean
                       , rMax_nmi:Double
                       , gwaf:Double
                       , var heading:Option[Double]
)

object TrackPoint {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
  val dtReads = Reads[LocalDateTime](js =>
    js.validate[String].map[LocalDateTime](dtString =>
      LocalDateTime.parse(dtString, DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm"))
    )
  )

  implicit val trackPointReads: Reads[TrackPoint] = (
    (JsPath \ "catalogNumber").readNullable[Int].orElse(Reads.pure(None)) and
    (JsPath \ "stormName").read[String] and
    (JsPath \ "basin").readNullable[String].orElse(Reads.pure(None)) and
    (JsPath \ "timestamp").read[LocalDateTime](dtReads) and
    (JsPath \ "eyeLat_y").read[Double] and
    (JsPath \ "eyeLon_x").read[Double] and
    (JsPath \ "maxWind_kts").readNullable[Double].orElse(Reads.pure(None)) and
    (JsPath \ "minCp_mb").readNullable[Double].orElse(Reads.pure(None)) and
    (JsPath \ "sequence").read[Double] and
    (JsPath \ "fSpeed_kts").read[Double] and
    (JsPath \ "isLandfallPoint").read[Boolean] and
    (JsPath \ "rMax_nmi").read[Double] and
    (JsPath \ "gwaf").read[Double] and
    (JsPath \ "heading").readNullable[Double].orElse(Reads.pure(None))
  )(TrackPoint.apply _)
}
