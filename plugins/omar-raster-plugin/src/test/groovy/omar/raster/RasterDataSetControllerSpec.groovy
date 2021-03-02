package omar.raster

import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class RasterDataSetControllerSpec extends Specification implements ControllerUnitTest<RasterDataSetController> {

    void "getRasterFiles null ID returns 422 code"() {
        when:
        GetRasterFilesCommand cmd = new GetRasterFilesCommand()
        controller.getRasterFiles(cmd)

        then:
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }

    void "getRasterFiles empty ID returns 422 code"() {
        when:
        GetRasterFilesCommand cmd = new GetRasterFilesCommand()
        cmd.id = ""
        controller.getRasterFiles(cmd)

        then:
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }

    void "getRasterFiles non empty returns 200 code"(){
        given:
        HashMap result = [ results:[] ]
        controller.rasterDataSetService = Stub(RasterDataSetService) {
            getRasterFiles(_) >> result
        }

        when:
        GetRasterFilesCommand cmd = new GetRasterFilesCommand()
        cmd.id = "1265"
        controller.getRasterFiles(cmd)

        then:
        response.status == HttpStatus.OK.value()
    }

    void "getRasterFilesProcessing invalid offset and limit returns 422"() {
        given:
        HashMap result = [results: []]
        controller.rasterDataSetService = Stub(RasterDataSetService) {
            getFileProcessingStatus(_) >> result
        }
        GetRasterFilesProcessingCommand cmd = new GetRasterFilesProcessingCommand()

        when:
        cmd.offset = -1
        cmd.limit = -1
        controller.getRasterFilesProcessing()

        then:
        HttpStatus.UNPROCESSABLE_ENTITY.value() == response.status
    }

    void "getDistinctValues valid property returns 200"() {
        given:
        HashMap result = [results: []]
        controller.rasterDataSetService = Stub(RasterDataSetService) {
            getDistinctValues(_) >> result
        }
        GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()

        // Test one time for 200 and 422 response
        when:
        response.reset()
        cmd.property = "targetId"
        controller.getDistinctValues(cmd)

        then:
        response.status == HttpStatus.OK.value()
    }

    void "getDistinctValues invalid property returns 422"() {
        when:
        GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()
        response.reset()
        cmd.property = "fakeID"
        controller.getDistinctValues(cmd)

        then:
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }
}