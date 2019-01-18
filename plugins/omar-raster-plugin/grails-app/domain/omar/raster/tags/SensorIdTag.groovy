package omar.raster.tags

/*
SQL Update:

insert into sensor_id_tag(version, name) 
select distinct 0 as version, sensor_id as name 
from raster_entry 
where sensor_id is not null 
order by sensor_id;

update raster_entry set 
    sensor_id_tag_id=sensor_id_tag.id 
from sensor_id_tag 
where raster_entry.sensor_id=sensor_id_tag.name;
*/
class SensorIdTag {
    String name

   static mapping = {
        cache true
        id generator: 'identity'
    }

    static constraints = {
        name(unique: true, blank: false)
    }
}