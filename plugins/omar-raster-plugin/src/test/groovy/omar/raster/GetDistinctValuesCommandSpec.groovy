package omar.raster

import spock.lang.Specification

class GetDistinctValuesCommandSpec extends Specification {
    GetDistinctValuesCommand cmd = new GetDistinctValuesCommand()
    void "GetDistinctValuesCommand"() {
        when:
        cmd.property = "missionId"
        cmd.validate()

        then:
        cmd.error.allError.size() == 0
    }
}
