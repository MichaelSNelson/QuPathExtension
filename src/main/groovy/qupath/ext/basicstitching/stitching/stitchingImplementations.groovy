package qupath.ext.basicstitching.stitching

import qupath.lib.common.GeneralTools
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.images.servers.ImageServers
import qupath.lib.images.servers.SparseImageServer
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.regions.ImageRegion
import qupath.lib.gui.QuPathGUI

import java.awt.image.BufferedImage
import static qupath.lib.gui.scripting.QPEx.*
import qupath.ext.basicstitching.utilities.utilityFunctions
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.plugins.tiff.BaselineTIFFTagSet
import javax.imageio.plugins.tiff.TIFFDirectory
import javax.imageio.ImageIO

import org.slf4j.LoggerFactory;

// Interface for stitching strategies
interface StitchingStrategy {
    List<Map> prepareStitching(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString)
}

//TODO Look into reducing repetitive code here, though maybe the overall structure is wrong?
//abstract class BaseStitchingStrategy implements StitchingStrategy {
//    protected List<Map> prepareStitchingCommon(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString, Closure<Map> processSubDirectory) {
//        def logger = LoggerFactory.getLogger(QuPathGUI.class)
//        Path rootdir = Paths.get(folderPath)
//        List<Map> allFileRegionMaps = []
//
//        Files.newDirectoryStream(rootdir).each { path ->
//            if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
//                def fileRegionMaps = processSubDirectory.call(path)
//                allFileRegionMaps += fileRegionMaps
//            }
//        }
//
//        if (allFileRegionMaps.isEmpty()) {
//            Dialogs.showWarningNotification("Warning", "No valid tile configurations found in any subdirectory.")
//            return []
//        }
//
//        allFileRegionMaps
//    }
//}
// Concrete strategy for stitching based on file names
class FileNameStitchingStrategy implements StitchingStrategy {
    @Override
    List<Map> prepareStitching(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString) {
        Path rootdir = Paths.get(folderPath)
        List<Map> allFileRegionMaps = [] // This will store the file-region maps for all subdirectories


        Files.newDirectoryStream(rootdir).each { path ->
            if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
                def fileRegionMaps = processSubDirectory(path, pixelSizeInMicrons, baseDownsample)
                allFileRegionMaps += fileRegionMaps
            }
        }

        if (allFileRegionMaps.isEmpty()) {
            Dialogs.showWarningNotification("Warning", "No valid tile configurations found in any subdirectory.")
            return
        }

        allFileRegionMaps
    }
    private static List<Map> processSubDirectory(Path dir, double pixelSizeInMicrons, double baseDownsample) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        logger.info("Processing slide in folder $dir")

        def files = []
        Files.newDirectoryStream(dir, "*.tif*").each { path ->
            files << path.toFile()
        }

        def tileConfigOutput = buildTileConfigWithMinCoordinates(dir)
        def tileConfig = tileConfigOutput[0]
        def minimumXY = tileConfigOutput[1]
        List<Map> fileRegionMaps = []
        files.each { file ->
            def region = parseRegionFromOffsetTileConfig(file as File, tileConfig as List<Map>, minimumXY, pixelSizeInMicrons)
            if (region) {
                fileRegionMaps << [file: file, region: region]
            }
        }

        return fileRegionMaps
    }
    static ImageRegion parseRegionFromOffsetTileConfig(File file, List<Map> tileConfig, minimumXY, double pixelSizeInMicrons, int z = 0, int t = 0) {
        String imageName = file.getName()
        def config = tileConfig.find { it.imageName == imageName }
        def logger = LoggerFactory.getLogger(QuPathGUI.class)

        if (config) {
            int x = (config.x - minimumXY[0]) / pixelSizeInMicrons as int
            int y = (config.y - minimumXY[1]) / pixelSizeInMicrons as int
            def dimensions = utilityFunctions.getTiffDimensions(file)
            if (dimensions == null) {
                logger.info("Could not retrieve dimensions for image $imageName")
                return null
            }
            int width = dimensions.width
            int height = dimensions.height
            return ImageRegion.createInstance(x, y, width, height, z, t)
        } else {
            logger.info("No configuration found for image $imageName")
            return null
        }
    }
    static def buildTileConfigWithMinCoordinates(Path dir) {
        def images = []
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        Files.newDirectoryStream(dir, "*.{tif,tiff,ome.tif}").each { path ->
            def matcher = path.fileName.toString() =~ /.*\[(\d+),(\d+)\].*\.(tif|tiff|ome.tif)$/
            if (matcher.matches()) {
                def imageName = path.getFileName().toString()
                int x = Integer.parseInt(matcher[0][1])
                int y = Integer.parseInt(matcher[0][2])
                images << ['imageName': imageName, 'x': x, 'y': y]
            }
        }

        def minX = images.min { it.x }?.x ?: 0
        def minY = images.min { it.y }?.y ?: 0
        return [images, [minX, minY]]
    }
}


class TileConfigurationTxtStrategy implements StitchingStrategy {
    @Override
    List<Map> prepareStitching(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString) {

        Path rootdir = Paths.get(folderPath)
        List<Map> allFileRegionMaps = [] // This will store the file-region maps for all subdirectories


        Files.newDirectoryStream(rootdir).each { path ->
            if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
                def fileRegionMaps = processSubDirectory(path, pixelSizeInMicrons, baseDownsample)
                allFileRegionMaps += fileRegionMaps
            }
        }

        if (allFileRegionMaps.isEmpty()) {
            Dialogs.showWarningNotification("Warning", "No valid tile configurations found in any subdirectory.")
            return
        }

        allFileRegionMaps
    }
    private static List<Map> processSubDirectory(Path dir, double pixelSizeInMicrons, double baseDownsample) {

        logger.info("Processing slide in folder $dir")
        // Check for TileConfiguration.txt
        Path tileConfigPath = dir.resolve("TileConfiguration.txt")
        if (!Files.exists(tileConfigPath)) {
            logger.info("Skipping folder as TileConfiguration.txt is missing: $dir")
            return
        }
        def tileConfig = parseTileConfiguration(tileConfigPath.toString())
        logger.info('completed parseTileConfiguration')
        List<File> files = []
        Files.newDirectoryStream(dir, "*.tif*").each { path ->
            files << path.toFile()
        }

        List<Map> fileRegionMaps = []
        files.each { File file ->
            logger.info("parsing region from file $file")
            ImageRegion region = parseRegionFromTileConfig(file as File, tileConfig as List<Map>)
            if (region) {
                logger.info("Processing file: ${file.path}")
                fileRegionMaps << [file: file, region: region]
            }
        }
        // Perform stitching for this subdir here, or return fileRegionMaps to be stitched later
        return fileRegionMaps
    }

    /**
     * Parses the 'TileConfiguration.txt' file to extract image names and their coordinates.
     * The function reads each line of the file, ignoring comments and blank lines.
     * It extracts the image name and coordinates, then stores them in a list.
     *
     * @param filePath The path to the 'TileConfiguration.txt' file.
     * @return A list of maps, each containing the image name and its coordinates (x, y).
     */
    static def parseTileConfiguration(String filePath) {
        def lines = Files.readAllLines(Paths.get(filePath))
        def images = []

        lines.each { line ->
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                def parts = line.split(";")
                if (parts.length >= 3) {
                    def imageName = parts[0].trim()
                    def coordinates = parts[2].trim().replaceAll("[()]", "").split(",")
                    images << [imageName: imageName, x: Double.parseDouble(coordinates[0]), y: Double.parseDouble(coordinates[1])]
                }
            }
        }

        return images
    }
    /**
     * Parse an ImageRegion from the TileConfiguration.txt data and TIFF file dimensions.
     * @param imageName Name of the image file for which to get the region.
     * @param tileConfig List of tile configurations parsed from TileConfiguration.txt.
     * @param z index of z plane.
     * @param t index of timepoint.
     * @return An ImageRegion object representing the specified region of the image.
     */
    static ImageRegion parseRegionFromTileConfig(File file, List<Map> tileConfig, int z = 0, int t = 0) {
        String imageName = file.getName()
        def config = tileConfig.find { it.imageName == imageName }

        if (config) {
            int x = config.x as int
            int y = config.y as int
            def dimensions = utilityFunctions.getTiffDimensions(file)
            if (dimensions == null) {
                logger.info(  "Could not retrieve dimensions for image $imageName")
                return null
            }
            int width = dimensions.width
            int height = dimensions.height
            //logger.info( x+" "+y+" "+ width+ " " + height)
            return ImageRegion.createInstance(x, y, width, height, z, t)
        } else {
            logger.info(  "No configuration found for image $imageName")
            return null
        }
    }
}


class VectraMetadataStrategy implements StitchingStrategy {
    @Override
    List<Map> prepareStitching(String folderPath, double pixelSizeInMicrons, double baseDownsample, String matchingString) {
        Path rootdir = Paths.get(folderPath)
        List<Map> allFileRegionMaps = [] // This will store the file-region maps for all subdirectories


        Files.newDirectoryStream(rootdir).each { path ->
            if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
                def fileRegionMaps = processSubDirectory(path, pixelSizeInMicrons, baseDownsample)
                allFileRegionMaps += fileRegionMaps
            }
        }

        if (allFileRegionMaps.isEmpty()) {
            Dialogs.showWarningNotification("Warning", "No valid tile configurations found in any subdirectory.")
            return
        }

        allFileRegionMaps
    }

    private static List<Map> processSubDirectory(Path dir, double pixelSizeInMicrons, double baseDownsample) {
        logger.info("Processing slide in folder $dir")
        // Check for TileConfiguration.txt


        List<File> files = []
        Files.newDirectoryStream(dir, "*.tif*").each { path ->
            files << path.toFile()
        }
        logger.info('Parsing regions from ' + files.size() + ' files...')
        List<Map> fileRegionMaps = []
        files.each { file ->
            ImageRegion region = parseRegion(file as File)
            if (region) {
                //logger.info("Processing file: ${file.path}")
                fileRegionMaps << [file: file, region: region]
            }
        }

        return fileRegionMaps
    }

    static ImageRegion parseRegion(File file, int z = 0, int t = 0) {
        if (checkTIFF(file)) {
            try {
                return parseRegionFromTIFF(file, z, t)
            } catch (Exception e) {
                print e.getLocalizedMessage()
            }
        }
    }

    /**
     * Check for TIFF 'magic number'.
     * @param file
     * @return
     */
    static boolean checkTIFF(File file) {
        file.withInputStream {
            def bytes = it.readNBytes(4)
            short byteOrder = toShort(bytes[0], bytes[1])
            int val
            if (byteOrder == 0x4949) {
                // Little-endian
                val = toShort(bytes[3], bytes[2])
            } else if (byteOrder == 0x4d4d) {
                val = toShort(bytes[2], bytes[3])
            } else
                return false
            return val == 42 || val == 43
        }
    }

    /**
     * Combine two bytes to create a short, in the given order
     * @param b1
     * @param b2
     * @return
     */
    static short toShort(byte b1, byte b2) {
        return (b1 << 8) + (b2 << 0)
    }

    /**
     * Parse an ImageRegion from a TIFF image, using the metadata.
     * @param file image file
     * @param z index of z plane
     * @param t index of timepoint
     * @return
     */
    static ImageRegion parseRegionFromTIFF(File file, int z = 0, int t = 0) {
        int x, y, width, height
        file.withInputStream {
            def reader = ImageIO.getImageReadersByFormatName("TIFF").next()
            reader.setInput(ImageIO.createImageInputStream(it))
            def metadata = reader.getImageMetadata(0)
            def tiffDir = TIFFDirectory.createFromMetadata(metadata)

            double xRes = getRational(tiffDir, BaselineTIFFTagSet.TAG_X_RESOLUTION)
            double yRes = getRational(tiffDir, BaselineTIFFTagSet.TAG_Y_RESOLUTION)

            double xPos = getRational(tiffDir, BaselineTIFFTagSet.TAG_X_POSITION)
            double yPos = getRational(tiffDir, BaselineTIFFTagSet.TAG_Y_POSITION)

            width = tiffDir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH).getAsLong(0) as int
            height = tiffDir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH).getAsLong(0) as int

            x = Math.round(xRes * xPos) as int
            y = Math.round(yRes * yPos) as int
        }
        return ImageRegion.createInstance(x, y, width, height, z, t)
    }

    /**
     * Helper for parsing rational from TIFF metadata.
     * @param tiffDir
     * @param tag
     * @return
     */
    static double getRational(TIFFDirectory tiffDir, int tag) {
        long[] rational = tiffDir.getTIFFField(tag).getAsRational(0);
        return rational[0] / (double)rational[1];
    }
}


class stitchingImplementations {
    private static StitchingStrategy strategy

    static void setStitchingStrategy(StitchingStrategy strategy) {
        stitchingImplementations.strategy = strategy
    }

    static void stitchCore(String stitchingType, String folderPath, String compressionType, double pixelSizeInMicrons, double baseDownsample, String matchingString) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        switch(stitchingType) {
            case "Filename[x,y] with coordinates in microns":
                setStitchingStrategy(new FileNameStitchingStrategy())
                break

            case "Vectra tiles with metadata":
                setStitchingStrategy(new VectraMetadataStrategy())
                break

            case "Coordinates in TileCoordinates.txt file":
                setStitchingStrategy(new TileConfigurationTxtStrategy())
                break

            default:
                // Handle unexpected stitchingType
                Dialogs.showWarningNotification("Warning", "Error with choosing a stitching method, code here should not be reached in stitchingImplementations.groovy")
                return // Safely exit the method
        // Other cases for different stitching types
        }

        if(strategy) {

            def fileRegionPairs = strategy.prepareStitching(folderPath, pixelSizeInMicrons, baseDownsample, matchingString)
            OMEPyramidWriter.CompressionType compression = utilityFunctions.getCompressionType(compressionType)
            def builder = new SparseImageServer.Builder()

            if (fileRegionPairs == null || fileRegionPairs.isEmpty()) {
                Dialogs.showWarningNotification("Warning", "No valid folders found matching the criteria.")
                return // Exit the method to avoid further processing
            }
            fileRegionPairs.each { pair ->
                if (pair == null) {
                    logger.warn("Encountered a null pair in fileRegionPairs")
                    return // Skip this iteration
                }
                def file = pair['file'] as File
                def region = pair['region'] as ImageRegion

                if (file == null) {
                    logger.warn("File is null in pair: $pair")
                    return // Skip this iteration
                }

                if (region == null) {
                    logger.warn("Region is null in pair: $pair")
                    return // Skip this iteration
                }

                //logger.info("Processing file: ${file.path}, region: $region")
                def serverBuilder = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, file.toURI().toString()).getBuilders().get(0)
                builder.jsonRegion(region, 1.0, serverBuilder)
            }

            def server = builder.build()
            server = ImageServers.pyramidalize(server)

            long startTime = System.currentTimeMillis()
            def filename = Paths.get(folderPath).getFileName().toString()
            def outputPath = baseDownsample == 1 ?
                    Paths.get(folderPath).resolve(filename + '.ome.tif') :
                    Paths.get(folderPath).resolve(filename + '_' + (int) baseDownsample + 'x_downsample.ome.tif')

            def fileOutput = outputPath.toFile()
            String pathOutput = fileOutput.getAbsolutePath()

            new OMEPyramidWriter.Builder(server)
                    .tileSize(512)
                    .channelsInterleaved()
                    .parallelize(true)
                    .compression(compression)
                    .scaledDownsampling(baseDownsample, 4)
                    .build()
                    .writePyramid(pathOutput)

            long endTime = System.currentTimeMillis()
            println("Image written to ${pathOutput} in ${GeneralTools.formatNumber((endTime - startTime)/1000.0, 1)} s")
            server.close()
        } else {
            println("No valid stitching strategy set.")
        }
    }






}
