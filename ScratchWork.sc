//import xcb_app.hurricane.LatLonGrid
val test = List(0,1,2,3,4)
val test2 = List(5,6,7,8,9)

val test3 = List(test, test2)

val test4 = Seq.fill(10)(Seq.range(0,10)).zipWithIndex
val test4_1 = test4.flatMap(x => x._1.map(y => (y, x._2)))
//val test5 = test4.flatten

//val test4_1 = test4(1)
val test5 = List.fill(10)(List.range(0,10)).zipWithIndex
val test5_1 = test5(1)
val test6 = test5.flatMap( {case (inner, outerIndex) => inner.map( innerIndex => (innerIndex, outerIndex))} )


//val bBox = new LatLonGrid(30, 10, 10, 20, 10, 10)
//
//bBox.GetBlockLatY(7)
//
//var temp = bBox.GetLatLonList
//
//temp(1)
//temp.last
//s"${temp.last._1}, ${temp.last._2}"




