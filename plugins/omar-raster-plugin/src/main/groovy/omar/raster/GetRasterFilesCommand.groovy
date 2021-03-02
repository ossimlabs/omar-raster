package omar.raster

import grails.validation.Validateable
import groovy.transform.ToString

@ToString( includeNames = true )
class GetRasterFilesCommand  implements Validateable
{
   Integer limit
   String id // TODO maybe should be set, constraint should be non-empty string, return first 100? or all? what happens when id is empty?
   // TODO controller should not return 200 if not doing right thing
   static constraints = {
      id required: true
      limit nullable: true, min: 0, max: 100
   }

}
