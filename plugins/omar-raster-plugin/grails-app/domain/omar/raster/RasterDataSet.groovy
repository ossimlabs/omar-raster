package omar.raster

import omar.core.Repository
import java.util.UUID

class RasterDataSet
{
    static hasMany = [fileObjects: RasterFile, rasterEntries: RasterEntry]
    Collection fileObjects
    Collection rasterEntries

    Repository repository

    String catId

    static constraints = {
        repository(nullable: true)
        catId(nullable: true, blank: false)
    }

    static mapping = {
        cache true
        id generator: 'identity'
        repository index: 'raster_data_set_repository_idx'
        catId index: 'raster_data_set_cat_id_idx', unique: true
    }

    static RasterDataSet initRasterDataSet(rasterDataSetNode, rasterDataSet = null)
    {
        rasterDataSet = rasterDataSet ?: new RasterDataSet()

        for (def rasterFileNode in rasterDataSetNode.fileObjects.RasterFile)
        {
            RasterFile rasterFile = RasterFile.initRasterFile(rasterFileNode)
            rasterDataSet.addToFileObjects(rasterFile)
        }

        for (def rasterEntryNode in rasterDataSetNode.rasterEntries.RasterEntry)
        {
            RasterEntry rasterEntry = new RasterEntry()
            rasterEntry.rasterDataSet = rasterDataSet
            RasterEntry.initRasterEntry(rasterEntryNode, rasterEntry)

            if (rasterEntry?.groundGeom && (rasterEntry?.entryId?.toInteger() == 0 || !rasterEntry?.imageRepresentation?.equalsIgnoreCase("NODISPLY")))
            {
                rasterDataSet.addToRasterEntries(rasterEntry)
            }
        }

        //initCatId(rasterDataSet)

        return rasterDataSet
    }

    static void initCatId(RasterDataSet rasterDataSet) {
        if ( ! rasterDataSet?.catId ) {
            rasterDataSet?.catId = UUID.randomUUID()
        }
    }

    static void updateCatId(RasterDataSet rasterDataSet, String catId){
        rasterDataSet?.catId = catId
    }

    def getMainFile()
    {
        getFileFromObjects()
    }

    def getFileFromObjects(def type = "main")
    {
        return fileObjects?.find { it.type == type }
    }
}
