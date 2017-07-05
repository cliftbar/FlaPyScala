package xcb_app.hurricane

import xcb_app.{hurricaneNws23 => nws}
import java.io._
import java.awt.image._
import java.awt.Color
import javax.imageio.ImageIO

import scala.collection.parallel._
import scala.concurrent.forkjoin._

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

  def GetLatLonList:List[(Double,Double)] = {
    val height = this.GetHeightInBlocks
    val width = this.GetWidthInBlocks
    val grid = Array.fill(height)(Array.range(0,width)).zipWithIndex.flatMap(x => x._1.map(y => (y, x._2)).reverse).reverse
    return grid.map(x => this.GetBlockLatLon(x._1, x._2)).toList
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

  def CalcTrackpointHeadings():Unit = {
    if (this.trackPoints.length == 1) {
      this.trackPoints(0).heading = Some(0)
    } else {
      for (i <- 0 until this.trackPoints.length - 1) {
        val next_lat = this.trackPoints(i+1).eyeLat_y
        val next_lon = this.trackPoints(i+1).eyeLon_x
        val curr_lat = this.trackPoints(i).eyeLat_y
        val curr_lon = this.trackPoints(i).eyeLon_x

        val heading = HurricaneUtilities.CalcBearingNorthZero(curr_lat, curr_lon, next_lat, next_lon)
        this.trackPoints(i).heading = Some(heading)
      }

      this.trackPoints.last.heading = this.trackPoints(this.trackPoints.length - 2).heading
    }
  }

  def DoCalcs(maxDist:Int, par:Int):Unit = {
    println("DoCalcs")
    val latLonList = this.grid.GetLatLonList
    println("LatLonList")
    val parallel = if (par == -1) false else true

    val CalcedResults = if (parallel) {
      val latLonPar =latLonList.toParArray
      latLonPar.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(par))
      println("Parallel map: " + par.toString)
      latLonPar.map(x => TrackMap(x._1, x._2, maxDist)).toList
    } else {
      latLonList.map(x => TrackMap(x._1, x._2, maxDist))
    }

    println("calced")

    println("write Image")
    this.WriteToImage(CalcedResults, this.grid.GetWidthInBlocks, this.grid.GetHeightInBlocks)

    val writer = new FileWriter("testOut.txt")
    writer.write("LatY\tLonX\twind_kts\n")

    println("write txt")
    for (x <- CalcedResults) {
      writer.write(s"${x._1}\t${x._2}\t${x._3}\n")
    }

    writer.close()

    println("all written")
  }

  def TrackMap(pointLatY:Double, pointLonX:Double, maxDist:Int):(Double, Double, Int) = {
    return this.trackPoints.toArray.map(tp => PointMap(tp, pointLatY, pointLonX, maxDist)).maxBy(x => x._3)
  }

  def PointMap(tp:TrackPoint, pointLatY:Double, pointLonX:Double, maxDist:Int):(Double, Double, Int) = {
    val distance_nmi = HurricaneUtilities.haversine_degrees_to_meters(pointLatY, pointLonX, tp.eyeLat_y, tp.eyeLon_x) / 1000 * 0.539957

    val maxWind = if (distance_nmi <= maxDist) {
      val angleToCenter = HurricaneUtilities.CalcBearingNorthZero(tp.eyeLat_y, tp.eyeLon_x, pointLatY, pointLonX)
      nws.calcWindspeed(distance_nmi, tp.eyeLat_y, tp.fSpeed_kts, this.rMax_nmi.toInt, angleToCenter, tp.heading.getOrElse(0.0), tp.maxWind_kts.get, tp.gwaf)
    } else {
      0
    }

    return (pointLatY, pointLonX, math.min(math.max(maxWind, 0), 255).round.toInt)
  }

  def WriteToImage(outList: List[(Double, Double, Int)], width: Int, height: Int):Unit = {
    val pixels = outList.map(x => new Color(x._3, 0, 0, 255).getRGB)
    val pixLength = pixels.length
    val calcLength = width * height
    println(s"Pix Length: $pixLength, Calc Length: $calcLength")

    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val raster = image.getRaster
    raster.setDataElements(0, 0, width, height, pixels.toArray)
    ImageIO.write(image, "PNG", new File("OutImage.png"))
  }
}
