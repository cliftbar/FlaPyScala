import xcb_app.hurricane.{HurricaneUtilities => huru}

huru.haversine_degrees_to_meters(22.2, -97.6, 24.099999999999998, -99.6) / 1000 * 0.539957

//
//hur.radialDecay(300, 15)
//hur.asymmetryFactor(200, 10, 15, -45, 280)
//hur.calcWindspeed(25, 25.0, 15, 15, 45, 280, 150, 0.9)
//for (i <- 0 until 90 by 5) {
//  println(i)
//  println(hur.calcWindspeed(50, 25.0, 15, 15, 45, i, 150, 0.9))
//}

//val ls = List(50, 25.0, 15, 15, 45, 12, 150, 0.9)
//ls.maxBy(x => x * x)
//import xcb_app.hurricane.LatLonGrid
//val test = List(0,1,2,3,4)
//val test2 = List(5,6,7,8,9)
//
//val test3 = List(test, test2)
//
//val test4 = Seq.fill(10)(Seq.range(0,10)).zipWithIndex
//val test4_1 = test4.flatMap(x => x._1.map(y => (y, x._2)))
////val test5 = test4.flatten
//
////val test4_1 = test4(1)
//val test5 = List.fill(10)(List.range(0,10)).zipWithIndex
//val test5_1 = test5(1)
//val test6 = test5.flatMap( {case (inner, outerIndex) => inner.map( innerIndex => (innerIndex, outerIndex))} )


//val bBox = new LatLonGrid(30, 10, 10, 20, 10, 10)
//
//bBox.GetBlockLatY(7)
//
//var temp = bBox.GetLatLonList
//
//temp(1)
//temp.last
//s"${temp.last._1}, ${temp.last._2}"

