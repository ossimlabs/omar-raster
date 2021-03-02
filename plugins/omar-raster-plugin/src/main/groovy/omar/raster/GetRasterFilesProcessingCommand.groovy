package omar.raster

import grails.validation.Validateable
import groovy.transform.ToString

@ToString( includeNames = true )
class GetRasterFilesProcessingCommand implements Validateable
{
   String id
   Integer offset
   Integer limit
   String filter
   static constraints = {
      id required: true
      offset nullable: true, min: 0
      limit nullable: true, min: 0
   }
}
