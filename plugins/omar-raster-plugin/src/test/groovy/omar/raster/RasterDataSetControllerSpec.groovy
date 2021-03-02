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

    void "getDistinctValues test"() {
        given:
        HashMap result = [results: []]
        controller.rasterDataSetService = Stub(RasterDataSetService) {
            getDistinctValues(_) >> result
        }
        GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()

//        when:
//        cmd.value = "countryCode"
//        controller.getDistinctValues(cmd)
//
//        then:
//        HttpStatus.OK.value() == response.status
//
//        when:
//        cmd.value = "missionId"
//        controller.getDistinctValues(cmd)
//
//        then:
//        response.status == HttpStatus.OK.value()
//
//        when:
//        cmd.value = "sensorId"
//        controller.getDistinctValues(cmd)
//
//        then:
//        response.status == HttpStatus.OK.value()
//
//        when:
//        cmd.value = "productId"
//        controller.getDistinctValues(cmd)
//
//        then:
//        response.status == HttpStatus.OK.value()
//
//        when:
//        cmd.value = "targetId"
//        controller.getDistinctValues(cmd)
//
//        then:
//        response.status == HttpStatus.OK.value()

        when:
        cmd.rasterKey = "fakeID"
        controller.getDistinctValues(cmd)

        then:
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }
}