package omar.raster.tags

/*
SQL Update:

insert into country_code_tag(version, name) 
select distinct 0 as version, country_code as name 
from raster_entry 
where country_code is not null 
order by country_code;

update raster_entry set 
    country_code_tag_id=country_code_tag.id 
from country_code_tag 
where raster_entry.country_code=country_code_tag.name;
*/

class CountryCodeTag {
    String name

   static mapping = {
        cache true
        id generator: 'identity'
    }
    
    static constraints = {
        name(unique: true, blank: false)
    }
}