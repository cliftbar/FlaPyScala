package xcb_app

/**
  * Created by cameron.barclift on 5/12/2017.
  */
package object hurricaneNws23 {
  val Pw_SPH_kPa = 100.8
  val Pw_PMH_kPa = 102.0
  val Pw_SPH_inhg = 29.77
  val Pw_PMH_inhg = 30.12
  val Rho0_kPa = 101.325  // Mean Sea Level Pressure
  val KmToNmi = 0.539957
  val MpsToKts = 1.94384
  val KpaToInhg = 0.2953
  val MbToInhg = 0.02953

  /**
    * Linear interpolation function, returns y for given x interpolated over range x1,y1 to x2,y2
    * @param x
    * @param x1
    * @param x2
    * @param y1
    * @param y2
    * @return y
    */
  def linearInterpolation(x:Double, x1:Double, x2:Double, y1:Double, y2:Double):Double = {
    return ((y2 - y1) / (x2 - x1)) * (x - x1)
  }

  /**
    * Calculates the radial decay factor for a given radius.
    * rMax_nmi < r_nmi: NWS 23 pdf page 53, page 27, Figure 2.12, empirical fit
    * rMax_nmi > r_nmi: NWS 23 pdf page 54, page 28, Figure 2.13, empirical fit (logistic regression)
    * @param r_nmi
    * @param rMax_nmi
    * @return Radial Decay factor representing windspeed weakening as radial distance increases
    */
  def radialDecay(r_nmi:Double, rMax_nmi:Double):Double = {
    val ret = if (r_nmi >= rMax_nmi) {
      // NWS 23 pdf page 53
      val slope = (-0.051 * math.log(rMax_nmi)) - 0.1757
      val intercept = (0.4244 * math.log(rMax_nmi)) + 0.7586
      (slope * math.log(r_nmi)) + intercept
    } else {
      // NWS 23 pdf page 54
      // 1.01231578 / (1 + math.exp(-8.612066494 * ((r_nmi / float(rmax_nmi)) - 0.678031222)))
      // this is a concession for modeling time series, where everything within the max wind radius is expected to experience the max wind radius while the storm translates
      1
    }

    // keep radial decay between 0 and 1
    return math.max(math.min(ret, 1), 0)
  }

  /**
    * Calculate the coriolis factor for a given latitude
    * @param lat_deg
    * @return coriolis factor
    */
  def coriolisFrequency(lat_deg:Double):Double = {
    val w = 2.0 * math.Pi / 24
    return 2.0 * w * math.sin(math.toRadians(lat_deg))
  }

  /**
    * NWS 23 pdf page 50, page 24, figure 2.10, emperical relationship (linear regression)
    * This is for the PMH, We can also improve this relationship
    * @param lat_deg
    * @return K factor representing air density effects for units Knots, In. Hg
    */
  def kDensityCoefficient(lat_deg:Double):Double = {
    return 69.1952184 / (1 + math.exp(0.20252 * (lat_deg - 58.72458)))
  }

  /**
    * NWS 23 pdf page 49, page 23, equation 2.2
    * @param pw_InHg Peripheral Pressure, pressure at edge of storm, should be near MSLP, In. Hg
    * @param cp_InHg Central Pressure in In. Hg
    * @param r_nmi Radius from center of storm in nautical miles.  Use Radius of max winds (Rmax) to get maximum gradient wind
    * @param lat_deg Latitude of hurricane eye
    * @return Gradient Wind at point in Knots
    */
  def gradientWindAtRadius(pw_InHg:Double , cp_InHg:Double , r_nmi:Double , lat_deg:Double ):Double = {
    val k = kDensityCoefficient(lat_deg)
    val f = coriolisFrequency(lat_deg)

    return (k * math.pow((pw_InHg - cp_InHg), 0.5)) - ((r_nmi * f) / 2)
  }

  /**
    * Emperical inflow angle calculation of PMH
    * NWS 23 pdf page 55
    * NOAA_NWS23_Inflow_Calc.xlsx
    * @param r_nmi Radius from the center of the storm in Nautical Miles
    * @param rMax_nmi Radius of maximum winds in Nautical Miles
    * @return Inflow angle
    */
  def inflowAngle(r_nmi:Double, rMax_nmi:Double):Double = {
    val rPhiMax = (3.0688 * rMax_nmi) - 2.7151

    val phi = if (r_nmi < rPhiMax) {
      val a = 11.438 * math.pow(rMax_nmi, -1.416)
      val b = (1.1453 * rMax_nmi) + 1.4536
      val phiMax = 9.7043566358 * math.log(rMax_nmi) - 2.7295806727
      phiMax / (1 + math.exp(-1 * a * (r_nmi - b))) ///return this
    } else {
      val rNmiUse = math.min(r_nmi, 130)

      val x1 = (0.0000896902 * rMax_nmi * rMax_nmi) - (0.0036924418 * rMax_nmi) + 0.0072307906
      val x2 = (0.000002966 * rMax_nmi * rMax_nmi) - (0.000090532 * rMax_nmi) - 0.0010373287
      val x3 = (-0.0000000592 * rMax_nmi * rMax_nmi) + (0.0000019826 * rMax_nmi) - 0.0000020198
      val c = (9.7043566341 * math.log(rMax_nmi)) - 2.7295806689

      val phiIntermediate = (x3 * math.pow((rNmiUse - rPhiMax), 3)) + (x2 * math.pow((rNmiUse - rPhiMax), 2)) + (x1 * (rNmiUse - rPhiMax)) + c

      val phiTemp = if (130 < r_nmi && r_nmi < 360) { // justification on NWS23 pdf page 287 page 263
        val deltaPhi = linearInterpolation(r_nmi, 130, 360, phiIntermediate, (phiIntermediate - 2))
        phiIntermediate + deltaPhi
      } else if (360 <= r_nmi) {
        phiIntermediate - 2
      } else {
        phiIntermediate
      }

      phiTemp
    }

    return phi
  }

  /**
    * NWS 23 pdf page 51, page 25, equation 2.5
    * NWS 23 pdf page 263, page 269
    * NWS 23 pdf page 281, page 257
    * Factor for a moving hurricane, accounts for effect of forward speed on hurricane winds
    * To conversion factors: 1 kt, 0.514791 mps, 1.853248 kph, 1.151556 mph
    * @param fSpeed_kts Forward speed of the storm in Knots
    * @param r_nmi Radius from the center of the storm in Nautical Miles
    * @param rMax_nmi  Radius of maximum winds in Nautical Miles
    * @param angleFromCenter Simple angle from point to center of storm, in bearing notation (North = 0)
    * @param trackBearing Heading of track from current point to next point
    * @return Asymmetry Factor in Knots, represents the effect of storm forward movement and rotation on windspeed
    */
  def asymmetryFactor(fSpeed_kts:Double, r_nmi:Double, rMax_nmi:Double, angleFromCenter:Double, trackBearing:Double):Double = {
    val to = 1 //conversion factor
    val phi_r = inflowAngle(r_nmi, rMax_nmi)
    val phi_rmax = inflowAngle(rMax_nmi, rMax_nmi)
    val phi_beta = (phi_r - phi_rmax) % 360
    val bearing_shift = (90 - angleFromCenter + trackBearing) % 360
    val beta = (phi_beta + bearing_shift) % 360

    return 1.5 * math.pow(fSpeed_kts, 0.63) * math.pow(to, 0.37) * math.cos(math.toRadians(beta))
  }

  /**
    * Calculate the windspeed from parameters.  Maximum gradient wind calculated from a given Central and Peripheral pressure
    * @param cp_mb central pressure in Millibars
    * @param r_nmi Point radius from center of storm in Nautical Miles
    * @param lat_deg Latitude of hurricane eye
    * @param fSpeed_kts Forward speed of the storm in Knots
    * @param rMax_nmi Radius of maximum winds in Nautical Miles
    * @param angleToCenter Simple angle from point to center of storm, in bearing notation (North = 0)
    * @param trackHeading Heading of track from current point to next point
    * @param pw_kpa (102.0 kpa) Peripheral Pressure, pressure at edge of storm, in Milibars, should be near Mean Sea Level Pressue.
    * @param gwaf (0.9) Gradient Wind Adjustment Factor, semi-emprical adjustment to the Gradient Wind. Range 0.75-1.05, Generally between 0.9 and 1. NWS 23 pdf page 50, page 24, 2.2.7.2.1
    * @return Windspeed at a given radius for the storm, accounting for asymmetry, in Knots
    */
  def calcWindspeed(cp_mb:Double, r_nmi:Double, lat_deg:Double, fSpeed_kts:Double, rMax_nmi:Double, angleToCenter:Double, trackHeading:Double, pw_kpa:Double = Pw_PMH_kPa, gwaf:Double = 0.9):Double = {
    val cp_InHg = cp_mb * MbToInhg
    val pw_InHg = pw_kpa * KpaToInhg

    //Calculate the Maximum Gradient Windspeed from Central and Peripheral pressure, 10m-10min Average
    val vgxMax_kts = gradientWindAtRadius(pw_InHg, cp_InHg, rMax_nmi, lat_deg)

    // call calcWindspeed with the calculated Maximum Gradient Wind
    return calcWindspeed(r_nmi, lat_deg, fSpeed_kts, rMax_nmi, angleToCenter, trackHeading, vgxMax_kts, gwaf)
  }

  /**
    * Calculate the windspeed from parameters.
    * @param r_nmi Point radius from center of storm in Nautical Miles
    * @param lat_deg Latitude of hurricane eye
    * @param fSpeed_kts Forward speed of the storm in Knots
    * @param rMax_nmi Radius of maximum winds in Nautical Miles
    * @param angleToCenter Simple angle from point to center of storm, in bearing notation (North = 0)
    * @param trackHeading Heading of track from current point to next point
    * @param vgxMax_kts Maximum Gradient Windspeed, or Max Windspeed of the storm at the current time step, in Knots
    * @param gwaf (0.9) Gradient Wind Adjustment Factor, semi-emprical adjustment to the Gradient Wind. Range 0.75-1.05, Generally between 0.9 and 1. NWS 23 pdf page 50, page 24, 2.2.7.2.1
    * @return
    */
  def calcWindspeed(r_nmi:Double, lat_deg:Double, fSpeed_kts:Double, rMax_nmi:Double, angleToCenter:Double, trackHeading:Double, vgxMax_kts:Double, gwaf:Double):Double = {
    val radial_decay_factor = radialDecay(r_nmi, rMax_nmi)
    val asym = asymmetryFactor(fSpeed_kts, r_nmi, rMax_nmi, angleToCenter, trackHeading)

    // apply all factors and return windspeed at point
    return (vgxMax_kts * gwaf * radial_decay_factor) + asym
  }

}
