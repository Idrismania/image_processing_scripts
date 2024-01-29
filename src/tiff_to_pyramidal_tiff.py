"""WHAT IS RESOLUTION AND RESOLUTIONUNIT AND PIXELSIZE? I DONT KNOW."""

import argparse
import numpy as np
from math import log2
from pathlib import Path
from PIL import Image
from tifffile import TiffWriter, COMPRESSION


def main():

    # Parse arguments
    parser = argparse.ArgumentParser(
            description="File path parser",
            epilog="Example: src/tiff_to_pyramidal_tiff.py input.tiff output.tiff 4")

    parser.add_argument("input", help="Path to the first input image")
    parser.add_argument("output", help="Path to the second input image")

    args = parser.parse_args()

    # Load image and convert to numpy array
    image = Image.open(Path(args.input))
    image_array = np.array(image)

    
    with TiffWriter(Path(args.output), bigtiff=False) as tif:

        # subifds tells the writer in advance how many lower-resolution
        # images will be associated with this image
        n_subifds = int(log2(max(image_array.shape[:2])/256))

        options = dict(tile=(128, 128),
                       compression=COMPRESSION.ZSTD,
                       resolutionunit='CENTIMETER')

        tif.write(image_array, subifds=n_subifds, resolution=(1e4, 1e4), **options)

        for level in range(n_subifds):
            
            mag = 2 ** (level + 1)

            image_array_mag = image_array[::mag, ::mag]

            # subfiletype 1 tells the writer these are low-res images that arent to be opened regularly
            tif.write(image_array_mag, subfiletype=1, resolution=(1e4 / mag, 1e4 / mag), **options)





if __name__ == "__main__":
    main()
