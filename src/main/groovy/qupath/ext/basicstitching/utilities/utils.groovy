package qupath.ext.basicstitching.utilities

import qupath.lib.images.writers.ome.OMEPyramidWriter

class utils {
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
}
