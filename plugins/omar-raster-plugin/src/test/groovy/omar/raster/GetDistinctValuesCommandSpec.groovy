package omar.raster

import spock.lang.Specification

class GetDistinctValuesCommandSpec extends Specification {
    GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()
    void "is valid countryCode"() {
        when:
        cmd.rasterKey = "countryCode"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid missionId"() {
        when:
        cmd.rasterKey = "missionId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid sensorId"() {
        when:
        cmd.rasterKey = "sensorId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid productId"() {
        when:
        cmd.rasterKey = "productId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid targetId"() {
        when:
        cmd.rasterKey = "targetId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }

    void "is invalid fakeID"() {
        when:
        cmd.rasterKey = "fakeID"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true
    }

    void "null or empty has error"() {
        when:
        cmd.rasterKey = null
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true

        when:
        cmd.rasterKey = ""
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true
    }
}
