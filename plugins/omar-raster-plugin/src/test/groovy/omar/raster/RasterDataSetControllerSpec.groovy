package omar.raster

import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class RasterDataSetControllerSpec extends Specification implements ControllerUnitTest<RasterDataSetController> {

    void setup() {
        HashMap result = [ results:[] ]
        controller.rasterDataSetService = Stub(RasterDataSetService) {
            getFileProcessingStatus(_) >> result
            getDistinctValues(_) >> result
            getRasterFiles(_) >> result
        }
    }

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
        when:
        GetRasterFilesCommand cmd = new GetRasterFilesCommand()
        cmd.id = "1265"
        controller.getRasterFiles(cmd)

        then:
        response.status == HttpStatus.OK.value()
    }

    void "getRasterFilesProcessing invalid offset and limit returns 422"() {
        given:
        GetRasterFilesProcessingCommand cmd = new GetRasterFilesProcessingCommand()

        when:
        cmd.offset = -1
        cmd.limit = -1
        controller.getRasterFilesProcessing()

        then:
        HttpStatus.UNPROCESSABLE_ENTITY.value() == response.status
    }

    void "getDistinctValues valid property returns 200"() {
        when:
        GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()
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