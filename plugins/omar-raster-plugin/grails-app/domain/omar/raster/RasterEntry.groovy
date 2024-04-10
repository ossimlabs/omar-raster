package omar.raster

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.WKTReader
//import org.hibernate.spatial.GeometryType

// import org.joda.time.DateTime
// import org.joda.time.DateTimeZone
import java.sql.Timestamp

import omar.core.DateUtil

//import org.jadira.usertype.dateandtime.joda.PersistentDateTime

import omar.raster.tags.CountryCodeTag
import omar.raster.tags.FileTypeTag
import omar.raster.tags.MissionIdTag
import omar.raster.tags.ProductIdTag
import omar.raster.tags.SensorIdTag
import omar.raster.tags.TargetIdTag

import groovy.util.logging.Slf4j

import org.apache.commons.io.FilenameUtils


@Slf4j
class RasterEntry
{
  def grailsApplication

  String entryId
  String excludePolicy
  Long width
  Long height
  Integer numberOfBands

  Integer numberOfResLevels
  String gsdUnit
  Double gsdX
  Double gsdY

  Integer bitDepth
  String dataType
  String tiePointSet
  String indexId

  /** **************** BEGIN ADDING TAGS FROM MetaData to here  ******************/
  String filename
  String imageId
  String targetId
  String productId
  String sensorId
  String missionId
  String imageCategory
  String imageRepresentation
  Double azimuthAngle
  Double grazingAngle
  String securityClassification
  String securityCode
  String title
  String isorce
  String organization
  String description
  String countryCode
  String beNumber
  Double niirs
  String wacCode
  Double sunElevation
  Double sunAzimuth
  Double cloudCover
  BigInteger styleId
  Boolean keepForever
  Boolean crossesDateline
  MultiPolygon groundGeom
  Date acquisitionDate
  Boolean validModel


  // DateTime accessDate
  // DateTime ingestDate
  // DateTime receiveDate

  Timestamp accessDate
  Timestamp ingestDate
  Timestamp receiveDate

  BigInteger releaseId

  String fileType
  String className

  String otherTagsXml

  static transients = ["otherTagsMap"]

  Map<String, String> otherTagsMap = [:]

  /** **************** END ADDING TAGS FROM MetaData to here  ******************/

  static belongsTo = [rasterDataSet: RasterDataSet]

  static hasMany = [fileObjects: RasterEntryFile]

  Collection fileObjects

  CountryCodeTag countryCodeTag
  FileTypeTag fileTypeTag
  MissionIdTag missionIdTag
  ProductIdTag productIdTag
  SensorIdTag sensorIdTag
  TargetIdTag targetIdTag

  static namedQueries = {
    compositeId { compositeId ->
      or {
        if ( compositeId ==~ /\d+/ )
        {
          eq( 'id', compositeId as Long )
        }
        eq( 'indexId', compositeId )
        eq( 'title', compositeId )
      }
    }
  }

  static mapping = {
    cache true
    autowire true
    id generator: 'identity'
    accessDate index: 'raster_entry_access_date_idx', sqlType: "timestamp with time zone" /*, type: PersistentDateTime*/
    acquisitionDate index: 'raster_entry_acquisition_date_idx' , sqlType: "timestamp with time zone"
    beNumber index: 'raster_entry_be_number_idx'
    className index: 'raster_entry_class_name_idx'
    countryCode index: 'raster_entry_country_code_idx'
    entryId index: 'raster_entry_entry_id_idx'
    fileType index: 'raster_entry_filetype_idx'
    filename index: 'raster_entry_filename_idx'
    groundGeom /*type: GeometryType, */ sqlType: 'geometry(MultiPolygon, 4326)'
    imageCategory index: 'raster_entry_image_category_idx'
    imageId index: 'raster_entry_image_id_idx'
    imageRepresentation index: 'raster_entry_image_representation_idx'
    indexId index: 'raster_entry_index_id_idx', unique:true
    ingestDate index: 'raster_entry_ingest_date_idx', sqlType: "timestamp with time zone" /*, type: PersistentDateTime*/
    missionId index: 'raster_entry_mission_id_idx'
    niirs index: 'raster_entry_niirs_idx'
    otherTagsXml type: 'text'//, index: 'raster_entry_metadata_other_tags_idx'
    productId index: 'raster_entry_product_id_idx'
    rasterDataSet index: 'raster_entry_raster_data_set_idx'
    receiveDate index: 'raster_entry_receive_date_idx' , sqlType: "timestamp with time zone" /*, type: PersistentDateTime*/
    releaseId index: 'raster_entry_release_id_idx'
    securityClassification index: 'raster_entry_security_classification_idx'
    securityCode index: 'raster_entry_security_code_idx'
    sensorId index: 'raster_entry_sensor_id_idx'
    targetId index: 'raster_entry_target_id_idx'
    tiePointSet type: 'text'
    title index: 'raster_entry_title_idx'
    title wacCode: 'raster_entry_wac_code_idx'
    validModel index: 'raster_entry_valid_model_idx'
  }

  static constraints = {
    entryId()
    excludePolicy( nullable: true )
    width( min: 0l )
    height( min: 0l )
    numberOfBands( min: 0 )
    bitDepth( min: 0 )
    dataType()

    numberOfResLevels( nullable: true )
    gsdUnit( nullable: true )
    gsdX( nullable: true )
    gsdY( nullable: true )

    tiePointSet( nullable: true )

    filename( nullable: true )
    indexId( nullable: false, unique: false, blank: false )
    imageId( nullable: true, blank: false/*, unique: true*/ )
    targetId( nullable: true )
    productId( nullable: true )
    sensorId( nullable: true )
    missionId( nullable: true )
    imageCategory( nullable: true )
    imageRepresentation( nullable: true )
    azimuthAngle( nullable: true )
    grazingAngle( nullable: true )
    securityClassification( nullable: true )
    securityCode( nullable: true )
    title( nullable: true )
    niirs( nullable: true )
    isorce( nullable: true )
    wacCode( nullable: true )
    sunElevation( nullable: true )
    sunAzimuth( nullable: true )
    cloudCover( nullable: true )
    organization( nullable: true )
    description( nullable: true )
    countryCode( nullable: true )
    beNumber( nullable: true )
    accessDate( nullable: true )
    ingestDate( nullable: true )
    receiveDate( nullable: true )
    releaseId( nullable: true )
    styleId( nullable: true )
    keepForever( nullable: true )
    crossesDateline( nullable: true )
    validModel( nullable: true )


    fileType( nullable: true )
    className( nullable: true )

    otherTagsXml( nullable: true, blank: false )

    groundGeom( nullable: false )
    acquisitionDate( nullable: true )

    // Emerated Tags
    countryCodeTag(nullable: true)
    fileTypeTag(nullable: true)
    missionIdTag(nullable: true)
    productIdTag(nullable: true)
    sensorIdTag(nullable: true)
    targetIdTag(nullable: true)
  }

  def beforeInsert() {

    if ( !ingestDate )
    {
      // ingestDate = new DateTime(DateTimeZone.UTC);
      ingestDate = Calendar.getInstance(TimeZone.getTimeZone('GMT')).time.toTimestamp()

      if ( !indexId )
      {
        def mainFile = rasterEntry.rasterDataSet.getFileFromObjects( "main" )
        if ( mainFile )
        {
          def value = "${entryId}-${mainFile}"
          indexId = mainFile.omarIndexId;
        }
      }
      if(!receiveDate)
      {
        receiveDate = ingestDate;
      }
    }
    true
  }

  def getGeometryCenter()
  {
    def result = [:]
    if(groundGeom)
    {
      def point = groundGeom.centroid
      result.x = point.x
      result.y = point.y
    }

    result
  }
  def getGeometryBounds()
  {
    def result = [:]

    if(groundGeom)
    {
      def envelope = groundGeom.envelopeInternal
      result.minx = envelope.minX
      result.miny = envelope.minY
      result.maxx = envelope.maxX
      result.maxy = envelope.maxY
    }

    result
  }
  def adjustAccessTimeIfNeeded(def everyNHours = 24)
  {
    if ( !accessDate )
    {
      // accessDate = new DateTime(DateTimeZone.UTC);
      accessDate = Calendar.getInstance(TimeZone.getTimeZone('GMT')).time.toTimestamp()
    }
    else
    {
      // DateTime current = new DateTime(DateTimeZone.UTC);
      // long currentAccessMil = accessDate.getMillis()
      // long currentMil = current.getMillis()

      def current = Calendar.getInstance(TimeZone.getTimeZone('GMT')).time.toTimestamp()
      long currentAccessMil = accessDate.time()
      long currentMil = current.time()

      double millisPerHour = 3600000 // 60*60*1000  <seconds>*<minutes in an hour>*<milliseconds>
      double hours = ( currentMil - currentAccessMil ) / millisPerHour
      if ( hours > everyNHours )
      {
        accessDate = current
      }
    }
  }

  def getFileFromObjects(def type)
  {
    return fileObjects?.find { it.type == type }
  }

  def getMetersPerPixel()
  {
    // need to check unit type but for mow assume meters
    return gsdY; // use Y since X may decrease along lat.
  }

  def getMainFile()
  {
    def mainFile = null//rasterDataSet?.fileObjects?.find { it.type == 'main' }

    if ( !mainFile )
    {
      //mainFile = org.ossim.omar.raster.RasterFile.findByRasterDataSetAndType(rasterDataSet, "main")

      mainFile = RasterFile.createCriteria().get {
        eq( "type", "main" )
        createAlias( "rasterDataSet", "d" )
        eq( "rasterDataSet", this.rasterDataSet )
      }

    }

    return mainFile
  }

  def getAssociationType(def type)
  {
    def tempFile = RasterEntryFile.createCriteria().get {
      eq( "type", "${type}" )
      createAlias( "rasterEntry", "r" )
      eq( "rasterEntry", this )
    }

    tempFile;
  }

  def getHistogramFile()
  {
    def result = getFileFromObjects( "histogram" )?.name
    if ( !result )
    {
      result = mainFile?.name
      if ( result )
      {
        def nEntries = rasterDataSet?.rasterEntries?.size() ?: 1
        def ext = result.substring( result.lastIndexOf( "." ) )
        if ( ext )
        {
          if ( nEntries > 1 )
          {
            result = result.replace( ext, "_e${entryId}.his" )
          }
          else
          {
            result = result.replace( ext, ".his" )
          }
        }
        else
        {
          if ( nEntries > 1 )
          {
            result = result + "_e${entryId}.his"
          }
          else
          {
            result = result + ".his"
          }
        }
      }
      //
      result
    }

    result
  }

  static RasterEntry initRasterEntry(def rasterEntryNode, RasterEntry rasterEntry = null) {
    rasterEntry = rasterEntry ?: new RasterEntry()

    rasterEntry.entryId = rasterEntryNode.entryId?.text()?.trim()
    rasterEntry.width = rasterEntryNode?.width?.toLong()
    rasterEntry.height = rasterEntryNode?.height?.toLong()
    rasterEntry.numberOfBands = rasterEntryNode?.numberOfBands?.toInteger()
    rasterEntry.numberOfResLevels = rasterEntryNode?.numberOfResLevels?.toInteger()
    rasterEntry.bitDepth = rasterEntryNode?.bitDepth?.toInteger()
    rasterEntry.dataType = rasterEntryNode?.dataType
    if (rasterEntryNode?.TiePointSet) {
      rasterEntry.tiePointSet = "<TiePointSet><Image><coordinates>${rasterEntryNode?.TiePointSet.Image.coordinates.text().replaceAll("\n", "")}</coordinates></Image>"
      rasterEntry.tiePointSet += "<Ground><coordinates>${rasterEntryNode?.TiePointSet.Ground.coordinates.text().replaceAll("\n", "")}</coordinates></Ground></TiePointSet>"
    }
    def gsdNode = rasterEntryNode?.gsd
    def dx = gsdNode?.@dx?.text()
    def dy = gsdNode?.@dy?.text()
    def gsdUnit = gsdNode?.@unit.text()
    if (dx && dy && gsdUnit) {
      rasterEntry.gsdX = (dx != "nan") ? dx?.toDouble() : null
      rasterEntry.gsdY = (dy != "nan") ? dy?.toDouble() : null
      rasterEntry.gsdUnit = gsdUnit
    }

    rasterEntry.groundGeom = initGroundGeom(rasterEntryNode?.groundGeom)
    rasterEntry.acquisitionDate = initAcquisitionDate(rasterEntryNode)

/*
    if ( rasterEntry.groundGeom && !rasterEntry.tiePointSet )
    {
      def groundGeom = rasterEntry?.groundGeom.geom
      def w = rasterEntry.width as double
      def h = rasterEntry.height as double
      if ( groundGeom.numPoints() >= 4 )
      {
        rasterEntry.tiePointSet = "<TiePointSet><Image><coordinates>0.0,0.0 ${w},0.0 ${w},${h} 0.0,${h}</coordinates></Image><Ground><coordinates>"
        for ( def i in ( 0..<4 ) )
        {
          def point = groundGeom.getPoint( i );
          rasterEntry.tiePointSet += "${point.x},${point.y}"

          if ( i != 3 )
          {
            rasterEntry.tiePointSet += " "
          }
        }
        rasterEntry.tiePointSet += "</coordinates></Ground></TiePointSet>"
      }
    }
    */
    for (def rasterEntryFileNode in rasterEntryNode.fileObjects?.RasterEntryFile) {
      def obj = rasterEntry?.fileObjects?.find { it.name == rasterEntryFileNode?.name?.text() }
      if (!obj) {
        RasterEntryFile rasterEntryFile = RasterEntryFile.initRasterEntryFile(rasterEntryFileNode)
        if (rasterEntryFile) {
          rasterEntry.addToFileObjects(rasterEntryFile)
        }
      }
    }
    def metadataNode = rasterEntryNode.metadata
    def mainFile = rasterEntry.rasterDataSet.getFileFromObjects("main")

    def filename = mainFile?.name?.trim()

    if (!rasterEntry.filename && filename) {
      rasterEntry.filename = filename
    }

    initRasterEntryMetadata(metadataNode, rasterEntry)

    if (rasterEntry?.grailsApplication?.config?.stager?.includeOtherTags) {
      initRasterEntryOtherTagsXml(rasterEntry)
    }

    if (!rasterEntry.indexId) {
      def indexIdKey = "${rasterEntry.entryId}-${filename}"
      def indexIdValue = indexIdKey.encodeAsSHA256()
      // println "${indexIdKey}=${indexIdValue}"
      rasterEntry.indexId = indexIdValue
    }
    if (rasterEntry.validModel == null) {
      rasterEntry.validModel = false
    }

    /* HACK ALERT - START */
    def omdFile = "${FilenameUtils.removeExtension(filename)}.omd" as File

    if (omdFile?.exists()) {
      def kwl = omdFile?.readLines()?.inject([:]) { a, b ->
        def c = b.split(':')?.collect { it.trim() }

        a[c[0]] = c[1]
        a
      }

      if (kwl["ground_geom_${rasterEntry?.entryId}"]) {
        rasterEntry?.groundGeom = new WKTReader().read(kwl["ground_geom_${rasterEntry?.entryId}"])
        rasterEntry?.groundGeom.setSRID(4326)
      }

      if (kwl["mission_id"]) {
        rasterEntry?.missionId = kwl["mission_id"]
        rasterEntry.missionIdTag = MissionIdTag.findOrSaveWhere(name: rasterEntry?.missionId)
      }
    }
    /* HACK ALERT - END */

    if (rasterEntry?.missionId == 'SkySat') {
      def skysatFile = filename as File
      try {
        def hasScid = skysatFile?.name =~ /(ss[c]?)0*([0-9]?[1-9]?[0-9]+)/

        if (hasScid.find()) {
          def scid = hasScid.group(1) + hasScid.group(2)

          if (!scid?.contains("c")) {

            scid = "ssc${hasScid.group(2)}"
          }

          scid.toUpperCase()
        } else {
          System.err.println("Can't get the isorce for ${filename}")
        }
      }
      catch (Exception e) {
        System.err.println("Can't get the isorce for ${filename}")
      }

      return rasterEntry
    }

  }

  static Geometry initGroundGeom(def groundGeomNode)
  {
    def wkt = groundGeomNode?.text().trim()
    def srs = groundGeomNode?.@srs?.text().trim()
    def groundGeom = null

    if ( wkt && srs )
    {
      try
      {
        srs -= "epsg:"

        //def geomString = "SRID=${srs};${wkt}"

        //groundGeom = Geometry.fromString(geomString)
        groundGeom = new WKTReader().read( wkt )
        groundGeom.setSRID( Integer.parseInt( srs ) )
      }
      catch ( Exception e )
      {
        System.err.println( "Cannt create geom for: srs=${srs} wkt=${wkt}" )
      }

    }

    return groundGeom
  }

  static initRasterEntryMetadata(def metadataNode, def rasterEntry)
  {
//    if ( !rasterEntry.metadata )
//    {
//      rasterEntry.metadata = new RasterEntryMetadata()
//      rasterEntry.metadata.rasterEntry = rasterEntry
//    }

    for ( def tagNode in metadataNode.children() )
    {

      if ( tagNode.children().size() > 0 )
      {
        def name = tagNode.name().toString().toUpperCase()

        switch ( name )
        {
//          case "DTED_ACC_RECORD":
//          case "ICHIPB":
//          case "PIAIMC":
//          case "RPC00B":
//          case "STDIDC":
//          case "USE00A":
//            break
          default:
            initRasterEntryMetadata( tagNode, rasterEntry )
        }
      }
      else
      {
        def name = tagNode.name().toString().trim()
        def value = tagNode.text().toString().trim()
// Need to add following check in there
//        if ( !key.startsWith("LINE_NUM") &&
//            !key.startsWith("LINE_DEN") &&
//            !key.startsWith("SAMP_NUM") &&
//            !key.startsWith("SAMP_DEN") &&
//            !key.startsWith("SECONDARY_BE") &&
//            !key.equals("ENABLED") &&
//            !key.equals("ENABLE_CACHE")

        if ( name && value )
        {
          switch ( name.toLowerCase() )
          {
            case "filename":
              if ( value && !rasterEntry.filename)
              {
                rasterEntry.filename = value
              }
              break
            case "imageid":
              if ( value )
              {
                rasterEntry.imageId = value
              }
              break;
            case "iid":
              if ( value && !rasterEntry.imageId )
              {
                rasterEntry.imageId = value
              }
              break
            case "irep":
              if ( value && !rasterEntry.imageRepresentation )
              {
                rasterEntry.imageRepresentation = value
              }
              break
            case "imagerepresentation":
            case "image_representation":
              if ( value  )
              {
                rasterEntry.imageRepresentation = value
              }
              break
            case "tgtid":
              if ( value && !rasterEntry.targetId )
              {
                rasterEntry.targetId = value
                rasterEntry.targetIdTag = TargetIdTag.findOrSaveWhere(name: value)

              }
              break;
            case "targetid":
              if ( value )
              {
                rasterEntry.targetId = value
                rasterEntry.targetIdTag = TargetIdTag.findOrSaveWhere(name: value)
              }
              break;
            case "productid":
            case "iid1":
              if ( value )
              {
                rasterEntry.productId = value
                rasterEntry.productIdTag = ProductIdTag.findOrSaveWhere(name: value)
              }
              break;
            case "be":
            case "benumber":
              if ( value &&!rasterEntry.beNumber)
              {
                rasterEntry.beNumber = value;
              }
              break
            case "sensorid":
            case "sensor_id":
              if ( value )
              {
                rasterEntry.sensorId = value
                rasterEntry.sensorIdTag = SensorIdTag.findOrSaveWhere(name: value)
              }
              break;
            case ~/.*sensor.*/:
              if(value && !rasterEntry.sensorId )
              {
                rasterEntry.sensorId = value
                rasterEntry.sensorIdTag = SensorIdTag.findOrSaveWhere(name: value)
              }
              break;
            case "sensor_type":
              if ( value && !rasterEntry.sensorId )
              {
                rasterEntry.sensorId = value
                rasterEntry.sensorIdTag = SensorIdTag.findOrSaveWhere(name: value)
              }
              break
            case "country":
            case "countrycode":
              if ( value )
              {
                rasterEntry.countryCode = value
                rasterEntry.countryCodeTag = CountryCodeTag.findOrSaveWhere(name: value)
              }
              break
            //case "fsctlh":
            //  if(value &&!rasterEntry.securityCode)
            //  {
            //    rasterEntry.securityCode = value
            //  }
            //  break
            case "fscltx":
              if(value &&!rasterEntry.securityCode)
              {
                rasterEntry.securityCode = value
              }
              break
            case "security_code":
              if ( value && !rasterEntry.securityCode )
              {
                rasterEntry.securityCode = value
              }
              break;
            case "securityCode":
              if ( value )
              {
                rasterEntry.securityCode = value
              }
              break;
            case "mission":
            case "missionid":
              if ( value /*&& !rasterEntry.missionId*/ )
              {
                rasterEntry.missionId = value
                rasterEntry.missionIdTag = MissionIdTag.findOrSaveWhere(name: value)

              }
              break;
            case "isorce":
              if ( value && !rasterEntry.isorce )
              {
                rasterEntry.isorce = value
              }
              break;
            case "imagecategory":
            case "image_category":
              if ( value  )
              {
                rasterEntry.imageCategory = value
              }
              break
            case "icat":
              if ( value && !rasterEntry.imageCategory )
              {
                rasterEntry.imageCategory = value
              }
              break
            case "azimuthangle":
            case "azimuth_angle":
              if ( value?.isNumber() )
              {
                rasterEntry.azimuthAngle = value.toDouble().round( 2 )
              }
              break
            case "angletonorth":
              if ( value?.isNumber() && !rasterEntry.azimuthAngle )
              {
                rasterEntry.azimuthAngle = (( ( value as Double ) + 90.0 ) % 360.0).round( 2 );
              }
              break;
            case ~/.*graz_ang.*/:
            case "grazingangle":
              if ( value?.isNumber() )
              {
                rasterEntry.grazingAngle = value.toDouble().round( 2 )
              }
              break;
            case "cloud":
            case "cloud_cover":
            case "cloud_cvr":
            case "cloudcvr":
            case "cloudcover":
              if ( value?.isNumber() )
              {
                rasterEntry.cloudCover = value as Double
              }
              break;
            case "elevation_angle":
              if ( value?.isNumber() &&(rasterEntry.grazingAngle==null))
              {
                rasterEntry.grazingAngle = value.toDouble().round( 2 )
              }
              break;
            case "oblang":
              if ( value?.isNumber() && !rasterEntry.grazingAngle )
              {
                rasterEntry.grazingAngle = (90 - ( value as Double )).round( 2 )
              }
              break;
            case "obl_ang":
              if ( value?.isNumber() && !rasterEntry.grazingAngle )
              {
                rasterEntry.grazingAngle = (90 - ( value as Double )).round( 2 )
              }
              break;

            case "classification":
              if ( value &&!rasterEntry.securityClassification )
              {
                rasterEntry.securityClassification = value
              }

              break
            case "securityclassification":
              if ( value )
              {
                rasterEntry.securityClassification = value
              }
              break
            case "isclas":
              if ( value && !rasterEntry.securityClassification )
              {
                switch(value.toUpperCase())
                {
                  case "U":
                    rasterEntry.securityClassification = "UNCLASSIFIED"
                    break
                  case "R":
                    rasterEntry.securityClassification = "RESTRICTED"
                    break
                  case "S":
                    rasterEntry.securityClassification = "SECRET"
                    break
                  case "T":
                  case "TS":
                    rasterEntry.securityClassification = "TOP SECRET"
                    break
                  default:
                    rasterEntry.securityClassification = value
                    break
                }
              }
              break;
            case "title":
            case "ititle":
            case "iid2":
              if ( value && !rasterEntry.title )
              {
                rasterEntry.title = value
              }
              break;
            case "organization":
            case "oname":
              if ( value && !rasterEntry.organization )
              {
                rasterEntry.organization = value
              }
              break;
            case "description":
              if ( value && !rasterEntry.description )
              {
                rasterEntry.description = value
              }
              break;
            case "wac":
              if ( value && !rasterEntry.wacCode )
              {
                rasterEntry.wacCode = value
              }
              break;
            case "niirs":
            case "predicted_niirs":
              if ( value?.isNumber() && !rasterEntry.niirs )
              {
                rasterEntry.niirs = value as Double
              }
              break;

          // Just for testing
            case "filetype":
            case "file_type":
              if ( value && !rasterEntry.fileType )
              {
                rasterEntry.fileType = value
                rasterEntry.fileTypeTag = FileTypeTag.findOrSaveWhere(name: value)
              }
              break

            case "classname":
            case "class_name":
              if ( value && !rasterEntry.className )
              {
                rasterEntry.className = value
              }
              break
            case "validmodel":
              if ( value && (rasterEntry.validModel==null ))
              {
                rasterEntry.validModel = value as Boolean
              }
              break;
            case "acquisition_date":
            case "acquisitiondate":
              if(value && !rasterEntry.acquisitionDate)
              {
                rasterEntry.acquisitionDate = DateUtil.parseDate(value)
              }
              break;
            case "receive_date":
            case "receivedate":
              if(value && !rasterEntry.receiveDate)
              {
                rasterEntry.receiveDate = DateUtil.parseDate(value)
              }
              break;
            case "sunazimuth":
            case "sun_azimuth":
            case "sunaz":
              if(value?.isNumber() )
              {
                try{
                  rasterEntry.sunAzimuth = value.toDouble()
                }
                catch(e)
                {

                }
              }
              break;
            case "sunelevation":
            case "sun_elevation":
            case "sunel":
              if(value?.isNumber() )
              {
                try{
                  rasterEntry.sunElevation = value.toDouble()
                }
                catch(e)
                {

                }
              }
              break;
            case "crossesdateline":
              if(value)
              {
                try{
                  rasterEntry.crossesDateline = value.toBoolean()
                }
                catch(e)
                {

                }
              }
              break
            default:
              if(rasterEntry?.grailsApplication?.config?.stager?.includeOtherTags)
              {
                rasterEntry.otherTagsMap[name] = value
              }
          }
        }
      }
    }

    if ( !rasterEntry.imageId ) {
      log.debug("Generating Image ID... ${rasterEntry.indexId}")
      rasterEntry.imageId = rasterEntry.indexId
    }

    if ( !rasterEntry.title ) {
      def basename = rasterEntry.filename.toString().split("/").last()
      log.debug("Generating title... $basename")
      rasterEntry.title = basename
    }

    def csdida = metadataNode.NITF?.CSDIDA

    if ( csdida )
    {
      if ( ! rasterEntry.missionId  )
      {
        def missionId =  "${csdida?.platform_code?.text()}${csdida?.vehicle_id?.text()}"

        if ( missionId )
        {
          rasterEntry.missionId = missionId
          rasterEntry.missionIdTag = MissionIdTag.findOrSaveWhere(name: missionId)
        }
      }

      if ( ! rasterEntry.sensorId )
      {
        def sensorId =  csdida?.sensor_id?.text()

        if ( sensorId )
        {
          rasterEntry.sensorId = sensorId
          rasterEntry.sensorIdTag = SensorIdTag.findOrSaveWhere(name: sensorId)
        }
      }

      if ( ! rasterEntry.acquisitionDate  )
      {
        def time = csdida?.time?.text() + 'Z'

        if ( time )
        {
          rasterEntry.acquisitionDate = DateUtil.parseDate(time)
        }
      }
    }

    if ( ! rasterEntry.securityCode  )
    {
      def securityCode = metadataNode?.NTIF?.fsclas?.text()

      if ( securityCode )
      {
        rasterEntry.securityCode = securityCode

        switch(securityCode.toUpperCase())
        {
          case "U":
            rasterEntry.securityClassification = "UNCLASSIFIED"
            break
          case "R":
            rasterEntry.securityClassification = "RESTRICTED"
            break
          case "S":
            rasterEntry.securityClassification = "SECRET"
            break
          case "T":
          case "TS":
            rasterEntry.securityClassification = "TOP SECRET"
            break
         }
      }
    }


    // if ( ! rasterEntry.fileType )
    // {
    //   if  ( metadataNode?.NITF )
    //   {
    //     rasterEntry.fileType = 'nitf'
    //   }
    //   else if ( metadataNode?.TIFF )
    //   {
    //     rasterEntry.fileType = 'tiff'
    //   }
    // }

    //println "RASTERENTRY METADATA = ${rasterEntry.metadata}"

  }

  static initRasterEntryOtherTagsXml(RasterEntry rasterEntry)
  {
    if ( rasterEntry )
    {
      def builder = new groovy.xml.StreamingMarkupBuilder().bind {
        metadata {
          for ( def entry in rasterEntry.otherTagsMap )
          {
            "${entry.key}"( entry.value )
          }
        }
      }

      rasterEntry.otherTagsXml = builder.toString()
    }
  }

  static Date initAcquisitionDate(rasterEntryNode)
  {
    def when = rasterEntryNode?.TimeStamp?.when

    return DateUtil.parseDate( when?.text() )
  }

  static update(def file, def entryId)
  {
    def rasterFile = RasterFile.findWhere( name: file )
  }
}
