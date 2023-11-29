package qupath.ext.basicstitching.utilities

import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.common.GeneralTools
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.images.servers.ImageServers
import qupath.lib.images.servers.SparseImageServer
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.regions.ImageRegion
import qupath.lib.gui.QuPathGUI
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import qupath.ext.basicstitching.utilities.utils
import static qupath.lib.gui.scripting.QPEx.*

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.util.regex.Matcher
import java.util.regex.Pattern

class textfilestitching {
    public static void stitchByFileName(String folderPath, String compressionType, double pixelSizeInMicrons, double baseDownsample, String matchingString) {
        // Constructing the message to display
//        String message = "Folder Path: " + folderPath + "\n" +
//                "Compression Type: " + compressionType + "\n" +
//                "Pixel Size: " + pixelSize + "\n" +
//                "Downsample: " + downsample + "\n" +
//                "Matching String: " + matchingString
//
//        // Display the dialog with the constructed message
//        Dialogs.showConfirmDialog("Values Passed", message)
//        OMEPyramidWriter.CompressionType compresionType = getCompressionType(compressionType)
        Path rootdir = Paths.get(folderPath)
        OMEPyramidWriter.CompressionType compression = utils.getCompressionType(compressionType)
        Logger logger = LoggerFactory.getLogger(QuPathGUI.class);
        def subdir = []
        try {
            Files.newDirectoryStream(rootdir).each { path ->
                if (Files.isDirectory(path) && path.fileName.toString().contains(matchingString)) {
                    subdir.add(path)
                }
            }
        } catch (IOException e) {
            e.printStackTrace()
        }

        if (subdir.isEmpty()) {
            throw new RuntimeException("No folders found containing the string '$matchingString'")
        }

// At this point, we should have a list of folder locations in subdir
        for (folderOfTilesPath in subdir) {
            Path dir = Paths.get(folderOfTilesPath.toString())

            println "Processing slide in folder $dir"


            // Collect only .tif files
            def files = []
            Files.newDirectoryStream(dir, "*.tif*").each { path ->
                files.add(path.toFile())
            }

            ArrayList<List<Integer>> tileConfigOutput = buildTileConfigWithMinCoordinates(dir)
            List<Integer> tileConfig = tileConfigOutput[0]
            //print tileConfig
            Object minimumXY = tileConfigOutput[1]
            print minimumXY
            //Create the File Name for the resulting stitched image
            File fileOutput
            String filename = dir.getFileName().toString()
            def outputPath
            if (baseDownsample == 1){
                outputPath = rootdir.resolve(filename + '.ome.tif')
            } else {
                outputPath = rootdir.resolve(filename + '_' + (int) baseDownsample + 'x_downsample.ome.tif')
                print outputPath
            }

            fileOutput = outputPath.toFile()
            println "Output stitched file will be $fileOutput"


            // Parse image regions & create a sparse server
            print 'Parsing regions from ' + files.size() + ' files...'

            def builder = new SparseImageServer.Builder()
            files.parallelStream().forEach { f ->
                def region = parseRegion(f as File, tileConfig as List<Map>, minimumXY, pixelSizeInMicrons)
                if (region == null) {
                    print 'WARN: Could not parse region for ' + f
                    return
                }
                def serverBuilder = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, f.toURI().toString()).getBuilders().get(0)
                builder.jsonRegion(region, 1.0, serverBuilder)
            }
            print 'Building server...'
            def server = builder.build()
            server = ImageServers.pyramidalize(server)

            long startTime = System.currentTimeMillis()
            String pathOutput = fileOutput.getAbsolutePath()
            new OMEPyramidWriter.Builder(server)
            //.downsamples(server.getPreferredDownsamples()) // Use pyramid levels calculated in the ImageServers.pyramidalize(server) method
                    .tileSize(512)      // Requested tile size
                    .channelsInterleaved()      // Because SparseImageServer returns all channels in a BufferedImage, it's more efficient to write them interleaved
                    .parallelize(true)              // Attempt to parallelize requesting tiles (need to write sequentially)
                    .compression(compression)
                    .scaledDownsampling(baseDownsample, 4)
                    .build()
                    .writePyramid(pathOutput)
            long endTime = System.currentTimeMillis()
            print('Image written to ' + pathOutput + ' in ' + GeneralTools.formatNumber((endTime - startTime)/1000.0, 1) + ' s')
            server.close()
        }


    }
    /**
     * Builds a map of image file names and their coordinates extracted from the file names,
     * and finds the minimum X and Y coordinates.
     *
     * @param dirPath The path to the directory containing the image files.
     * @return A map containing a list of image configurations and an array [xmin, ymin].
     */
    static def buildTileConfigWithMinCoordinates(Path dir) {
        def images = []
        logger.info("buildTileCOnfigWithMinCoordinates: " + dir)
        Files.newDirectoryStream(dir, "*.{tif,tiff,ome.tif}").each { path ->
            //logger.info("path: " + path)
            def matcher = path.fileName.toString() =~ /.*\[(\d+),(\d+)\].*\.(tif|tiff|ome.tif)$/
            if (matcher.matches()) {
                //logger.info("Match!")
                def imageName = path.getFileName().toString()
                int x = Integer.parseInt(matcher[0][1])
                //logger.info("x: "+x)
                int y = Integer.parseInt(matcher[0][2])
                images << ['imageName': imageName, 'x': x, 'y': y]
            }
        }

        def minX = images.min { it.x }?.x ?: 0
        def minY = images.min { it.y }?.y ?: 0

        return [images, [minX, minY]]
    }


    static Map<String, Integer> getTiffDimensions(filePath) {

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
    /**
     * Parse an ImageRegion from the TileConfiguration.txt data and TIFF file dimensions.
     * @param imageName Name of the image file for which to get the region.
     * @param tileConfig List of tile configurations parsed from TileConfiguration.txt.
     * @param z index of z plane.
     * @param t index of timepoint.
     * @return An ImageRegion object representing the specified region of the image.
     */
    static ImageRegion parseRegionFromOffsetTileConfig(File file, List<Map> tileConfig, minimumXY, pixelSizeInMicrons, int z = 0, int t = 0) {
        String imageName = file.getName()
        def config = tileConfig.find { it.imageName == imageName }

        if (config) {
            ////////////////////////////////////////////////
            int x = (config.x-minimumXY[0])/pixelSizeInMicrons as int
            int y = (config.y-minimumXY[1])/pixelSizeInMicrons as int
            def dimensions = getTiffDimensions(file)
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
    //Possibility that someday several of these stitching scripts could be merged at this point, based on the inputs.
    static ImageRegion parseRegion(File file,List<Map> tileConfig, minimumXY,pixelSizeInMicrons, int z = 0, int t = 0) {

        try {
            //return parseRegionFromTIFF(file, z, t)
            return parseRegionFromOffsetTileConfig(file, tileConfig, minimumXY, pixelSizeInMicrons)
        } catch (Exception e) {
            logger.info(e.getLocalizedMessage())
        }
    }
}
