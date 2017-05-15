package xcb_app.hurricane

import java.util.Date
/**
  * Created by cameron.barclift on 5/15/2017.
  */
case class TrackPoint(catalog_number:Int, storm_name:String, basin:String, timestamp:Date, lat_y:Double, lon_x:Double, max_wind_kts:Int, min_cp_mb:Int, sequence:Int, rmax_nmi:Int, bearing:Double, fspeed_kts:Int, gwaf:Double)
