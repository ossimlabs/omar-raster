package omar.raster.tags

import groovy.sql.Sql

class ProductIdTag {
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
        insert into product_id_tag(version, name) 
        select distinct 0 as version, product_id as name 
        from raster_entry 
        where product_id is not null 
        order by product_id;
        """.trim()

        sql.executeUpdate """
        update raster_entry set 
            product_id_tag_id=product_id_tag.id 
        from product_id_tag 
        where raster_entry.product_id=product_id_tag.name;
        """.trim()
    }

}
