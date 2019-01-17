package omar.raster.tags

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