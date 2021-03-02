package omar.raster

import grails.validation.Validateable

class GetDistinctValuesCommand  implements Validateable{
    String property

    static constraints = {
        property "countryCode missionId sensorId targetId productId"
    }

}
