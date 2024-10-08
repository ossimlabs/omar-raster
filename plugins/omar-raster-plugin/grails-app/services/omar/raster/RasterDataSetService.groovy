package omar.raster

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.util.logging.Slf4j

import omar.core.Repository
import omar.core.HttpStatus
import omar.core.DateUtil
import omar.raster.tags.CountryCodeTag
import omar.raster.tags.FileTypeTag
import omar.raster.tags.MissionIdTag
import omar.raster.tags.ProductIdTag
import omar.raster.tags.SensorIdTag
import omar.raster.tags.TargetIdTag
import omar.stager.core.OmarStageFile
import org.apache.commons.io.FilenameUtils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import grails.gorm.transactions.Transactional
import grails.core.GrailsApplication
import grails.converters.JSON

import org.locationtech.jts.io.geojson.GeoJsonWriter

@Slf4j
@Transactional
class RasterDataSetService implements ApplicationContextAware {
    GrailsApplication grailsApplication

    def dataInfoService
    def dataSource
    def ingestService
    def stagerService
    def ingestMetricsService
    def sessionFactory
    ApplicationContext applicationContext
    def catalogIdService

    def deleteFromRepository(Repository repository) {
        def rasterDataSets = RasterDataSet.findAllByRepository(repository)

        rasterDataSets?.each { it.delete() }
    }

    private Timestamp convertToTimestamp(def v) {
        Timestamp result
        if (v instanceof String) {
            result = DateUtil.parseDate(v)?.toTimestamp()
        } else if (v instanceof Date) {
            result = v.toTimestamp()
        } else if (v instanceof Timestamp) {
            result = v
        }
    }

    void applyOverrideToRasterEntry(RasterEntry rasterEntry, HashMap overrides) {
        overrides?.each { k, v ->
            switch (k) {
                case "receiveDate":
                    rasterEntry?.receiveDate = convertToTimestamp(v)
                    break
                default:
                    if (rasterEntry?.hasProperty(k)) {
                        rasterEntry?."${k}" = v;
                    }
                    break
            }
        }

    }
    /**
     * Should for now only call with one embedded raster dataset
     */
    def addRasterXml(def xml, HashMap overrides = null) {
        def requestType = "POST"
        def requestMethod = "addRasterXml"
        HashMap result = [status   : HttpStatus.OK,
                          message  : "",
                          startTime: new Date(),
                          endTime  : new Date(),
                          metadata : [:]
        ]
        try {
            Date startTime = new Date()
            def oms = new XmlSlurper().parseText(xml)
            def omsInfoParser = applicationContext?.getBean("rasterInfoParser")
            def repository = ingestService?.findRepositoryForFile("/")
            def rasterDataSets = omsInfoParser?.processDataSets(oms, repository)
            String filename

            rasterDataSets?.each { rasterDataSet ->
                filename = rasterDataSet.mainFile?.name
                if (rasterDataSet.rasterEntries.size() > 0) {
                    try {
                        if (overrides) {
                            rasterDataSet.rasterEntries.each { rasterEntry ->
                                applyOverrideToRasterEntry(rasterEntry, overrides)
                            }
                        }
                        if (rasterDataSet.save()) {
                            //stagerHandler.processSuccessful(filename, xml)
                            result?.status = HttpStatus.OK
                            def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
                            result?.message = "Added raster ${ids}:${filename}"
                            result.metadata.missionids = rasterDataSet?.rasterEntries.collect { it.missionId } ?: []
                            result.metadata.imageids = rasterDataSet?.rasterEntries.collect { it.imageId } ?: []
                            result.metadata.sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId } ?: []
                            result.metadata.fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType } ?: []
                            result.metadata.filenames = rasterDataSet?.rasterEntries.collect { it.filename } ?: []
                            result.metadata.entryIds = rasterDataSet?.rasterEntries.collect { it.entryId } ?: []
                            result.metadata.acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate ? DateUtil.formatUTC(it.acquisitionDate).toString() : "" }.join(",")
                            result.metadata.ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate ? DateUtil.formatUTC(it.ingestDate).toString() : "" }.join(",")
                            result.metadata.bes = rasterDataSet?.rasterEntries.collect { it.beNumber }.join(",")
                        } else {
                            result?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
                            result?.message = "Unable to save XML, data probably already exists"
                            log.error(result?.message)
                            ingestService.writeErrors(filename, result?.message, result?.status)
                        }
                    }
                    catch (Exception e) {
                        result?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
                        result?.message = "Unable to save XML: ${e}"
                        log.error(result?.message)
                        ingestService.writeErrors(filename, result?.message, result?.status)
                    }
                } else {
                    result?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
                    result?.message = "No raster entries found for ${filename} - check for ground geom!"
                    log.error(result?.message)
                    ingestService.writeErrors(filename, result?.message, result?.status)
                }
            }
            oms = null
        }
        catch (e) {
            result?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
            result?.message = "Unable to process XML: ${e}"
            log.error(e)
            ingestService.writeErrors(filename, result?.message, result?.status)

        }
        finally {
            result.endTime = new Date()
            result.duration = (result.endTime.time - result.startTime.time)
        }

        result
    }

    /**
     * This service allows one to add a raster to the omar tables.
     *
     * @param httpStatusMessage Is used to populate the http response.  This will
     *                          identify the status code messages and any additional
     *                          header paramters that need to be added to the response.
     * @param filename is the file you wish to add to the OMAR tables
     */
    def addRaster(def httpStatusMessage, AddRasterCommand params) {
        String filename = params?.filename
        httpStatusMessage?.status = HttpStatus.OK
        httpStatusMessage?.message = "Added raster ${filename}"
        URI uri = new URI(filename.toString())
        String scheme = uri.scheme?.toLowerCase()
        def raster_logs
        def requestType = "GET"
        def requestMethod = "addRaster"
        Date startTime = new Date()
        def ingestDates
        def acquisitionDates
        def missionids
        def imageids
        def sensorids
        def fileTypes
        def filenames
        def isFileStaged = RasterFile.findByName(filename)

        if (!scheme || (scheme == "file")) {
            File testFile = filename as File
            if (!testFile?.exists()) {
                httpStatusMessage?.status = HttpStatus.NOT_FOUND // 404
                httpStatusMessage?.message = "File Not Found"

                log.error("🚩 Error: ${filename} ${httpStatusMessage?.status} ${httpStatusMessage?.message}")
                ingestService.writeErrors(filename, httpStatusMessage?.message, httpStatusMessage?.status)
            } else if (!testFile?.canRead()) {
                httpStatusMessage?.status = HttpStatus.FORBIDDEN //403
                httpStatusMessage?.message = "File Not Readable"

                log.error("🚩 Error: ${filename} ${httpStatusMessage?.status} ${httpStatusMessage?.message}")
                ingestService.writeErrors(filename, httpStatusMessage?.message, httpStatusMessage?.status)
            }
        }

        if (httpStatusMessage?.status == HttpStatus.OK) {
            if (!isFileStaged) {
                def xml = dataInfoService.getInfo(filename)
                def background = true;
                try {
                    background = params?.background
                }
                catch (Exception e) {
                    log.error(e)
                    ingestService.writeErrors(filename, e, httpStatusMessage?.status)
                }

                // We will add a return here temporarily but we need to refactor
                // and put the hashmap generator in a method and anything
                // else we can put in a method that makes sense
                if (!xml) {
                    httpStatusMessage?.message = "Unable to get information on file."
                    httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE // 415

                    log.error("🚩 Error: ${filename} ${httpStatusMessage?.status} ${httpStatusMessage?.message}")
                    ingestService.writeErrors(filename, httpStatusMessage?.message, httpStatusMessage?.status)

                    return httpStatusMessage
                }

                def oms = new XmlSlurper().parseText(xml)
                def omsInfoParser = applicationContext?.getBean("rasterInfoParser")
                def repository = ingestService?.findRepositoryForFile(filename)
                def rasterDataSets = omsInfoParser?.processDataSets(oms, repository)

                if (background) {
                    def result = stagerService.addFileToStage(filename, params.properties)

                    httpStatusMessage.status = result.status
                    httpStatusMessage.message = result.message

                    if (rasterDataSets?.size() > 0) {
                        rasterDataSets?.each { rasterDataSet ->
                            def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
                            missionids = rasterDataSet?.rasterEntries.collect { it.missionId } ?: []
                            imageids = rasterDataSet?.rasterEntries.collect { it.imageId } ?: []
                            sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId } ?: []
                            fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType } ?: []
                            filenames = rasterDataSet?.rasterEntries.collect { it.filename } ?: []
                            acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate }.join(",")
                            ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")

                            raster_logs = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
                                    requestMethod: requestMethod, httpStatus: httpStatusMessage?.status, message: httpStatusMessage?.message,
                                    filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
                                    ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

                            log.info raster_logs.toString()
                        }

                    }


                } else {
                    Boolean fileStaged = false

                    if (params.buildOverviews || params.buildHistograms) {
                        def result = stagerService.stageFileJni([filename               : params.filename,
                                                                 buildThumbnails        : params.buildThumbnails,
                                                                 buildOverviews         : params.buildOverviews,
                                                                 buildHistograms        : params.buildHistograms,
                                                                 overviewCompressionType: params.overviewCompressionType,
                                                                 buildHistogramsWithR0  : params.buildHistogramsWithR0,
                                                                 useFastHistogramStaging: params.useFastHistogramStaging,
                                                                 overviewType           : params.overviewType,
                                                                 thumbnailSize          : params.thumbnailSize,
                                                                 thumbnailType          : params.thumbnailType,
                                                                 thumbnailStretchType   : params.thumbnailStretchType
                        ])
                        if (result?.status >= 300) {

                            log.error(result?.message)
                            ingestService.writeErrors(filename, result?.message, result?.status)
                        }
                        httpStatusMessage.status = result.status
                        httpStatusMessage.message = result.message

                        if (rasterDataSets?.size() > 0) {
                            rasterDataSets?.each { rasterDataSet ->
                                //httpStatusMessage?.status = HttpStatus.OK
                                def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
                                //httpStatusMessage?.message = "Added raster ${ids}:${filename}"
                                missionids = rasterDataSet?.rasterEntries.collect { it.missionId } ?: []
                                imageids = rasterDataSet?.rasterEntries.collect { it.imageId } ?: []
                                sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId } ?: []
                                fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType } ?: []
                                filenames = rasterDataSet?.rasterEntries.collect { it.filename } ?: []
                                acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate }.join(",")
                                ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")

                                raster_logs = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
                                        requestMethod: requestMethod, httpStatus: httpStatusMessage?.status, message: httpStatusMessage?.message,
                                        filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
                                        ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

                                log.info raster_logs.toString()
                            }
                        }
                    } else {
                        rasterDataSets?.each { rasterDataSet ->
                            def savedRaster = true
                            try {
                                if (rasterDataSet.save()) {

                                    //stagerHandler.processSuccessful(filename, xml)
                                    //httpStatusMessage?.status = HttpStatus.OK
                                    def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
                                    httpStatusMessage?.message = "Added raster ${ids}:${filename}"
                                    missionids = rasterDataSet?.rasterEntries.collect { it.missionId } ?: []
                                    imageids = rasterDataSet?.rasterEntries.collect { it.imageId } ?: []
                                    sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId } ?: []
                                    fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType } ?: []
                                    filenames = rasterDataSet?.rasterEntries.collect { it.filename } ?: []
                                    acquisitionDates = rasterDataSet?.rasterEntries.collect {
                                        it.acquisitionDate
                                    }.join(",")
                                    ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")


                                    // TODO: This get set to a 200 status, but in reality it can fail as it is really running in the background, and could fail
                                    // if the image has already been staged
                                    raster_logs = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
                                            requestMethod: requestMethod, httpStatus: httpStatusMessage?.status, message: httpStatusMessage?.message,
                                            filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
                                            ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

                                    log.info raster_logs.toString()
                                }
                            }
                            catch (Exception e) {
                                httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
                                httpStatusMessage?.message = "${e?.message}"
                                log.error("🚩 Error: ${filename} ${httpStatusMessage?.status} ${httpStatusMessage?.message}")
                                def session = sessionFactory.currentSession
                                def transaction = session.getCurrentTransaction()
                                if (transaction.isActive()) {
                                    log.info("Initiating transaction rollback due to Exception.")
                                    transaction.rollback()
                                }
                                ingestService.writeErrors(filename, httpStatusMessage?.message, httpStatusMessage?.status)
                                throw new Exception(e)

                            }
                        }
                    }
                }
            } else {
                log.info("${filename} is already staged.")
            }
        }

        // Even if there's no logs, we still want to output the status in the logs.
        if (!params.logs) params.logs = "{}"

        def logsJson = new JsonSlurper().parseText(params.logs)
        addTotalStageTimeToLogs(logsJson)
        addTotalTimeFromAcquisitionToLogs(logsJson)
        // Some images may fail but still need to be logged and differentiated from successes.
        logsJson["ingest_status"] = httpStatusMessage?.status
        logsJson["file_name"] = filename

        def catId = generateCatalogId(filename)
        // Write out the stac spec
        writeStacJson(filename)
        logsJson["catId"] = catId

        // Print logs in JSON for ElasticSearch and Kibana parsing
        println new JsonBuilder(logsJson).toString()
//        if (httpStatusMessage?.status != 200)
//        {
//            log.error("🚩 Error: ${filename} ${httpStatusMessage?.status} ${httpStatusMessage.message}")
//        }

        httpStatusMessage
    }

    private static double convertDurationToSeconds(TimeDuration duration) {
        return duration.toMilliseconds() / 1000
    }

    private static String ACQUISITION_DATE_KEY = "acquisition_date"
    /**
     * Adds the total time from when the image was acquired to when the ingest pipeline is completed to the JSON logs object.
     * This method assumes the current time is when the pipeline is completed.
     * @param logsJson This param must be a JSON log containing the {@code ACQUISITION_DATE_KEY}
     */
    private static void addTotalTimeFromAcquisitionToLogs(logsJson) {
        // Calculate total time (acq-time to now)
        Date pipelineFinishTime = new Date()

        if (logsJson[ACQUISITION_DATE_KEY]) {
            Date imageAcquiredTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", logsJson[ACQUISITION_DATE_KEY])
            TimeDuration totalTimeFromAcquisition = TimeCategory.minus(pipelineFinishTime, imageAcquiredTime)
            logsJson["time_from_acquisition"] = convertDurationToSeconds(totalTimeFromAcquisition)
        }
    }

    private static String PIPELINE_START_DATE_KEY = "ingest_date"
    /**
     * Adds the total time in the ingest pipeline apps (omar-sqs/avro/stager) to the JSON logs object.
     * This method assumes the current time is when the pipeline is completed.
     * @param logsJson This param must be a JSON log containing the {@code PIPELINE_START_DATE_KEY}
     */
    private static void addTotalStageTimeToLogs(logsJson) {
        // Calculate stage time (total time in all three apps)
        Date pipelineFinishTime = new Date()

        if (logsJson[PIPELINE_START_DATE_KEY]) {
            Date pipelineStartTime = new Date().parse("yyyy-MM-dd HH:mm:ss.SSS", logsJson[PIPELINE_START_DATE_KEY])
            TimeDuration totalStagingTime = TimeCategory.minus(pipelineFinishTime, pipelineStartTime)
            logsJson["staging_time"] = convertDurationToSeconds(totalStagingTime)
        }
    }

    def removeRaster(def httpStatusMessage, def params) {
        def status = false
        String filename = params?.filename //as File

        def rasterFile = RasterFile.findByNameAndType(filename, "main")
        if (rasterFile) {
            httpStatusMessage?.status = HttpStatus.OK
            def ids = rasterFile?.rasterDataSet?.rasterEntries?.collect { it?.id }?.join(",")

            if (params.deleteFiles?.toBoolean()) {
                def files = []
                rasterFile?.rasterDataSet?.fileObjects.each() { files << it.name }
                rasterFile?.rasterDataSet?.rasterEntries.each() {
                    it.fileObjects.each() { files << it.name }
                }

                files.each() {
                    def file = it as File
                    if (file?.isFile() && file.name != filename) {
                        File fileToRemove = file as File
                        if (fileToRemove.canWrite()) {
                            if (fileToRemove.isDirectory()) {
                                fileToRemove.deleteDir()
                            } else {
                                fileToRemove.delete()
                            }
                            if (!fileToRemove.exists()) {
                                log.info("Deleted ${file}")
                            } else {
                                log.info("Unable to delete ${file}")
                            }
                        }
                    } else {
                        log.info("Don't have permissions to delete ${file}")
                    }
                }
            }

            if (params.deleteSupportFiles?.toBoolean()) {
                def files = rasterFile?.rasterDataSet?.fileObjects?.grep { it.type != 'main' }

                files.each() {
                    def file = it as File
                    if (file?.isFile() && file.name != filename) {
                        File fileToRemove = file as File
                        if (fileToRemove.canWrite()) {
                            if (fileToRemove.isDirectory()) {
                                fileToRemove.deleteDir()
                            } else {
                                fileToRemove.delete()
                            }
                            if (!fileToRemove.exists()) {
                                log.info("Deleted ${file.name}")
                            } else {
                                log.info("Unable to delete ${file.name}")
                            }
                        }
                    } else {
                        log.info("Don't have permissions to delete ${file.name}")
                    }
                }
            }
            def catId = rasterFile?.rasterDataSet?.getCatId()
            rasterFile?.rasterDataSet?.delete(flush: true)
            httpStatusMessage?.message = "removed raster ${catId} - ${ids}:${filename}"
        } else {
            httpStatusMessage?.status = HttpStatus.NOT_FOUND
            httpStatusMessage?.message = "Raster file does not exist in the database: ${filename}"
            log.error(httpStatusMessage?.message)
            ingestService.writeErrors(filename, httpStatusMessage?.message, httpStatusMessage?.status)
        }
    }


    def deleteRaster(def httpStatusMessage, def params) {
        removeRaster(httpStatusMessage, params)
    }


    def getFileProcessingStatus(GetRasterFilesProcessingCommand cmd) {
        HashMap result = [
                results   : [],
                pagination: [
                        count : 0,
                        offset: 0,
                        limit : 0
                ]
        ]

        try {
            result.pagination.count = OmarStageFile.count()
            result.pagination.offset = cmd.offset ?: 0
            Integer limit = cmd.limit ?: result.pagination.count
            def files = OmarStageFile.list([offset: result.pagination.offset, max: limit])
            files?.each { record ->
                result.results <<
                        [
                                filename               : record.filename,
                                processId              : record.processId,
                                status                 : record.status.name,
                                statusMessage          : record.statusMessage,
                                buildThumbnails        : record.buildThumbnails,
                                buildOverviews         : record.buildOverviews,
                                buildHistograms        : record.buildHistograms,
                                buildHistogramsWithR0  : record.buildHistogramsWithR0,
                                useFastHistogramStaging: record.useFastHistogramStaging,
                                overviewCompressionType: record.overviewCompressionType,
                                overviewType           : record.overviewType,
                                thumbnailSize          : record.thumbnailSize,
                                thumbnailType          : record.thumbnailType,
                                thumbnailStretchType   : record.thumbnailStretchType,
                                dateCreated            : record.dateCreated,
                        ]
            }

            result.pagination.limit = limit
        }
        catch (e) {
            result.status = HttpStatus.BAD_REQUEST
            result.message = e.toString()
            result.remove("results")
            result.remove("pagination")
        }

        result
    }

    def getRasterFiles(GetRasterFilesCommand cmd) {
        HashMap result = [results: []]

        def files = RasterEntry.compositeId(cmd.id)

        RasterEntry entry = files?.get()
        def fileList = []
        if (entry) {
            entry.fileObjects.each { fileObject ->
                fileList << [name: fileObject.name, type: fileObject.type]
            }
            entry?.rasterDataSet?.fileObjects.each { fileObject ->
                fileList << [name: fileObject.name, type: fileObject.type]
            }
        }
        result.results = fileList

        result
    }

    List<String> updateAccessDates(List<String> rasterEntryIds) {
        List<String> updatedRasters = []
        rasterEntryIds.forEach {
            RasterEntry re = RasterEntry.get(it)
            if (updateLastAccess(re)) updatedRasters.add(it)
        }
        println "DEBUG: Updated rasters = $updatedRasters"
        return updatedRasters
    }

    private static boolean updateLastAccess(RasterEntry re) {
        if (re.accessDate == null) {
            re.accessDate = new Timestamp(System.currentTimeMillis())
            re.save()
            return true
        }

        long oneDay = new TimeDuration(1, 0, 0, 0, 0).toMilliseconds()
        long millisecondsSinceAccessed = System.currentTimeMillis() - re.accessDate.getTime()

        if (millisecondsSinceAccessed > oneDay) {
            re.accessDate = new Timestamp(System.currentTimeMillis())
            re.save()
            return true
        }
        return false
    }

    def getDistinctValues(def params) {
        def results = []

        def criteria = {
            projections {
                property('name')
            }
            order('name')
        }

        switch (RasterEntryDistinctValues.findByValue(params?.property)) {
            case RasterEntryDistinctValues.COUNTRY_CODE:
                results = CountryCodeTag.withCriteria(criteria)
                break
            case RasterEntryDistinctValues.FILE_TYPE:
                results = FileTypeTag.withCriteria(criteria)
                break
            case RasterEntryDistinctValues.MISSION_ID:
                results = MissionIdTag.withCriteria(criteria)
                break
            case RasterEntryDistinctValues.PRODUCT_ID:
                results = ProductIdTag.withCriteria(criteria)
                break
            case RasterEntryDistinctValues.SENSOR_ID:
                results = SensorIdTag.withCriteria(criteria)
                break
            case RasterEntryDistinctValues.TARGET_ID:
                results = TargetIdTag.withCriteria(criteria)
                break
            default:
                log.warn("Invalid property for getDistinctValues. Value passed: ${params.property}")
        }

        return results
    }

    Boolean hasSICD(String indexId) {
        RasterEntry sidd = RasterEntry.findByIndexId(indexId)

        List<File> sicdFiles = new File(sidd.filename)?.parentFile?.listFiles({
            it?.name?.toUpperCase() ==~ /.*SICD.*\.NTF/
        } as FileFilter)

        sicdFiles?.size() > 0
    }

    def findByCatId(String catId) {
        def sql = Sql.newInstance(dataSource)

        def query = """select re.mission_id, re.filename, st_area( ground_geom::geography ) / 1000^2 as "area",
                st_astext(ground_geom) as geometry 
                from raster_entry re, raster_data_set rds 
                where re.raster_data_set_id=rds.id and cat_id=?"""

        def results = sql.firstRow(query, catId)
        def json = results as JSON

        if (!json) {
            json = [message: "No RasterDataSet found for catId: ${catId}"] as JSON
        }

        sql?.close()
        json
    }

    def findByFilePath(String filePath) {
        def rasterDataSet = RasterFile.where {
            name == filePath && type == 'main'
        }.find()?.rasterDataSet

        def json
        def results = getSatelliteIdAndMissionIdByFilename(filename)

        if (rasterDataSet && results?.missionId) {
            json = formatAsStacCollection(rasterDataSet)
        } else {
            json = [message: "No RasterDataSet found for filePath: ${filePath}"] as JSON
        }

        [contentType: 'application/json', text: json]
    }

    def writeStacJson(String filename) {
        File jsonFile = "${filename}_discovery.json" as File
        def returnJson = [:]
        try {
            def rasterDataSet = RasterFile.where {
                name == filename && type == 'main'
            }.find()?.rasterDataSet

            def json

            def results = getSatelliteIdAndMissionIdByFilename(filename)

            if (rasterDataSet && results?.missionId) {
                json = formatAsStacCollection(rasterDataSet)
                jsonFile.withWriter { Writer out ->
                    out.println json
                }
                log.info("Created STAC spec: ${jsonFile}")
                returnJson.text = "Successfully created ${jsonFile}."
            } else {
                log.error("Error creating STAC. Couldn't find rasterDataset: ${filename}")
                returnJson.error = "Couldn't find rasterDataset: ${filename}"
            }
        }
        catch (Exception e) {
            log.error("Failed to write STAC spec for ${filename}. ${e.message}")
            ingestService.writeErrors(filename, "Failed to write STAC Spec", HttpStatus.INTERNAL_SERVER_ERROR)
            returnJson.error = "Failed to write STAC spec for ${filename}. ${e.message}"
        }
        return JsonOutput.toJson(returnJson)
    }


    def formatAsStacCollection(RasterDataSet rasterDataSet) {
        def data = [
                type          : 'FeatureCollection',
                features      : rasterDataSet?.rasterEntries?.collect { rasterEntry ->
                    def coords = rasterEntry?.groundGeom?.envelope?.coordinates
                    def geoJsonWriter = new GeoJsonWriter()

                    [
                            id             : rasterEntry?.rasterDataSet?.catId,
                            bbox           : [
                                    coords?.x?.min(),
                                    coords?.y?.min(),
                                    coords?.x?.max(),
                                    coords?.y?.max(),
                            ],
                            type           : 'Feature',
                            links          : [],
                            assets         : [:],
                            geometry       : new JsonSlurper().parseText(geoJsonWriter.write(rasterEntry.groundGeom)),
                            collection     : rasterEntry?.missionId?.toLowerCase(),
                            properties     : [
                                    datetime            : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.format(rasterEntry?.acquisitionDate) ?: '',
                                    gsd                 : rasterEntry.gsdY ?: 0,
                                    title               : (rasterEntry.imageId && rasterEntry.imageId != "SIDD: Unknown") ? rasterEntry.imageId : rasterEntry.title ?: '',
                                    'eo:bands'          : (1..rasterEntry?.numberOfBands)?.collect { b ->
                                        [
                                                name: String.valueOf(b)
                                        ]
                                    },
                                    'eo:cloud_cover'    : rasterEntry?.cloudCover ?: 0,
                                    'view:azimuth'      : rasterEntry?.azimuthAngle ?: 0,
                                    'view:off_nadir'    : 90 - (rasterEntry?.grazingAngle ?: 90),
                                    'view:sun_azimuth'  : rasterEntry?.sunAzimuth ?: 0,
                                    'view:sun_elevation': rasterEntry?.sunElevation ?: 0,
                            ],
                            stac_version   : '1.0.0',
                            stac_extensions: [
                                    'https://stac-extensions.github.io/eo/v1.0.0/schema.json',
                                    'https://stac-extensions.github.io/view/v1.0.0/schema.json'
                            ]
                    ]
                },
                numberReturned: rasterDataSet?.rasterEntries?.size(),
                timestamp     : Instant.now() as String,
                links         : [],
        ]

        JsonOutput.prettyPrint(JsonOutput.toJson(data))
    }

    def getSatelliteIdAndMissionIdByFilename(String filename) {
        def selectedRaster = RasterEntry.findByFilename(filename)

        if (!selectedRaster) {
            return [missionId: null]
        }
        def isorce = selectedRaster?.isorce
        def missionId = selectedRaster?.missionId

        [missionId: missionId, satelliteId: isorce]
    }

    def generateCatalogId(String filename) {

        def catId
        try {
            def rasterDataSet = RasterFile.where {
                name == filename && type == 'main'
            }.find()?.rasterDataSet
            rasterDataSet?.lock()
            if (!rasterDataSet?.catId) {

                def selectedRaster = RasterEntry.findByFilename(filename)

                if (selectedRaster) {

                    def isorce = selectedRaster?.isorce
                    def missionId = selectedRaster?.missionId
                    if (missionId && isorce) {
                            catId = catalogIdService.generateCatId(missionId, isorce, filename)

                        if (catId) {
                            // Get the rasterDataSet to update the catId
                            rasterDataSet.setCatId(catId)
                                rasterDataSet.save(flush: true)
                        } else {
                            RasterDataSet.initCatId(rasterDataSet)
                        }
                    } else {
                        RasterDataSet.initCatId(rasterDataSet)
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Hit an error generating catalogId. ${e.message}")
            ingestService.writeErrors(filename, "Failed generating catalogId", HttpStatus.INTERNAL_SERVER_ERROR)
        }
        catId
    }
}
