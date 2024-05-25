"""This script generates and saves an overview of your tiling settings."""

import os

# Dynamically add pyvips bin path to PATH for ctypes libray to find it during import
basepath = os.path.abspath(os.path.join(os.path.dirname( __file__ ), "..\.."))
dllspath = os.path.join(basepath, "backends", "pyvips", "bin")
os.environ['PATH'] = dllspath + os.pathsep + os.environ['PATH']
print(dllspath)
with os.add_dll_directory(r"D:\Users\Horlings\ii_hh\image_processing_scripts\backends\openslide\bin\\"):
    import openslide

with os.add_dll_directory(r"D:\Users\Horlings\ii_hh\image_processing_scripts\backends\pyvips\bin\\"):
    import pyvips

import dlup
import matplotlib.pyplot as plt
import numpy as np
import tifffile
from dlup.data.dataset import TiledROIsSlideImageDataset
from dlup.background import get_mask
from pathlib import Path
from PIL import Image, ImageDraw
from tqdm import tqdm

# File paths of H&E and mask image to generate tiles from, as well as a reference to pyvips binary paths necessay for import
HE_FILE_PATH = Path(r"D:\Users\Horlings\ii_hh\FINAL_PROCESSED_IMAGE_DATA\d161fa98-0035-4c66-80f3-c02e11ff84ce_he.ome.tif")
MASK_FILE_PATH = Path(r"D:\Users\Horlings\ii_hh\FINAL_PROCESSED_IMAGE_DATA\d161fa98-0035-4c66-80f3-c02e11ff84ce_mask.ome.tif")  # Mask
PYVIPS_BIN_PATH = Path(r"D:\Users\Horlings\ii_hh\image_processing_scripts\backends\pyvips\bin\\")

OUTPUT_PATH = Path(r"D:\Users\Horlings\ii_hh\image_processing_scripts\data\wsi")

# Tiling parameters
TARGET_MPP = 0.2425 # 0.5
TILE_SIZE = (512, 512) # 512

with os.add_dll_directory(PYVIPS_BIN_PATH):
    import pyvips


# Load H&E slide through dlup
slide_image = dlup.SlideImage.from_file_path(HE_FILE_PATH)

# Generate foreground/background mask to not tile unnecessary area (this method is deprecated but works with dlup 0.3.27)
tissue_mask = get_mask(slide_image)

# Create dlup dataset
dataset = TiledROIsSlideImageDataset.from_standard_tiling(HE_FILE_PATH, TARGET_MPP, TILE_SIZE, (0, 0), mask=tissue_mask)

# Open threshold mask file through pyvips for easy coordinate accesss
mask_image = pyvips.Image.new_from_file(MASK_FILE_PATH, access="sequential")

# Grab a tile produced by dlup, extract its coordinates and grab an identical tile from the mask using the coordinates
for i, d in enumerate(dataset, start=0):
    tile = d["image"]
    tifffile.imwrite(Path(OUTPUT_PATH, "he", f"{i}.tif"), np.array(tile))
    coords = np.array(d["coordinates"])
    mask_patch = mask_image.crop(coords[0], coords[1], TILE_SIZE[0], TILE_SIZE[1]).numpy()
    tifffile.imwrite(Path(OUTPUT_PATH, "masks", f"{i}.tif"), mask_patch)
