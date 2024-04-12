import qupath.lib.images.servers.ImageServer
import qupath.lib.regions.RegionRequest
import qupath.lib.scripting.QP
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.images.servers.TransformedServerBuilder
import java.awt.image.BufferedImage
import java.nio.file.FileSystems

def filename = "exported_channel.ome.tif"
def downsample = 1
def proj = QP.getProject()
def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)

// Can be called by name as String, or by Index. 0 is DAPI in my case
def singleChannelServer = new TransformedServerBuilder(QP.getCurrentServer() as ImageServer<BufferedImage>).extractChannels(0).build()

def roi = QP.getSelectedObject().getROI()
def region = RegionRequest.createInstance(singleChannelServer.getPath(), downsample, roi)

// If specific downsamplings are required, pass an array like below to OMEPyramidWriter.Builder.downsamples()
// double[] downSampling = [1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0]

new OMEPyramidWriter.Builder(singleChannelServer)
.region(region)
.parallelize(36) // Adjust based on threads
// Use line below for specific downsampling
//.downsamples(downSampling)
.downsamples(singleChannelServer.getPreferredDownsamples())
.pixelType(PixelType.UINT8) // Adjust based on requirement
.compression(OMEPyramidWriter.CompressionType.DEFAULT) // .LZW  .ZLIB   .UNCOMPRESSED
.build()
.writePyramid(outpth as String)
print(outpth.toString())





