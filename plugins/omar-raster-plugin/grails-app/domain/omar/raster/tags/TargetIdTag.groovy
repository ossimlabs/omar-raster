package omar.raster.tags

import groovy.sql.Sql

class TargetIdTag {
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
        insert into target_id_tag(version, name) 
        select distinct 0 as version, target_id as name 
        from raster_entry 
        where target_id is not null 
        order by target_id;
        """.trim()

        sql.executeUpdate """
        update raster_entry set 
            target_id_tag_id=target_id_tag.id 
        from target_id_tag 
        where raster_entry.target_id=target_id_tag.name;
        """.trim()
    }
}
