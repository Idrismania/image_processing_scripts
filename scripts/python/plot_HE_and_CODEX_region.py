"""This script takes an H&E image and a CODEX mask. It will plot identical
regions of the image side-by-side."""

import argparse
import matplotlib.pyplot as plt
import numpy as np
import uuid
import warnings
from pathlib import Path
from PIL import Image


def main():

    # Initialize argparser
    parser = argparse.ArgumentParser(
        description="Image argument parser",
        epilog="Example: src\plot_HE_and_CODEX_region.py data\core_YB_b_14\HE.tiff data\core_YB_b_14\masks\C01_DAPI_1.tiff 512 512 -plot -offset 512 671 -o .\output\\",
    )
    # Add arguments for arg parses
    parser.add_argument("image", help="Path to the first input image")
    parser.add_argument("mask", help="Path to the second input image")
    parser.add_argument(
        "-size",
        nargs=2,
        type=int,
        metavar=("X", "Y"),
        help="Region X and Y size",
        default=[512, 512],
    )
    parser.add_argument("-plot", action="store_true", help="Flag for plotting")
    parser.add_argument("-save", action="store_true", help="Flag for saving")
    parser.add_argument(
        "-offset",
        nargs=2,
        type=int,
        metavar=("X", "Y"),
        help="Plotting offset",
    )
    parser.add_argument("-o", "--output", metavar="", help="Output folder path")

    args = parser.parse_args()

    size_x, size_y = args.size
    he_image = Image.open(Path(args.image))
    mask = Image.open(Path(args.mask))

    # If no offset is provided, calculate the centre of the image
    if args.offset is None:
        width, height = he_image.size
        offset_x = int(width / 2 - size_x)
        offset_y = int(height / 2 - size_y)
        warnings.warn("No offsets specified. Offset defaults to image center.", Warning)
    else:
        offset_x, offset_y = args.offset

    he_image = he_image.crop((offset_x, offset_y, offset_x + size_x, offset_y + size_y))

    mask = mask.crop((offset_x, offset_y, offset_x + size_x, offset_y + size_y))

    if args.plot:

        plt.subplot(1, 2, 1)
        plt.imshow(he_image)
        plt.title("H&E")

        plt.subplot(1, 2, 2)
        plt.imshow(mask)
        plt.title("CODEX mask")

        plt.show()

    if args.save:
        if args.output is None:
            raise TypeError("No output folder specified to save images to.")
        # Generate randomized filenames and save
        he_image.save(Path(args.output, f"{uuid.uuid4().hex}.png"))
        mask.save(Path(args.output, f"{uuid.uuid4().hex}.png"))


if __name__ == "__main__":
    main()
