/**
 * Simple script for exporting an H&E ROI to
 * ome.tiff. This is necessary for using Warpy
 * image registration.
 * 
 * QuPath v0.5.1.
 *
 * @author Idris Iritas
**/

import qupath.lib.images.servers.ImageServer
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.regions.RegionRequest
import qupath.lib.scripting.QP
import qupath.lib.images.writers.ome.OMEPyramidWriter
import java.awt.image.BufferedImage
import java.nio.file.FileSystems

def filename = 'he_wsi.ome.tif'
def downsample = 1
def proj = QP.getProject()
def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)
myServer = QP.getCurrentServer()

def roi = QP.getSelectedObject().getROI()
def region = RegionRequest.createInstance(myServer.getPath(), downsample, roi)

new OMEPyramidWriter.Builder(myServer)
.region(region)
.parallelize(36) // Adjust based on threads
.downsamples(myServer.getPreferredDownsamples())
.pixelType(PixelType.UINT8) // Adjust based on requirement
.compression(OMEPyramidWriter.CompressionType.LZW) // .LZW  .ZLIB   .UNCOMPRESSED .JPEG
.build()
.writePyramid(outpth as String)

print(outpth.toString())
