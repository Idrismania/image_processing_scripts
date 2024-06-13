/**
 * Given an image with classified detections,
 * this script exports a 275x275 px patch per
 * detection centroid, adhering to the naming
 * convention used by ChangeGamers.
 * 
 * QuPath v0.5.1.
 *
 * @author Idris Iritas
**/

import qupath.lib.images.writers.JpegWriter
import java.nio.file.FileSystems
import java.nio.file.Files
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.ROIs
import qupath.lib.objects.PathObject
import qupath.lib.images.servers.ImageServer
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

// Image parameters
def imageID = 0
def imageMPP = 0.5
def tileSize = 275
def downsample = 1    // 1 = Full resolution

// Retrieve imageServer for exporting
def proj = QP.getProject()
ImageServer<BufferedImage> myserver = QP.getCurrentServer() as ImageServer<BufferedImage>

// Retrieve all detections
def all_detections = QP.getDetectionObjects()

// Initialize an index for cell_idx tracking
AtomicInteger index = new AtomicInteger(0)

// Output directory
def outputDir = FileSystems.getDefault().getPath(proj.getPath().parent.toString(), 'cells')

// Ensure output directory exists
Files.createDirectories(outputDir)

// Iterate over detection objects
all_detections.each { detection ->

    // Extract StarDist segmentation probability from measurements
    def objMeasurements = detection.getMeasurements()
    def detectionProbability = objMeasurements["Detection probability"]
    
    // Retrieve detection information
    def roi = detection.getROI()
    def centroid_X = roi.getCentroidX()
    def centroid_Y = roi.getCentroidY()
    
    // Compute offsets for naming convention
    def offset_X = Math.round(centroid_X) - tileSize.intdiv(2)
    def offset_Y = Math.round(centroid_Y) - tileSize.intdiv(2)
    
    // Format string according to naming convention
    def outFileName = "image_${imageID}_uuid_certainty_${detectionProbability}_x_offset_${offset_X}_y_offset_${offset_Y}_target_mpp_${imageMPP}_idx_${index.getAndIncrement()}.jpg"
    def outFilePath = outputDir.resolve(outFileName)
    
    def mywriter = new JpegWriter()
    def imagePlane = roi.getImagePlane()
    def export_region = ROIs.createRectangleROI(offset_X, offset_Y, tileSize, tileSize, imagePlane)
    def region = RegionRequest.createInstance(myserver.getPath(), downsample, export_region)
    
    // Synchronized block to avoid race conditions
    synchronized (this) {
        // Write the image
        mywriter.writeImage(myserver, region, outFilePath.toString())
    }
}

println("Done!")
