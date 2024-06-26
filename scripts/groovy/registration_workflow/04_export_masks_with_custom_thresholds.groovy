/**
 * This script allows you to set an array of thresholds,
 * it will export an 8-bit multi-channel binary mask collection.
 * 
 * Images are thresholded through set thresholds and
 * median-filtered (4x4 kernel) for denoising.
 * 
 * QuPath v0.5.1.
 *
 * @author Idris Iritas
**/

import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.roi.interfaces.ROI
import qupath.imagej.tools.IJTools
import qupath.lib.images.PathImage
import qupath.lib.regions.RegionRequest
import ij.ImagePlus
import ij.process.ImageProcessor
import ij.process.AutoThresholder
import qupath.opencv.ml.pixel.PixelClassifiers
import qupath.lib.scripting.QP
import qupath.lib.gui.viewer.OverlayOptions
import qupath.lib.gui.viewer.RegionFilter
import qupath.lib.gui.viewer.overlays.PixelClassificationOverlay
import qupath.lib.images.servers.ColorTransforms.ColorTransform
import qupath.lib.images.ImageData
import qupath.opencv.ops.ImageOp
import qupath.opencv.ops.ImageOps
import qupath.opencv.ml.pixel.PixelClassifierTools
import qupath.lib.images.writers.ome.OMEPyramidWriter
import java.nio.file.FileSystems
import qupath.opencv.ops.ImageOps

def channelServers = []

// Enter threshold for each channel below
float[] thresholdArray = [530, 280, 270, 350, 200, 780, 1000]

def proj = QP.getProject()
def filename = ("prediction_image.ome.tif" as String)
def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)

def imageData = QP.getCurrentImageData()
def imgServer = imageData.getServer()
def channels = getCurrentServer().getMetadata().getChannels()
def cal = imgServer.getPixelCalibration()
def resolution = cal.createScaledInstance(1, 1)
def above = getPathClass("Positive")
def below = getPathClass("Ignore*")

def roi = getSelectedObject()

logger.info("Calculating thresholds for all channels...")

// for every channel:
for (i=0; i<channels.size(); i++) {

    def threshold = thresholdArray[i]
    def classifier = qupath.opencv.ml.pixel.PixelClassifiers.createThresholdClassifier(resolution, i, threshold, below, above)
    var server = PixelClassifierTools.createPixelClassificationServer(imageData, classifier)
    
    // Store channel mask server in array
    channelServers.add(server)
}

// channel IDs of only masks
int[] maskChannels = (channels.size()..(channels.size()*2)-1).toArray()

// Add classification channels to imageServer
def preServer = new TransformedServerBuilder(imgServer)
                        .concatChannels(channelServers)
                        .build()

// Remove first half of channels, as these are not masks
def maskServer = new TransformedServerBuilder(preServer)
                        .extractChannels(maskChannels)
                        .build()

double exportDownsample = 1
def region = RegionRequest.createInstance(imgServer.getPath(), exportDownsample, roi.getROI())

// Extract mask imageData from maskServer for processing operations
def maskImageData = new ImageData(maskServer, null, imageData.getImageType())

// Define processing operations for images
def op = ImageOps.buildImageDataOp()
    .appendOps(ImageOps.Core.ensureType(qupath.lib.images.servers.PixelType.UINT8),
               ImageOps.Filters.median(4))

// Create imageServer with Median blur applied
def serverSmooth = ImageOps.buildServer(maskImageData, op, imageData.getServer().getPixelCalibration())

// Export should take about 3 hours when not writing pyramids
new OMEPyramidWriter.Builder(serverSmooth)
.region(region)
.parallelize(36) // Adjust based on threads
.downsamples(serverSmooth.getPreferredDownsamples())
.channelsInterleaved()
.pixelType(PixelType.UINT8)
.compression(OMEPyramidWriter.CompressionType.LZW) // .LZW  .ZLIB   .UNCOMPRESSED
.build()
.writePyramid(outpth as String)


print(outpth.toString())