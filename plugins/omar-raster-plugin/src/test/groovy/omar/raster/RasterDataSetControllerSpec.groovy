package omar.raster //TODO not right package, directory structure  is not omar/raster
//TODO RasterDatasetController needs to be fixed
// omar dir under controller
// add dir structure under controller dir, will clear up IDE problem
// omar is a dir, raster is a dir (go to project --> project files to see real path)
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class RasterDataSetControllerSpec extends Specification implements ControllerUnitTest<RasterDataSetController> {
    GetRasterFilesCommand cmd = new GetRasterFilesCommand()
//    def "GetRasterFilesProcessing"() {
//    }


    // TODO : Question - How does this work? Does each ID relate to a specific image or a collection of files?
    void "GetRasterFiles"() {
       when:
       cmd.id = "1265"
       cmd.validate()

       then:
       cmd.error.allError.size() == 0
    }

    void "GetRasterFilesProcessing"(){
        when:
        cmd.id = "1265"
        cmd.offset = 1
        cmd.limit = 1

        then:
        cmd.error.allError.size() == 0
    }

}