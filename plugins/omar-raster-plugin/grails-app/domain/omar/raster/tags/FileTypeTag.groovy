package omar.raster.tags

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