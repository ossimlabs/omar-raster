package omar.raster

class OmarRasterUrlMappings {

    static mappings = {
        "/dataManager/addRaster"(controller: 'rasterDataSet', action: 'addRaster')
        "/dataManager/removeRaster"(controller: 'rasterDataSet', action: 'removeRaster')
        "/dataManager/getDistinctValues"(controller: 'rasterDataSet', action: 'getDistinctValues')
        "/dataManager/getRasterFilesProcessing"(controller: 'rasterDataSet', action: 'getRasterFilesProcessing')
        "/dataManager/getRasterFiles"(controller: 'rasterDataSet', action: 'getRasterFiles')
        "/dataManager/updateAccessDates"(controller: 'rasterDataSet', action: 'updateAccessDates')
        "/dataManager/hasSICD"(controller: 'rasterDataSet', action: 'hasSICD')
        "/dataManager/writeStacJson"(controller: 'rasterDataSet', action: 'writeStacJson')
    }
}