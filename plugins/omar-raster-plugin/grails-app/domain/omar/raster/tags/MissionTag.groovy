package omar.raster.tags

class MissionTag {
    String name

    static mapping = {
        cache true
        id generator: 'identity'
    }
    
    static constraints = {
        name(unique: true, blank: false)
    }
}