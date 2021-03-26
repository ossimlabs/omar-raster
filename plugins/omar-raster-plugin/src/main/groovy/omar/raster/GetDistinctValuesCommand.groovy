package omar.raster

import grails.validation.Validateable
import groovy.transform.ToString

@ToString( includeNames = true )
class GetDistinctValuesCommand  implements Validateable{
    String property

    static constraints = {
        property blank: false, validator: { val, obj,errors ->
                if (!RasterEntryDistinctValues.findByValue(val)) {
                    errors.rejectValue('property', 'invalid property')
                }}
    }
}
