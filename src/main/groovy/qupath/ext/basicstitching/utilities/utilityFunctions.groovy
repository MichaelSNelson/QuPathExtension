package qupath.ext.basicstitching.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.writers.ome.OMEPyramidWriter
import org.slf4j.Logger
import javax.imageio.ImageIO
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class utilityFunctions {
    static OMEPyramidWriter.CompressionType getCompressionType(String selectedOption) {
        switch (selectedOption) {
            case "Lossy compression":
                return OMEPyramidWriter.CompressionType.J2K_LOSSY
            case "Lossless compression":
                return OMEPyramidWriter.CompressionType.J2K
            default:
                return null // or a default value
        }
    }
    static Map<String, Integer> getTiffDimensions(File filePath) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        if (!filePath.exists()) {
            logger.info("File not found: $filePath")
            return null
        }

        try {
            def image = ImageIO.read(filePath)
            if (image == null) {
                logger.info("ImageIO returned null for file: $filePath")
                return null
            }
            return [width: image.getWidth(), height: image.getHeight()]
        } catch (IOException e) {
            logger.info("Error reading the image file $filePath: ${e.message}")
            return null
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
}
