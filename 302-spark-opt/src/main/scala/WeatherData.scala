object WeatherData {
  def extract(row:String) = {
    val usaf = row.substring(4,10)
    val wban = row.substring(10,15)
    val year = row.substring(15,19)
    val month = row.substring(19,21)
    val day = row.substring(21,23)
    val airTemperature = row.substring(87,92)
    val airTemperatureQuality = row.charAt(92)

    WeatherData(usaf,wban,year,month,day,airTemperature.toInt/10,airTemperatureQuality == '1')
  }
}

case class WeatherData(
  usaf:String,
  wban:String,
  year:String,
  month:String,
  day:String,
  temperature:Double,
  validTemperature:Boolean
)