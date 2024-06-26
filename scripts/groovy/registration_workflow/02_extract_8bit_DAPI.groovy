/**
 * Script to extract DAPI channel from multi-channel image
 * to 8-bit without causing integer overflows.
 *
 * 8-bit format is necessary for Warpy registration.
 *
 * Note that var op includes a division by 255. This value
 * may be adjusted to optimize the dynamic range of the data.
 * 
 * QuPath v0.5.1.
 *
 * @author Pete Bankhead, edited by Idris Iritas
**/

import qupath.lib.images.servers.PixelType
import qupath.opencv.ops.ImageOps
import qupath.lib.images.servers.ImageServers
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.images.servers.ImageServerMetadata
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.images.servers.TransformingImageServer
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.gui.commands.display.BrightnessContrastHistogramPane
import qupath.lib.display.DirectServerChannelInfo
import qupath.lib.images.servers.ColorTransforms

// Can be called by name as String, or by Index. 0 is DAPI in my case
def singleChannelServer = new TransformedServerBuilder(QP.getCurrentServer() as ImageServer<BufferedImage>).extractChannels(0).build()

def imageData = new ImageData(singleChannelServer)

// Output downsample (may be 1 for full-resolution)
double outputDownsample = 1

// Output server path
def path = buildFilePath(PROJECT_BASE_DIR, 'DAPI_8-bit_no_pyramidal.ome.tif')

// Create an op to handle all transforms
def op = ImageOps.buildImageDataOp()
    .appendOps(
            ImageOps.Core.divide(255),
            ImageOps.Core.ensureType(PixelType.UINT8)
    )

// Create a scaling & bit-depth-clipping server
def server = ImageOps.buildServer(
        imageData,
        op,
        imageData.getServer().getPixelCalibration().createScaledInstance(outputDownsample, outputDownsample))

// Ensure the server is pyramidal
List<Double> downsamples = [outputDownsample]
double d = outputDownsample * 4
while (Math.min(server.getWidth()/d, server.getHeight()/d) >= 512) {
    downsamples << d
    d *= 4
}
server = ImageServers.pyramidalize(server, downsamples as double[])

double[] downSampling = [1.0]

new OMEPyramidWriter.Builder(server)
.parallelize(36) // Adjust based on threads
.downsamples(downSampling)
.pixelType(PixelType.UINT8) // Adjust based on requirement
.compression(OMEPyramidWriter.CompressionType.LZW) // .LZW  .ZLIB   .UNCOMPRESSED .JPEG
.channelsInterleaved()
.build()
.writePyramid(path as String)