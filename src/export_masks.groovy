// This script loops over every channel, calculating the optimal threshold 
// through the Otsu method and outputs a mask for that channel. Then writes all masks
// as a multi-channel pyramidal .ome.tiff

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

def proj = QP.getProject()
def filename = ("prediction_image_12.ome.tif" as String)
def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)

def imageData = QP.getCurrentImageData()
def imgServer = imageData.getServer()
def channels = getCurrentServer().getMetadata().getChannels()
def cal = imgServer.getPixelCalibration()
def resolution = cal.createScaledInstance(1, 1)
def above = getPathClass("Positive")
def below = getPathClass("Ignore*")


// Downsample value for calculating threshold
// Should be increased if error "invalid scan stride" is thrown
double thresholdDownsample = 32
def roi = getSelectedObject()

logger.info("Calculating thresholds for all channels...")

// for every channel:
for (i=0; i<channels.size(); i++) {

    // calculate the Otsu threshold
    def threshold = autoThreshold(roi, i, thresholdDownsample)
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
.downsamples(maskServer.getPreferredDownsamples())
.channelsInterleaved()
.pixelType(PixelType.UINT8)
.compression(OMEPyramidWriter.CompressionType.LZW) // .LZW  .ZLIB   .UNCOMPRESSED
.build()
.writePyramid(outpth as String)


print(outpth.toString())

// Function that calls imageJ autothresholder to determine threshold values
def autoThreshold(annotation, channel, thresholdDownsample) {
    def qupath = getQuPath()
    def imageData = getCurrentImageData()
    def server = imageData.getServer()
    def classifierChannel

    server = new TransformedServerBuilder(server).extractChannels(channel).build()
    classifierChannel = ColorTransforms.createChannelExtractor(channel)

    // Determine threshold value
    ROI pathROI = annotation.getROI() // Get QuPath ROI
    PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(server.getPath(), thresholdDownsample, pathROI)) // Get PathImage within bounding box of annotation
    def ijRoi = IJTools.convertToIJRoi(pathROI, pathImage) // Convert QuPath ROI into ImageJ ROI
    ImagePlus imagePlus = pathImage.getImage() // Convert PathImage into ImagePlus
    ImageProcessor ip = imagePlus.getProcessor() // Get ImageProcessor from ImagePlus
    ip.setRoi(ijRoi) // Add ImageJ ROI to the ImageProcessor to limit the histogram to within the ROI only

    ip.setAutoThreshold("Otsu")

    double thresholdValue = ip.getMaxThreshold()

    return thresholdValue
    }
