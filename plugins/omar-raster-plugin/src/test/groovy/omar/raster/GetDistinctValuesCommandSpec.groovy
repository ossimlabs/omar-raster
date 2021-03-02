package omar.raster

import spock.lang.Specification

class GetDistinctValuesCommandSpec extends Specification {
    GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()
    void "is valid countryCode"() {
        when:
        cmd.property = "countryCode"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid missionId"() {
        when:
        cmd.property = "missionId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid sensorId"() {
        when:
        cmd.property = "sensorId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid productId"() {
        when:
        cmd.property = "productId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }
    void "is valid targetId"() {
        when:
        cmd.property = "targetId"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == false
    }

    void "is invalid fakeID"() {
        when:
        cmd.property = "fakeID"
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true
    }

    void "null or empty has error"() {
        when:
        cmd.property = null
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true

        when:
        cmd.property = ""
        cmd.validate()

        then:
        cmd.errors.hasErrors() == true
    }
}
