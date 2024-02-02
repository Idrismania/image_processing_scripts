import qupath.lib.objects.PathObjects
import qupath.lib.roi.RoiTools
import qupath.lib.roi.interfaces.ROI
import qupath.lib.scripting.QP
import java.awt.geom.AffineTransform


static ROI transformROI(ROI roi, AffineTransform transform) {
    def shape = RoiTools.getShape(roi)
    def shape2 = transform.createTransformedShape(shape)
    return RoiTools.getShapeROI(shape2, roi.getImagePlane(), 0.5)
}

def detections = QP.getDetectionObjects()

double[] matrix = [1.0, 0.0,
		           0.0, 1.0,
		           0.0, 0.0]

def transform = new AffineTransform(
        matrix[0], matrix[1], 
        matrix[2], matrix[3], 
        matrix[4], matrix[5]
)

double[] inversetransform = [0.0, 1.0, 0.0,
                             1.0, 0.0, 0.0]

transform.createInverse().getMatrix(inversetransform)
print("FORWARD = " + matrix.toString())
print("INVERSE = " + inversetransform.toString())

def newObjects = []

detections.forEach({
    def newroi_cell = transformROI(it.getROI(), transform)
    def newroi_nucleus = transformROI(it.getNucleusROI(), transform)
    newObjects.add(PathObjects.createCellObject(newroi_cell, newroi_nucleus, it.getPathClass(), it.getMeasurementList()))
})

QP.clearDetections()
QP.addObjects(newObjects)
