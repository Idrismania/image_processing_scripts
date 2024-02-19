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


def channelNames = getCurrentServer().getMetadata().getChannels().collect { c -> c.name }

// Loop over actual channels, excluding irrelevant ones
for (i=5; i<32; i++) {
    
    // Parse filename, may need to be edited per whole slide
    def raw_filename = channelNames.get(i)
    def marker = raw_filename.tokenize("#").get(1).tokenize("1").get(0)
    def filename = ("T19-01454" + marker + ".ome.tif" as String)

    def downsample = 1
    def proj = QP.getProject()
    def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, filename)
    
    // Can be called by name as String, or by Index.
    def singleChannelServer = new TransformedServerBuilder(QP.getCurrentServer() as ImageServer<BufferedImage>).extractChannels(i).build()
    
    def tsb_converted = new TypeConvertServer(singleChannelServer, 64f, 0f)
    
    def roi = QP.getSelectedObject().getROI()
    def region = RegionRequest.createInstance(singleChannelServer.getPath(), downsample, roi)
    new OMEPyramidWriter.Builder(tsb_converted)
    .region(region)
    .parallelize()
    .downsamples(tsb_converted.getPreferredDownsamples())
    .compression(OMEPyramidWriter.CompressionType.DEFAULT)
    .build()
    .writePyramid(outpth as String)    
    print(outpth.toString())
}

class TypeConvertServer extends TransformingImageServer<BufferedImage> {

    private float scale = 1f
    private float offset = 0
    private ImageServerMetadata originalMetadata
    def cm = ColorModelFactory.getDummyColorModel(8)

    protected TypeConvertServer(ImageServer<BufferedImage> server, float scale, float offset) {
        super(server)
        this.scale = scale
        this.offset = offset
        this.originalMetadata = new ImageServerMetadata.Builder(server.getMetadata())
            .pixelType(PixelType.UINT8)
            .build()
    }

    public ImageServerMetadata getOriginalMetadata() {
        return originalMetadata
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        throw new UnsupportedOperationException()
    }

    @Override
    protected String createID() {
        return TypeConvertServer.class.getName() + ": " + getWrappedServer().getPath() + " scale=" + scale + ", offset=" + offset
    }

    @Override
    String getServerType() {
        return "Type converting image server"
    }

    public BufferedImage readBufferedImage(RegionRequest request) throws IOException {
        def img = getWrappedServer().readBufferedImage(request);
        def raster = img.getRaster()
        int nBands = raster.getNumBands()
        int w = img.getWidth()
        int h = img.getHeight()
        def raster2 = WritableRaster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, nBands, null)
        float[] pixels = null
        for (int b = 0; b < nBands; b++) {
            pixels = raster.getSamples(0, 0, w, h, b, (float[])pixels)
            for (int i = 0; i < w*h; i++) {
                pixels[i] = (float)GeneralTools.clipValue(pixels[i] * scale + offset, 0, 255)
            }
            raster2.setSamples(0, 0, w, h, b, pixels)
        }
        return new BufferedImage(cm, raster2, false, null)
    }

}
