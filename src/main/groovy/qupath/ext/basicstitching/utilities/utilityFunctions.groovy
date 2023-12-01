package qupath.ext.basicstitching.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.images.writers.ome.OMEPyramidWriter
import javax.imageio.ImageIO



/**
 * Class containing utility functions used throughout the application.
 */
class utilityFunctions {

    /**
     * Gets the compression type for OMEPyramidWriter based on the selected option.
     *
     * @param selectedOption The selected compression option as a string.
     * @return The corresponding OMEPyramidWriter.CompressionType.
     */
    static OMEPyramidWriter.CompressionType getCompressionType(String selectedOption) {
        switch (selectedOption) {
            case "Lossy compression":
                return OMEPyramidWriter.CompressionType.J2K_LOSSY
            case "Lossless compression":
                return OMEPyramidWriter.CompressionType.J2K
            default:
                // Consider providing a default compression type or handling this case
                return null
        }
    }

    /**
     * Retrieves the dimensions (width and height) of a TIFF image file.
     *
     * @param filePath The file path of the TIFF image.
     * @return A map containing the 'width' and 'height' of the image, or null if an error occurs.
     */
    static Map<String, Integer> getTiffDimensions(File filePath) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)

        // Check if the file exists
        if (!filePath.exists()) {
            logger.info("File not found: $filePath")
            return null
        }

        try {
            // Read the image file
            def image = ImageIO.read(filePath)
            if (image == null) {
                logger.info("ImageIO returned null for file: $filePath")
                return null
            }

            // Return the image dimensions as a map
            return [width: image.getWidth(), height: image.getHeight()]
        } catch (IOException e) {
            // Log and handle the error
            logger.info("Error reading the image file $filePath: ${e.message}")
            return null
        }
    }
}

    //TODO Move this somewhere
//    List<Map> prepareStitching(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString) {
//        def logger = LoggerFactory.getLogger(QuPathGUI.class)
//        Path rootdir = Paths.get(folderPath)
//        List<Map> allFileRegionMaps = [] // This will store the file-region maps for all subdirectories
//        def subdir = []
//
//        Files.newDirectoryStream(rootdir).each { path ->
//            if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
//                logger.info("Processing: $path")
//                def fileRegionMaps = processSubDirectory(path, pixelSizeInMicrons, baseDownsample)
//                allFileRegionMaps += fileRegionMaps
//            }
//        }
//
//        if (allFileRegionMaps.isEmpty()) {
//            Dialogs.showWarningNotification("Warning", "No valid tile configurations found in any subdirectory.")
//            return
//        }
//
//        allFileRegionMaps
//    }
//}
