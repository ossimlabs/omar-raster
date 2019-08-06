package omar.raster

import io.swagger.annotations.*

import grails.converters.JSON
import omar.core.HttpStatusMessage
import omar.core.BindUtil
import groovy.json.JsonOutput
import groovy.xml.XmlUtil
import groovy.util.XmlSlurper
import groovy.json.JsonSlurper
@Api( value = "dataManager",
     produces = 'application/json',
     consumes = 'application/json',
		description = "DataManager Support"
)
class RasterDataSetController
{
	static allowedMethods = [
			addRaster: 'POST',
			removeRaster: 'POST' ,
			getRasterFilesProcessing: 'GET'
	]

	def rasterDataSetService
	@ApiOperation( value = "Add a Raster to the database",
			httpMethod = "POST",
			notes = """
The service api **addRaster**

## Parameter List

*   **filename**

    Pass the filename to be added and indexed into the database

*   **background**

    If the parameter is true it will indicate to submit the process as a background job.

*   **buildThumbnails**

    If the parameter is true it will indicate to build thumbnails for the image.

*   **buildOverviews**

    If the parameter is true it will indicate to build overviews for the image. Also known as reduced resolution sets.

*   **buildHistograms**

    If the parameter is true it will indicate to build histograms for the image.

*   **buildHistogramsWithR0**

    If the parameter is true and buildHistograms is true then it will use the full resolution to build the histograms. Ususally this is specified if you have images with internal overviews. If internal overviews are found and buildHistograms is true and buildHistogramsWithR0 is false then it will build using the first iteration of the overview and that is whatever the last level the internal overviews end. So if you have an image with internal 5 levels then it will use the 5th level for the histogram. If

*   **useFastHistogramStaging**

    If enabled it will only use a few tiles in the image to calculate the histogram

*   **overviewType**

    Indicates the overview type to use.

*   **overviewCompressionType**

    Indicates the type of compression to use during the building of the overviews.

*   **thumbnailType**

    Indicates the type of thumbnails to create.  This value can be **jpeg** or **png**.

*   **thumbnailStretchType**

    Indicates the type of stretch to use for thumbnail generation  This can be
	 **none**, **auto-minmax**, **auto-percentile**, **std-stretch-1**, **std-stretch-2**, **std-stretch-3**.

**Additional Notes**

You can also pass the arguments as a JSON string and post
to the URL.  The format supported:

```
{
   "filename": "",
   "background": "",
	"buildThumbnails": "",
   "buildOverviews": "",
   "buildHistograms": "",
   "overviewType": "",
   "overviewCompressionType":"",
	"thumbnailSize":"",
	"thumbnailType":"",
	"thumbnailStretchType":""

}
```
            """)
	@ApiImplicitParams( [
			@ApiImplicitParam( name = 'filename', value = 'Path to file to add', dataType = 'string', paramType = "query", required = true ),
			@ApiImplicitParam( name = 'background', value = 'Process in the background', allowableValues="true,false", defaultValue="true", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'buildThumbnails', value = 'Build thumbnails', allowableValues="true,false", defaultValue="true", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'buildOverviews', value = 'Build overviews', allowableValues="true,false", defaultValue="true", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'buildHistograms', value = 'Build histograms', allowableValues="true,false", defaultValue="true", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'buildHistogramsWithR0', value = 'Build histograms with R0', allowableValues="true,false", defaultValue="false", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'useFastHistogramStaging', value = 'Fast Histogram calculation', allowableValues="true,false", defaultValue="false", dataType = "boolean", paramType = "query", required = false),
			@ApiImplicitParam( name = 'overviewType', value = 'Overview type', allowableValues="ossim_tiff_box, ossim_tiff_nearest, ossim_kakadu_nitf_j2k", defaultValue = "ossim_tiff_box", dataType = "string", paramType = "query", required = false),
			@ApiImplicitParam( name = 'overviewCompressionType', value = 'Overview compression type', allowableValues="NONE,JPEG,PACKBITS,DEFLATE", defaultValue="NONE", dataType = "string", paramType = "query", required = false),
			@ApiImplicitParam( name = 'thumbnailSize', value = 'Thumbnail size', allowableValues="512,256,128,64,32", defaultValue="512", dataType = "integer", paramType = "query", required = false),
			@ApiImplicitParam( name = 'thumbnailType', value = 'Thumbnail type', allowableValues="jpeg,png", defaultValue="jpeg", dataType = "string", paramType = "query", required = false),
			@ApiImplicitParam( name = 'thumbnailStretchType', value = 'Thumbnail stretch type', allowableValues="none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3", defaultValue="auto-minmax", dataType = "string", paramType = "query", required = false),
	] )
	def addRaster()
	{
		String body = request.reader.text
		def xmlString

		println("Entering addRaster()....body: $body")

		if(body)
		{
			def xmlCheck
			try{
				xmlCheck = new XmlSlurper().parseText(body)
				//xml = request.XML
			}
			catch(e)
			{
				xmlCheck = null
			}
			if(xmlCheck)
			{
				xmlString = body
			}
		}
		if(!xmlString)
		{
			def json
			try{
				json = body?new JsonSlurper().parseText(body):null
			}
			catch(e)
			{
				json = null
			}
			def jsonData = json?json as HashMap : null
			def requestParams = params - params.subMap( ['controller', 'action'] )
			def cmd = new AddRasterCommand()

			// get map from JSON and merge into parameters
			if(jsonData) requestParams << jsonData
			BindUtil.fixParamNames( AddRasterCommand, requestParams )
			bindData( cmd, requestParams )

			def httpStatusMessage = new HttpStatusMessage()
			def status = rasterDataSetService.addRaster( httpStatusMessage, cmd )

			response.status = httpStatusMessage.status
			render( httpStatusMessage.message )
		}
		else
		{
			def result = rasterDataSetService.addRasterXml(xmlString)
   		response.status = result.status
			render( result.message )
		}
	}

	@ApiOperation( value = "Remove a Raster from the database",
		            produces = 'text/plain',
		            httpMethod = 'POST' )
	@ApiImplicitParams([
			@ApiImplicitParam( name = 'deleteFiles',
				                value = 'Delete the image file and all support files linked to it in the database (e.g. his, ovr, etc.)',
				                allowableValues="true,false",
				                defaultValue="false",
				                dataType = "boolean",
				                paramType = 'query',
				                required = false),
			@ApiImplicitParam( name = 'filename', value = 'Path to file to remove', dataType = 'string', paramType = 'query',required = true ),
	])
	def removeRaster()
	{
//		log.info "removeRaster: ${params}"

		def httpStatusMessage = new HttpStatusMessage()
		def status = rasterDataSetService.removeRaster( httpStatusMessage, params )

		response.status = httpStatusMessage.status
		render( httpStatusMessage.message )
	}

	@ApiOperation( value = "Returns the processing status of the raster files",
			         produces = 'application/json',
			         httpMethod = 'GET' )
	@ApiImplicitParams( [
			@ApiImplicitParam(name = 'offset', value = 'Process Id', required=false, paramType = 'query', dataType = 'integer'),
			@ApiImplicitParam(name = 'limit', value = 'Process status', defaultValue = '', paramType = 'query', dataType = 'integer'),
	 ] )
	def getRasterFilesProcessing()
	{
//		log.info "getRasterFilesProcessing: ${params}"

		def jsonData = request.JSON?request.JSON as HashMap:null
		def requestParams = params - params.subMap( ['controller', 'action'] )
		def cmd = new GetRasterFilesProcessingCommand()

		// get map from JSON and merge into parameters
		if(jsonData) requestParams << jsonData
		BindUtil.fixParamNames( GetRasterFilesProcessingCommand, requestParams )
		bindData( cmd, requestParams )
		HashMap result = rasterDataSetService.getFileProcessingStatus(cmd)

		render contentType: "application/json", text: result as JSON
	}

	@ApiOperation( value = "Returns the Files assoicated with a given raster ID",
			produces = 'application/json',
			httpMethod = 'GET',
			notes = """
The service api **getRasterFiles**

## Parameter List

*   **id**

    This can be the record ID, image ID, or the indexId for a entry to search for
    """)
	@ApiImplicitParams( [
			@ApiImplicitParam(name = 'id',
				               value = 'Search Id',
				               required=false,
				               paramType = 'query',
				               dataType = 'string'),
	] )
	def getRasterFiles()
	{
//		log.info "getRasterFiles: ${params}"

		def jsonData = request.JSON?request.JSON as HashMap:null
		def requestParams = params - params.subMap( ['controller', 'action'] )
		def cmd = new GetRasterFilesCommand()

		// get map from JSON and merge into parameters
		if(jsonData) requestParams << jsonData
		BindUtil.fixParamNames( GetRasterFilesCommand, requestParams )
		bindData( cmd, requestParams )
		HashMap result = rasterDataSetService.getRasterFiles(cmd)

		render contentType: "application/json", text: result as JSON
	}

	@ApiOperation(
		value = "Returns an array of distinct values in the Raster Entry table for a given column name",
		produces = 'application/json',
		httpMethod = 'GET'
	)
	@ApiImplicitParams([
		@ApiImplicitParam(
			allowableValues = "countryCode, missionId, sensorId, targetId, productId",
 			dataType = 'string',
			defaultValue = "countryCode",
			name = 'property',
			paramType = 'query',
			required = true,
			value = 'Column Name'
		)
	])
	def getDistinctValues() {
		log.info "getDistinctValues: ${params}"
        def results = rasterDataSetService.getDistinctValues(params)
		render contentType: "application/json", text: JsonOutput.toJson(results.findAll({ it != null }))
	}

	@ApiOperation(
			value = "Updates each last access date of the given rastersEntry IDs if their access date is older than 24 hours",
			produces = 'text/plain',
			httpMethod = 'GET'
	)
	@ApiImplicitParams([
			@ApiImplicitParam( name = 'rasterEntries', value = 'CSV of rasterEntry IDs', dataType = 'string', paramType = "query", required = true )
	])
	def updateAccessDates() {
        List<String> rasterEntries = params.rasterEntries.split(",")
		List<String> updatedRasters = rasterDataSetService.updateAccessDates(rasterEntries)

        render contentType: "application/json", text: "${updatedRasters.join(",")}"
	}
}
