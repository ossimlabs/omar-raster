package omar.raster

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import omar.core.Repository
import omar.core.HttpStatus

import omar.stager.OmarStageFile
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


class RasterDataSetService implements ApplicationContextAware {

	static transactional = true

	def parserPool
	def dataInfoService
	def ingestService
	def stagerService
	def ingestMetricsService
	ApplicationContext applicationContext


	def deleteFromRepository( Repository repository ) {
		def rasterDataSets = RasterDataSet.findAllByRepository( repository )

		rasterDataSets?.each { it.delete() }
	}

	/**
	 * This service allows one to add a raster to the omar tables.
	 *
	 * @param httpStatusMessage Is used to populate the http response.  This will
	 *                          identify the status code messages and any additional
	 *                          header paramters that need to be added to the response.
	 * @param filename is the file you wish to add to the OMAR tables
	 */
	def addRaster( def httpStatusMessage, AddRasterCommand params ) {
		String filename = params?.filename
		httpStatusMessage?.status = HttpStatus.OK
		httpStatusMessage?.message = "Added raster ${filename}"
		URI uri = new URI(filename.toString())
		String scheme = uri.scheme?.toLowerCase()
		def raster_logs
		def requestType = "GET"
		def requestMethod = "addRaster"
		Date startTime = new Date()

		def missionids
		def imageids
		def sensorids
		def fileTypes
		def acquisitionDates
		def ingestDates
		def filenames

//		println params.logs


		if(!scheme || (scheme=="file"))
		{
			File testFile = filename as File
			if (!testFile?.exists()) {
				httpStatusMessage?.status = HttpStatus.NOT_FOUND
				httpStatusMessage?.message = "Not Found: ${filename}"
				log.error(httpStatusMessage?.message)
			}
			else if (!testFile?.canRead()) {
				httpStatusMessage?.status = HttpStatus.FORBIDDEN
				httpStatusMessage?.message = "Not Readable ${filename}"
				log.error(httpStatusMessage?.message)
			}
		}


		if(httpStatusMessage?.status == HttpStatus.OK)
		{
			def xml = dataInfoService.getInfo(filename)
			def background = true;
			try { background = params?.background }
			catch (Exception e) { log.error(e) }

			def parser = parserPool?.borrowObject()

         // We will add a return here temporarily but we need to refactor
         // and put the hashmap generator in a method and anything 
         // else we can put in a method that makes sense
         if (!xml) {
            httpStatusMessage?.message = "Unable to get information on file ${filename}"
            httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
            log.error(httpStatusMessage?.message)

            return httpStatusMessage
         }

         def oms = new XmlSlurper(parser)?.parseText(xml)
         def omsInfoParser = applicationContext?.getBean("rasterInfoParser")
         def repository = ingestService?.findRepositoryForFile(filename)
         def rasterDataSets = omsInfoParser?.processDataSets(oms, repository)

			if (background)
			{
				def result = stagerService.addFileToStage(filename, params.properties)

				httpStatusMessage.status = result.status
				httpStatusMessage.message = result.message

				if(rasterDataSets?.size() > 1) {
					rasterDataSets?.each { rasterDataSet ->
						def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
						missionids = rasterDataSet?.rasterEntries.collect { it.missionId }.join(",")
						imageids = rasterDataSet?.rasterEntries.collect { it.imageId }.join(",")
						sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId }.join(",")
						fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType }.join(",")
						acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate }.join(",")
						ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")
						filenames = rasterDataSet?.rasterEntries.collect { it.filename }.join(",")
					}
					raster_logs = new JsonBuilder(timestamp: startTime.format("yyyy-MM-dd hh:mm:ss.ms"), requestType: requestType,
							requestMethod: requestMethod, status: httpStatusMessage?.status, message: httpStatusMessage?.message,
							filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
							ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

					log.info raster_logs.toString()

				}


			}
			else {
				Boolean fileStaged = false
				parserPool?.returnObject(parser)

				if(params.buildOverviews||params.buildHistograms) {
					def result = stagerService.stageFileJni([filename:params.filename,
						  buildOverviews: params.buildOverviews,
						  buildHistograms:params.buildHistograms,
						  overviewCompressionType: params.overviewCompressionType,
						  buildHistogramsWithR0: params.buildHistogramsWithR0,
						  useFastHistogramStaging: params.useFastHistogramStaging,
						  overviewType: params.overviewType
					])
					if(result?.status >= 300) { log.error(result?.message) }
					httpStatusMessage.status = result.status
					httpStatusMessage.message = result.message

					if(rasterDataSets?.size() > 1) {
						rasterDataSets?.each { rasterDataSet ->
							httpStatusMessage?.status = HttpStatus.OK
							def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
							httpStatusMessage?.message = "Added raster ${ids}:${filename}"
							missionids = rasterDataSet?.rasterEntries.collect { it.missionId }.join(",")
							imageids = rasterDataSet?.rasterEntries.collect { it.imageId }.join(",")
							sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId }.join(",")
							fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType }.join(",")
							acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate }.join(",")
							ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")
							filenames = rasterDataSet?.rasterEntries.collect { it.filename }.join(",")
						}
						raster_logs = new JsonBuilder(timestamp: startTime.format("yyyy-MM-dd hh:mm:ss.ms"), requestType: requestType,
								requestMethod: requestMethod, status: httpStatusMessage?.status, message: httpStatusMessage?.message,
								filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
								ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

						log.info raster_logs.toString()

					}
				}
				else {
					if (rasterDataSets?.size() < 1) {
						httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
						httpStatusMessage?.message = "Not a raster file: ${filename}"
						log.error(httpStatusMessage?.message)
					}
					else {
						rasterDataSets?.each { rasterDataSet ->
							def savedRaster = true
							try {
								if (rasterDataSet.save()) {
									//stagerHandler.processSuccessful(filename, xml)
									httpStatusMessage?.status = HttpStatus.OK
									def ids = rasterDataSet?.rasterEntries.collect { it.id }.join(",")
									httpStatusMessage?.message = "Added raster ${ids}:${filename}"
									missionids = rasterDataSet?.rasterEntries.collect { it.missionId }.join(",")
									imageids = rasterDataSet?.rasterEntries.collect { it.imageId }.join(",")
									sensorids = rasterDataSet?.rasterEntries.collect { it.sensorId }.join(",")
									fileTypes = rasterDataSet?.rasterEntries.collect { it.fileType }.join(",")
									acquisitionDates = rasterDataSet?.rasterEntries.collect { it.acquisitionDate }.join(",")
									ingestDates = rasterDataSet?.rasterEntries.collect { it.ingestDate }.join(",")
									filenames = rasterDataSet?.rasterEntries.collect { it.filename }.join(",")

									raster_logs = new JsonBuilder(timestamp: startTime.format("yyyy-MM-dd hh:mm:ss.ms"), requestType: requestType,
											requestMethod: requestMethod, status: httpStatusMessage?.status, message: httpStatusMessage?.message,
											filetypes: fileTypes, filenames: filenames, acquisitionDates: acquisitionDates,
											ingestDates: ingestDates, missionids: missionids, imageids: imageids, sensorids: sensorids)

									log.info raster_logs.toString()
								}
								else {
									savedRaster = false
									httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
									httpStatusMessage?.message = "Unable to save image ${filename}, image probably already exists"
									log.error(httpStatusMessage?.message)
								}
							}
							catch (Exception e) {
								httpStatusMessage?.status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
								httpStatusMessage?.message = "Unable to save image ${filename}, image probably already exists\n${e?.message}"
								log.error(httpStatusMessage?.message)
							}
						}
					}
				}
			}
		}

        // Even if there's no logs, we still want to output the status in the logs.
        if(!params.logs) params.logs = "{}"

		def logsJson = new JsonSlurper().parseText(params.logs)
		addTotalStageTimeToLogs(logsJson)
		addTotalTimeFromAcquisitionToLogs(logsJson)
		// Some images may fail but still need to be logged and differentiated from successes.
		logsJson["ingestStatus"] = httpStatusMessage?.status

		// Print logs in JSON for ElasticSearch and Kibana parsing
		println logsJson.toString()


      httpStatusMessage
	}

	private static String getPrettyStringFromTimeDuration(TimeDuration duration) {
		int hours = duration.hours + (duration.days * 24)
		int minutes = duration.minutes
		int seconds = duration.seconds

		return "$hours:$minutes:$seconds"
	}

	private static String ACQUISITION_DATE_KEY = "acquisitiondate"
	/**
	 * Adds the total time from when the image was acquired to when the ingest pipeline is completed to the JSON logs object.
	 * This method assumes the current time is when the pipeline is completed.
	 * @param logsJson This param must be a JSON log containing the {@code ACQUISITION_DATE_KEY}
	 */
	private static void addTotalTimeFromAcquisitionToLogs(logsJson) {
		// Calculate total time (acq-time to now)
		Date pipelineFinishTime = new Date()

		if(logsJson[ACQUISITION_DATE_KEY]) {
			Date imageAcquiredTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", logsJson[ACQUISITION_DATE_KEY])
			TimeDuration totalTimeFromAcquisition = TimeCategory.minus(pipelineFinishTime, imageAcquiredTime)
			logsJson["totalTimeFromAcquisition"] = getPrettyStringFromTimeDuration(totalTimeFromAcquisition)
		}
	}

	private static String PIPELINE_START_DATE_KEY = "ingestdate_sqs"
	/**
	 * Adds the total time in the ingest pipeline apps (omar-sqs/avro/stager) to the JSON logs object.
	 * This method assumes the current time is when the pipeline is completed.
	 * @param logsJson This param must be a JSON log containing the {@code PIPELINE_START_DATE_KEY}
	 */
	private static void addTotalStageTimeToLogs(logsJson) {
		// Calculate stage time (total time in all three apps)
		Date pipelineFinishTime = new Date()

		if(logsJson[PIPELINE_START_DATE_KEY]) {
			Date pipelineStartTime = new Date().parse("yyyy-MM-dd HH:mm:ss.ms", logsJson[PIPELINE_START_DATE_KEY])
			TimeDuration totalStagingTime = TimeCategory.minus(pipelineFinishTime, pipelineStartTime)
		    logsJson["stagingTime"] = getPrettyStringFromTimeDuration(totalStagingTime)
		}
	}

	def removeRaster( def httpStatusMessage, def params ) {
		def status = false
		String filename = params?.filename //as File

		def rasterFile = RasterFile.findByNameAndType( filename, "main" )
		if ( rasterFile ) {
			rasterFile?.rasterDataSet?.delete( flush: true )
			httpStatusMessage?.status = HttpStatus.OK
			def ids = rasterFile?.rasterDataSet?.rasterEntries?.collect { it?.id }?.join( "," )
			httpStatusMessage?.message = "removed raster ${ ids }:${ filename }"
			
			if (params.deleteFiles?.toBoolean()) {
				def files = []
				rasterFile?.rasterDataSet?.fileObjects.each() { files << it.name }
				rasterFile?.rasterDataSet?.rasterEntries.each() {
                			it.fileObjects.each() { files << it.name }
				}

				files.each() {
					def file = it
					URI uri = new URI(file)
					String scheme = uri.scheme?.toLowerCase()
					if(!scheme||(scheme=="file"))
					{
						File fileToRemove = file as File
						if (fileToRemove.canWrite()) {
							if (fileToRemove.isDirectory())
							{
								fileToRemove.deleteDir() 
							}
							else 
							{ 
								fileToRemove.delete() 
							}
							if (!fileToRemove.exists()) 
							{ 
//								log.info("Deleted ${file}")
							}
							else 
							{ 
//								log.info("Unable to delete ${file}")
							}
						}
					}
					else 
					{ 
//						log.info("Don't have permissions to delete ${file}")
					}
				}
			}
		}
		else {
			httpStatusMessage?.status = HttpStatus.NOT_FOUND
			httpStatusMessage?.message = "Raster file does not exist in the database: ${ filename }"
			log.error( httpStatusMessage?.message )
		}
	}


	def deleteRaster( def httpStatusMessage, def params )
	{
		removeRaster( httpStatusMessage, params )
	}


	def getFileProcessingStatus(GetRasterFilesProcessingCommand cmd)
	{
		HashMap result = [
				results:[],
				pagination: [
						count: 0,
						offset: 0,
						limit: 0
				]
		]

		try
		{
			result.pagination.count = OmarStageFile.count()
			result.pagination.offset = cmd.offset?:0
			Integer limit = cmd.limit?:result.pagination.count
			def files = OmarStageFile.list([offset:result.pagination.offset, max:limit])
			files?.each{record->
				result.results <<
					[
						filename:record.filename,
						processId: record.processId,
						status: record.status.name,
						statusMessage: record.statusMessage,
						buildOverviews: record.buildOverviews,
						buildHistograms: record.buildHistograms,
						buildHistogramsWithR0: record.buildHistogramsWithR0,
						useFastHistogramStaging: record.useFastHistogramStaging,
						overviewCompressionType: record.overviewCompressionType,
						overviewType: record.overviewType,
						dateCreated: record.dateCreated,
					]
			}

			result.pagination.limit = limit
		}
		catch(e) {
			result.status = HttpStatus.BAD_REQUEST
			result.message = e.toString()
			result.remove("results")
			result.remove("pagination")
		}

		result
	}

	def getRasterFiles(GetRasterFilesCommand cmd) {
		HashMap result = [ results:[] ]

		def files = RasterEntry.compositeId(cmd.id)

		RasterEntry entry = files?.get()
		def fileList = []
		if(entry) {
			entry.fileObjects.each{fileObject->
				fileList << fileObject.name
			}
			entry?.rasterDataSet?.fileObjects.each{ fileObject->
				fileList << fileObject.name
			}
		}
		result.results = fileList

		result
	}
}
