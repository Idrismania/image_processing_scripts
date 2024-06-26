import qupath.lib.geom.Point2
import qupath.lib.images.servers.ImageServer
import qupath.lib.images.writers.PngWriter
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.ROIs
import qupath.lib.scripting.QP
import java.awt.image.BufferedImage
import java.nio.file.FileSystems

def IMAGE_SIZE = 137  // Half of final image region - 1
def downsample = 1

def proj = QP.getProject()
ImageServer<BufferedImage> myserver = QP.getCurrentServer() as ImageServer<BufferedImage>
def all_detections = QP.getDetectionObjects()

all_detections.each { detection ->

    // Retrieve detection information
    def ID = detection.getID().toString()
    def roi = detection.getROI()
    def centroid_X = roi.getCentroidX()
    def centroid_Y = roi.getCentroidY()
    
    // Prepare region and filenames for exporting
    def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, 'cells', ID + ".png")
    def mywriter = new PngWriter()
    def imagePlane = roi.getImagePlane()
    def export_region = ROIs.createRectangleROI(centroid_X-IMAGE_SIZE, centroid_Y-IMAGE_SIZE, 2*IMAGE_SIZE, 2*IMAGE_SIZE, imagePlane)
    def region = RegionRequest.createInstance(myserver.getPath(), downsample, export_region)
    mywriter.writeImage(myserver, region, outpth as String)
}