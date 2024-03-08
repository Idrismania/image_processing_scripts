import java.nio.file.FileSystems
import java.awt.image.BufferedImage
import qupath.lib.scripting.QP
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.images.servers.ImageServer

def all_detections = QP.getDetectionObjects()

def imageServer = QP.getCurrentServer()

def measurements = ObjectMeasurements.Measurements.values() as List
def compartments = ObjectMeasurements.Compartments.values() as List
def downsample = 1.0

for (detection in all_detections) {
    ObjectMeasurements.addIntensityMeasurements(
      imageServer, detection, downsample, measurements, compartments
      )
}
