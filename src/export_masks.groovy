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
import qupath.opencv.ops.ImageOp
import qupath.opencv.ops.ImageOps
import qupath.opencv.ml.pixel.PixelClassifierTools
import qupath.lib.images.writers.ome.OMEPyramidWriter
import java.nio.file.FileSystems


def channelServers = []

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


// downsample value for calculating threshold
double thresholdDownsample = 8
def roi = getSelectedObject()

// for channel in channels:
//    threshold = autoThreshold(channel)
//    thresholdValues.add threshold

// for every channel:
for (i=0; i<channels.size(); i++) {
    
    // calculate the Otsu threshold
    def threshold = autoThreshold(roi, i, thresholdDownsample)
    def classifier = qupath.opencv.ml.pixel.PixelClassifiers.createThresholdClassifier(resolution, i, threshold, below, above)
    var server = PixelClassifierTools.createPixelClassificationServer(imageData, classifier)
    
    // Store channel mask server in array
    channelServers.add(server)
}

// FROM THIS POINT I DONT KNOW HOW TO MAKE THE WRITER WRITE THE MASKS. This works, but adds the 40 mask channels
// After the 40 regular channels. How do I initialize an empty image Server?
def maskServer = new TransformedServerBuilder(imgServer)
                        .concatChannels(channelServers)
                        .build()

double exportDownsample = 1
def region = RegionRequest.createInstance(imgServer.getPath(), exportDownsample, roi.getROI())

new OMEPyramidWriter.Builder(maskServer)
.region(region)
.parallelize() // Adjust based on threads
.downsamples(maskServer.getPreferredDownsamples())
.channelsInterleaved()
.pixelType(PixelType.UINT8)
.compression(OMEPyramidWriter.CompressionType.LZW) // .LZW  .ZLIB   .UNCOMPRESSED
.build()
.writePyramid(outpth as String)


print(outpth.toString())

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
