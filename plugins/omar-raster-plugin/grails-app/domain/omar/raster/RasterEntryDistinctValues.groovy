package omar.raster
enum RasterEntryDistinctValues {
  COUNTRY_CODE("countryCode"),
  MISSION_ID ("missionId"),
  SENSOR_ID ("sensorId"),
  TARGET_ID ("targetId"),
  PRODUCT_ID ("productId"),
  FILE_TYPE("fileType")

  final String rasterDbFieldKey
  RasterEntryDistinctValues(String rasterDbFieldKey) {
    this.rasterDbFieldKey = rasterDbFieldKey
  }

  static RasterEntryDistinctValues findByValue(String value) { values().find { it.rasterDbFieldKey == value } }
}