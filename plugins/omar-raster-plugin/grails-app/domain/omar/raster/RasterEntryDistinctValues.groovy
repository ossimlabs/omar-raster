package omar.raster
enum RasterEntryDistinctValues {
  COUNTRY_CODE("countryCode"),
  MISSION_ID ("missionId"),
  SENSOR_ID ("sensorId"),
  TARGET_ID ("targetId"),
  PRODUCT_ID ("productId")

  final String rasterDbFieldKey
  RasterEntryDistinctValues(String rasterDbFieldKey) {
    this.rasterDbFieldKey = rasterDbFieldKey
  }

}