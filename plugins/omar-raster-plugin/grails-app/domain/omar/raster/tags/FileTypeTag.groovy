package omar.raster.tags

/*
SQL Update:

insert into file_type_tag(version, name) 
select distinct 0 as version, file_type as name 
from raster_entry 
where file_type is not null 
order by file_type;

update raster_entry set 
    file_type_tag_id=file_type_tag.id 
from file_type_tag 
where raster_entry.file_type=file_type_tag.name;
*/
class FileTypeTag {
    String name

   static mapping = {
        cache true
        id generator: 'identity'
    }

    static constraints = {
        name(unique: true, blank: false)
    }
}