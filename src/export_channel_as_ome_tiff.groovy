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

// Can be called by name as String, or by Index.
def singleChannel = new TransformedServerBuilder(QP.getCurrentServer() as ImageServer<BufferedImage>).extractChannels("DAPI").build()

def roi = QP.getSelectedObject().getROI()
def region = RegionRequest.createInstance(singleChannel.getPath(), downsample, roi)
def mywriter = new OMEPyramidWriter()

mywriter.writeImage(singleChannel, outpth as String, OMEPyramidWriter.CompressionType.DEFAULT, region)
print(outpth.toString())
