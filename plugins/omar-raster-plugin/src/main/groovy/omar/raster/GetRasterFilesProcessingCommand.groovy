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
   static constraints = { // constraints of value if provided will be validated when do cmd.validate()
      // validatable is groovy level?
      // there are min, max, nullable, required: true/false,  etc...
      id required: true
      offset nullable: true, min: 0
      limit nullable: true, min: 0  // if finds negative, will error
      // as many errors as constraints defined
      // cmd.validate() in controller (controller checks for bad data and sends back error instead of sending bad data into service)
      // write unit tests w/o min:0 then add in later
   }
}
