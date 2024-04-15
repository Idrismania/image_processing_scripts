import java.nio.file.FileSystems
import java.awt.image.BufferedImage
import qupath.lib.scripting.QP
import qupath.lib.images.servers.ImageServer

def all_detections = QP.getDetectionObjects()

all_detections.forEach { detection ->

    def startclass = detection.getClassifications()[0]
    

    
    if (startclass == null) {

        detection.setClassifications(["Unclassified"])
        
    }
    // Workaround for composite class integration
    else if (detection.getClassifications()[0] == "Tumor" &&
             detection.getClassifications()[1] == "proliferating_ki67") {
                 
        detection.setClassifications(["Tumor", "proliferating_ki67"])
        
    }
    
    else {
        detection.setClassifications([startclass])
    }
}