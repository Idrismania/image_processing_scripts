"""
This script takes an H&E Whole-slide image in .ome tiff format,
corresponding multi-channel image mask in .ome.tiff format and
output directory containg subdirectories 'he' and 'masks' as input.

It will generate patches with no overlap from the WSI and export
H&E patches and corresponding mask regions with identical index names.

Script offers parameters to adjust MPP, tile size and percentage of foreground to tile a region.

Note that this script requires paths to Pyvips and Openslide binaries.
"""

import os
from pathlib import Path

# File paths of H&E and mask image to generate tiles from, as well as a reference to pyvips and openslide binaries paths necessay for import


HE_FILE_PATH = Path("data", "d161fa98-0035-4c66-80f3-c02e11ff84ce_he.ome.tif")
MASK_FILE_PATH = Path("data", "d161fa98-0035-4c66-80f3-c02e11ff84ce_mask.ome.tif") 

PYVIPS_BIN_PATH = Path("backends", "pyvips", "bin")
OPENSLIDE_BIN_PATH = Path("backends", "openslide", "bin")

OUTPUT_PATH = Path("data", "output")

# Tiling parameters
TARGET_MPP = 0.2425
TILE_SIZE = (512, 512) # 512
FOREGROUND_STRINGENCY = 0.9

# Dynamically add pyvips bin path to PATH for ctypes libray to find it during import
basepath = os.path.abspath(os.path.join(os.path.dirname( __file__ ), "..\.."))
dllspath = os.path.join(basepath, PYVIPS_BIN_PATH)
os.environ['PATH'] = dllspath + os.pathsep + os.environ['PATH']

with os.add_dll_directory(dllspath):
    import pyvips

dllspath = os.path.join(basepath, OPENSLIDE_BIN_PATH)
os.environ['PATH'] = dllspath + os.pathsep + os.environ['PATH']

with os.add_dll_directory(dllspath):
    import openslide

import dlup
import numpy as np
import tifffile
from tqdm import tqdm
from dlup.data.dataset import TiledROIsSlideImageDataset
from dlup.background import get_mask
from PIL import Image


# Load H&E slide through dlup
slide_image = dlup.SlideImage.from_file_path(HE_FILE_PATH)

# Generate foreground/background mask to not tile unnecessary area (this method is deprecated but works with dlup 0.3.27)
print("Generating mask (this takes a while)...")
tissue_mask = get_mask(slide_image)

# Create dlup dataset
dataset = TiledROIsSlideImageDataset.from_standard_tiling(HE_FILE_PATH, TARGET_MPP, TILE_SIZE, tile_overlap=(0, 0), mask=tissue_mask, mask_threshold=FOREGROUND_STRINGENCY)

# Open threshold mask file through pyvips for easy coordinate accesss
mask_image = pyvips.Image.new_from_file(MASK_FILE_PATH, access="sequential")

# Grab a tile produced by dlup, extract its coordinates and grab an identical tile from the mask using the coordinates
for i, d in tqdm(enumerate(dataset)):
    tile = d["image"]
    tifffile.imwrite(Path(OUTPUT_PATH, "he", f"{i}.tif"), np.array(tile))
    coords = np.array(d["coordinates"])
    mask_patch = mask_image.crop(coords[0], coords[1], TILE_SIZE[0], TILE_SIZE[1]).numpy()
    tifffile.imwrite(Path(OUTPUT_PATH, "masks", f"{i}.tif"), mask_patch)
