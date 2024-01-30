"""This script takes an image (single-channel or RGB) and writes it as pyramidal ome tiff.
Script takes 3 arguments. An input image, output path and optional pixel size in µm/pixel."""

import argparse
import numpy as np
from math import log2
from pathlib import Path
from PIL import Image
from tifffile import TiffWriter, COMPRESSION


def main() -> None:

    # Parse arguments
    parser = argparse.ArgumentParser(
            description="File path parser",
            epilog="Example: src/tiff_to_pyramidal_tiff.py input.tiff output.tiff 4")

    parser.add_argument("input", help="Path to the first input image")
    parser.add_argument("output", help="Path to the second input image")
    parser.add_argument("-pixelsize", help="Scale in µm per pixel. Defaults to 1.", default=1)

    args = parser.parse_args()

    # Load image and convert to numpy array
    image = Image.open(Path(args.input))
    image_array = np.array(image)

    
    with TiffWriter(Path(args.output), bigtiff=True) as tif:
        
        pixelsize = float(args.pixelsize)
        # subifds tells the writer in advance how many lower-resolution
        # images will be associated with this image
        n_subifds = int(log2(max(image_array.shape)/256))
        
        metadata = {
            "SignificantBits": 8,
            "PhysicalSizeX": pixelsize,
            "PhysicalSizeXUnit": "µm",
            "PhysicalSizeY": pixelsize,
            "PhysicalSizeYUnit": "µm",
        }

        # Adjust the number of axes depending on image type
        if len(image_array.shape) == 2:
            metadata["axes"] = "YX"
        elif len(image_array.shape) == 3:
            metadata["axes"] = "YXC"
        else:
            raise ValueError("Unexpected image shape. Writer only supports single-channel and RGB images")

        options = dict(
            tile=(128, 128),
            # compression=COMPRESSION.ZSTD,
            resolutionunit="CENTIMETER"
            )

        if len(image_array.shape) == 2:
            options["photometric"] = "minisblack"
        elif len(image_array.shape) == 3:
            options["photometric"] = "rgb"
        else:
            raise ValueError("Unexpected image shape. Writer only supports single-channel and RGB images")

        tif.write(
            image_array,
            subifds=n_subifds,
            resolution=(1e4 / pixelsize, 1e4 / pixelsize),
            metadata=metadata,
            **options
            )

        for level in range(n_subifds):
            
            mag = 2 ** (level + 1)

            if metadata["axes"] == "YX":
                pyramidal_slice_arg = image_array[::mag, ::mag]
            elif metadata["axes"] == "YXC":
                pyramidal_slice_arg = image_array[::mag, ::mag, :]
            else:
                raise ValueError("Unsupported axes configuration in metadata")

            # subfiletype 1 tells the writer these are low-res images that arent to be opened regularly
            tif.write(
                pyramidal_slice_arg,
                subfiletype=1,
                resolution=(1e4 / mag / pixelsize, 1e4 / mag / pixelsize),
                **options
                )

        thumbnail = (image_array[::32, ::32] >> 2).astype("uint8")
        tif.write(thumbnail, metadata={"Name": "thumbnail"}, **options)


if __name__ == "__main__":
    main()