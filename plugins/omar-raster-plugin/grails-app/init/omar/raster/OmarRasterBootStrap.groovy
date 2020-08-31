package omar.raster 

import omar.raster.tags.CountryCodeTag
import omar.raster.tags.FileTypeTag
import omar.raster.tags.MissionIdTag
import omar.raster.tags.ProductIdTag
import omar.raster.tags.SensorIdTag
import omar.raster.tags.TargetIdTag
 
import groovy.sql.Sql

class OmarRasterBootStrap {
    def dataSource
    
    def init = { servletContext ->    
      if ( RasterEntry.count() > 0 ) {
        Sql sql = new Sql(dataSource)
        
        sql?.executeUpdate "create index if not exists raster_entry_ground_geom_idx on raster_entry using gist ( ground_geom )"

        if ( CountryCodeTag.count() == 0 )  {
          CountryCodeTag.backPopulate sql
        }  
        if ( FileTypeTag.count() == 0 )  {
          FileTypeTag.backPopulate sql
        }  
        if ( MissionIdTag.count() == 0 )  {
          MissionIdTag.backPopulate sql
        }  
        if ( ProductIdTag.count() == 0 )  {
          ProductIdTag.backPopulate sql
        }  
        if ( SensorIdTag.count() == 0 )  {
          SensorIdTag.backPopulate sql
        }  
        if ( TargetIdTag.count() == 0 )  {
          TargetIdTag.backPopulate sql
        }
        sql?.close()  
      }
    }
    def destroy = {
    }
}
