package omar.raster

import grails.validation.Validateable
import groovy.transform.ToString

@ToString( includeNames = true )
class GetDistinctValuesCommand  implements Validateable{
    String property

    static constraints = {
        property blank: false, matches: "countryCode|missionId|sensorId|targetId|productId"
    }
}
