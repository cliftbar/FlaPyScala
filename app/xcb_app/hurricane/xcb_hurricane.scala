package xcb_app.hurricane

import xcb_app.{hurricaneNws23 => nws}
import java.io._
import java.awt.image._
import java.awt.Color
import javax.imageio.ImageIO

package object HurricaneUtilities {

  def CalcBearingNorthZero(latRef:Double, lonRef:Double, latLoc:Double, lonLoc:Double):Double = {
    val lonDelta = lonLoc - lonRef
    val latDelta = latLoc - latRef

    val angleDeg = math.toDegrees(math.atan2(lonDelta, latDelta))
    return (angleDeg + 360) % 360
  }

  def calc_bearing_great_circle(latRef:Double, lonRef:Double, latLoc:Double, lonLoc:Double):Double = {
    val y = math.sin(lonLoc - lonRef) * math.cos(latLoc)
    val x = math.cos(latRef) * math.sin(latLoc) - math.sin(latRef) * math.cos(latLoc) * math.cos(lonLoc - lonRef)
    val brng = math.toDegrees(math.atan2(y, x))
    return (brng + 360) % 360
  }

  def haversine_degrees_to_meters(lat_1:Double, lon_1:Double, lat_2:Double, lon_2:Double):Double = {
    val r = 6371000
    val delta_lat = math.toRadians(lat_2 - lat_1)
    val delta_lon = math.toRadians(lon_2 - lon_1)

    val a = math.pow(math.sin(delta_lat / 2), 2) + math.cos(math.toRadians(lat_1)) * math.cos(math.toRadians(lat_2)) * math.pow(math.sin(delta_lon / 2), 2)
    val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return r * c
  }
}
class BoundingBox (val topLatY:Double, val botLatY:Double, val leftLonX:Double, val rightLonX:Double) {
//  val topLatY:Double = topLatY
//  val botLatY:Double = Double.NaN
//  val leftLonX:Double = Double.NaN
//  val rightLonX:Double = Double.NaN

  def Update(side:String, value:Double):BoundingBox = {
    return side match {
      case "top" => new BoundingBox(value, this.botLatY, this.leftLonX, this.rightLonX)
      case "bottom" => new BoundingBox(this.topLatY, value, this.leftLonX, this.rightLonX)
      case "left" => new BoundingBox(this.topLatY, this.botLatY, value, this.rightLonX)
      case "right" => new BoundingBox(this.topLatY, this.botLatY, this.leftLonX, value)
      case _ => throw new Exception("Unsupported Update")
    }
  }

  def GetWidth:Double = {return math.abs(this.rightLonX - this.leftLonX)}
  def GetHeight:Double = {return math.abs(this.topLatY - this.botLatY)}
}

class LatLonGrid(topLatY:Double, botLatY:Double, leftLonX:Double, rightLonX:Double, val BlockPerDegreeX:Int, val BlockPerDegreeY:Int) extends BoundingBox (topLatY, botLatY, leftLonX, rightLonX) {

  override def Update(item: String, value: Double): BoundingBox = {
    return item match {
      case "blocksX" => new LatLonGrid(this.topLatY, this.botLatY, this.leftLonX, this.rightLonX, value.toInt, this.BlockPerDegreeY)
      case "blocksY" => new LatLonGrid(this.topLatY, this.botLatY, this.leftLonX, this.rightLonX, value.toInt, this.BlockPerDegreeY)
      case _ => super.Update(item, value)
    }
  }

  def GetBlockIndex(latY:Double, lonX:Double):(Int, Int) = {
    val blockX = (lonX - this.leftLonX) * this.BlockPerDegreeX
    val blockY = (latY - this.botLatY) * this.BlockPerDegreeY

    return (blockY.toInt, blockX.toInt)
  }
  def GetBlockLatLon(blockX:Int, blockY:Int):(Double, Double) = {
    return (GetBlockLatY(blockY),GetBlockLonX(blockX))
  }

  def GetBlockLatY(blockY:Int):Double = {return this.botLatY + (blockY / this.BlockPerDegreeY.toDouble)}
  def GetBlockLonX(blockX:Int):Double = {this.leftLonX + (blockX / this.BlockPerDegreeX.toDouble)}

  def GetWidthInBlocks:Int = {(this.GetWidth * this.BlockPerDegreeX).toInt}
  def GetHeightInBlocks:Int = {(this.GetHeight * this.BlockPerDegreeY).toInt}

  def GetLatLonList:List[(Double,Double)] = { //List[(Double,Double)]
    val height = this.GetHeightInBlocks
    val width = this.GetWidthInBlocks
    //val grid = List.fill(height)(List.range(0, width)).zipWithIndex
    //val grid2 = grid.flatMap( {case (inner, outerIndex) => inner.map( innerIndex => (innerIndex, outerIndex))} )
    val grid3 = List.fill(height)(List.range(0,width)).zipWithIndex.flatMap(x => x._1.map(y => (y, x._2)).reverse).reverse
    return grid3.map(x => this.GetBlockLatLon(x._1, x._2))
  }

}
/**
  * Created by cameron.barclift on 5/12/2017.
  */
class HurricaneEvent (val grid:LatLonGrid, val trackPoints:List[TrackPoint], val rMax_nmi:Double) {

  def AddTrackPoint(tp:TrackPoint):HurricaneEvent = {
    return new HurricaneEvent(this.grid, this.trackPoints ::: List(tp), this.rMax_nmi)
  }

  def AddGrid(grid: LatLonGrid):HurricaneEvent = {
    return new HurricaneEvent(grid, this.trackPoints, this.rMax_nmi)
  }

  def DoCalcs():Unit = {
    println("DoCalcs")
    val latLonList = this.grid.GetLatLonList
    println("LatLonList")
    val CalcedResults = latLonList.map(x => TrackMap(this.trackPoints, x._1, x._2, this.rMax_nmi.toInt))
    //val CalcedTakeTwo = latLonList.map(x => (this.trackPoints, x)).map(x => x._1.map(tp => PointMap(tp, x._2._1, x._2._2, this.rMax_nmi.toInt)).maxBy(pt => pt._3))
    println("calced")

    this.WriteToImage(CalcedResults, this.grid.GetWidthInBlocks, this.grid.GetHeightInBlocks)

    val writer = new FileWriter("testOut.txt")
    writer.write("LatY\tLonX\twind_kts\n")

    for (x <- CalcedResults) {
      writer.write(s"${x._1}\t${x._2}\t${x._3}\n")
    }

//    for (x <- latLonList) {
//      val point = TrackMap(this.trackPoints, x._1, x._2, 15)
//      writer.write(s"${point._1}\t${point._2}\t${point._3}\n")
//    }

    writer.close()

    println("written")
  }

  def WriteToImage(outList: List[(Double, Double, Int)], width: Int, height: Int):Unit = {
    val pixels = outList.map(x => new Color(x._3, 0, 0, 255).getRGB)
    val pixLength = pixels.length
    val calcLength = width * height
    println(s"Pix Length: $pixLength, Calc Length: $calcLength")
    //val pixels = outList.map(x => x._3.toShort)
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val raster = image.getRaster
    raster.setDataElements(0, 0, width, height, pixels.toArray)
    ImageIO.write(image, "PNG", new File("OutImage.png"))
  }
  def TrackMap(pointLatY:Double, pointLonX:Double, rMax_nmi:Double):(Double, Double, Int) = {
    val parallel = true

    val ret = if (parallel) {
      this.trackPoints.toParArray.map(tp => PointMap(tp, pointLatY, pointLonX, rMax_nmi)).maxBy(x => x._3)
    } else {
      this.trackPoints.map(tp => PointMap(tp, pointLatY, pointLonX, rMax_nmi)).maxBy(x => x._3)
    }
    return ret
  }

  def PointMap(tp:TrackPoint, pointLatY:Double, pointLonX:Double, rMax_nmi:Double):(Double, Double, Int) = {
    val distance_nmi = HurricaneUtilities.haversine_degrees_to_meters(pointLatY, pointLonX, tp.eyeLat_y, tp.eyeLon_x) / 1000 * 0.539957
    val angleToCenter = HurricaneUtilities.calc_bearing_great_circle(tp.eyeLat_y, tp.eyeLon_x, pointLatY, pointLonX)
    val maxWind = if (distance_nmi <= 360) {
      nws.calcWindspeed(distance_nmi, tp.eyeLat_y, tp.fSpeed_kts, rMax_nmi, angleToCenter, tp.heading.getOrElse(0.0), tp.maxWind_kts.get, tp.gwaf)
    } else {
      0
    }

    return (pointLatY, pointLonX, math.min(math.max(maxWind, 0), 255).toInt)
  }
}
