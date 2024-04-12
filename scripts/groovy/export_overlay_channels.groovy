import qupath.lib.color.ColorModelFactory
import qupath.lib.images.servers.ImageServer
import qupath.lib.regions.RegionRequest
import qupath.lib.scripting.QP
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.images.servers.TransformingImageServer
import qupath.lib.images.servers.ImageServerMetadata
import qupath.lib.images.servers.ImageServerBuilder
import java.awt.image.BufferedImage
import java.nio.file.FileSystems


// def channelNames = getCurrentServer().getMetadata().getChannels().collect { c -> c.name }
//setChannelNames('PDL1', 'CD8', 'FoxP3', 'CD68', 'PD1', 'CK')

// Specify channel indexes to extract. 0-based, so if H&E is present, fluorescent channels start at index 3
int[] channelArray = [3, 4, 5, 6, 7]

def filename = ("WSI_registered.ome.tif" as String)

def downsample = 1
def proj = QP.getProject()
def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)

// Can be called by name as String, or by Index.
def singleChannelServer = new TransformedServerBuilder(QP.getCurrentServer() as ImageServer<BufferedImage>).extractChannels(channelArray).build()
def roi = QP.getSelectedObject().getROI()
def region = RegionRequest.createInstance(singleChannelServer.getPath(), downsample, roi)
new OMEPyramidWriter.Builder(singleChannelServer)
.region(region)
.parallelize(36) // Adjust based on threads
.downsamples(singleChannelServer.getPreferredDownsamples())
.channelsInterleaved()
.pixelType(PixelType.UINT16)
.compression(OMEPyramidWriter.CompressionType.DEFAULT) // .LZW  .ZLIB   .UNCOMPRESSED
.build()
.writePyramid(outpth as String)

print(outpth.toString())