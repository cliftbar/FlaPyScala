package xcb_app.hurricane

import java.util.Date

/**
  * Created by cameron.barclift on 5/15/2017.
  */
case class TrackPoint(
                       catalogNumber:Int
                       , stormName:String
                       , basin:String
                       , timestamp:Date
                       , eyeLat_y:Double
                       , eyeLon_x:Double
                       , heading:Double
                       , maxWind_kts:Int
                       , minCp_mb:Int
                       , sequence:Int
                       , rMax_nmi:Int
                       , bearing:Double
                       , fSpeed_kts:Int
                       , gwaf:Double
)
