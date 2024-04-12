"""Script takes folder with images as input, merges channels and saves to given output path"""

import argparse
import numpy as np
import os
import tifffile
import warnings
from pathlib import Path
from PIL import Image
from tqdm import tqdm


def main() -> None:

    parser = argparse.ArgumentParser(
        description="File path parser",
        epilog="Example: src/print_tiff_metadata.py input.tiff",
    )

    parser.add_argument("input", help="Path to folder with images to merge")
    parser.add_argument("output", help="Path + Filename to save merged tiff to")
    args = parser.parse_args()

    input_folder = Path(args.input)
    output_path = Path(args.output)

    if ".ome." not in str(output_path):
        warnings.warn("Warning: Not saving as .ome.tiff may lead to issues when saving certain Tiff files.")

    masks = os.listdir(input_folder)
    
    # Retrieve image shape to intialize array
    width, height = Image.open(Path(input_folder, masks[0])).size

    # Initialize array with same amount of channels as images in input folder
    image_array = np.empty((len(masks), height, width), dtype=np.uint8)

    print("Writing...")

    with tifffile.TiffWriter(output_path) as tif:
        
        for i, mask in enumerate(tqdm(masks)):
        
            image = Image.open(Path(input_folder, mask))

            image_array[i, :, :] = np.array(image)

        tif.write(
            image_array[:, :, :],
            metadata={"axes": "CYX", "photometric": "minisblack"}
        )


    print("Done!")


if __name__ == "__main__":
    main()
