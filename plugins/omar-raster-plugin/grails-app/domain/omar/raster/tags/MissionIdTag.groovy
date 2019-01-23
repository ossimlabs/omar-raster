package omar.raster.tags

import groovy.sql.Sql

class MissionIdTag {
    String name

    static mapping = {
        cache true
        id generator: 'identity'
    }
    
    static constraints = {
        name(unique: true, blank: false)
    }

    static def backPopulate(Sql sql) {
        sql.executeUpdate """
        insert into mission_id_tag(version, name) 
        select distinct 0 as version, mission_id as name 
        from raster_entry 
        where mission_id is not null 
        order by mission_id;
        """.trim()

        sql.executeUpdate """
        update raster_entry set 
            mission_id_tag_id=mission_id_tag.id 
        from mission_id_tag 
        where raster_entry.mission_id=mission_id_tag.name;
        """.trim()
    }

}