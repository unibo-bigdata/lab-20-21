object StationData {
  def extract(row:String) = {
    def getDouble(str:String) : Double = {
      if (str.isEmpty)
        return 0
      else
        return str.toDouble
    }
    val columns = row.split(",").map(_.replaceAll("\"",""))
    val latitude = getDouble(columns(6))
    val longitude = getDouble(columns(7))
    val elevation = getDouble(columns(8))
    StationData(columns(0),columns(1),columns(2),columns(3),columns(4),columns(5),latitude,longitude,elevation,columns(9),columns(10))
  }
}

case class StationData(
  usaf:String,
  wban:String,
  name:String,
  country:String,
  state:String,
  call:String,
  latitude:Double,
  longitude:Double,
  elevation:Double,
  date_begin:String,
  date_end:String
)

