"""This script loads a specified region of any tiled tiff image. This serves as a test for a potential PyTorch dataloader."""

import os
import sys
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

# Dynamically add pyvips bin path to PATH for ctypes libray to find it during import
basepath = os.path.abspath(os.path.join(os.path.dirname( __file__ ), ".."))
dllspath = os.path.join(basepath, "backends", "pyvips", "bin")
os.environ['PATH'] = dllspath + os.pathsep + os.environ['PATH']

with os.add_dll_directory(r"D:\Users\Horlings\ii_hh\image_processing_scripts\backends\openslide\bin\\"):
    import openslide

with os.add_dll_directory(r"D:\Users\Horlings\ii_hh\image_processing_scripts\backends\pyvips\bin\\"):
    import pyvips

# ----- EVERYTHING ABOVE IS IMPORTS -----

# PARAMETERS (PyVips offset is top-left based)
CHANNEL = 0
ALL_CHANNELS = True # If true, ignored selected channel and plots every channel in a grid
OFFSET_X = 0
OFFSET_Y = 0
TILE_SIZE = 1024
IMAGE_PATH = Path(r"D:\Users\Horlings\ii_hh\pretty_codex_qupath_project\mask\prediction_image_14.ome.tif")  # Mask
# IMAGE_PATH = Path(r"D:\Users\Horlings\ii_hh\T19-13744_qupath_project\T19_13744_registered.ome.tif")         # CODEX 16-bit
image = pyvips.Image.new_from_file(IMAGE_PATH, access="sequential")
patch = image.crop(OFFSET_X, OFFSET_Y, TILE_SIZE, TILE_SIZE).numpy()



n_channels = patch.shape[2]

fig, axs = plt.subplots(3,9)
axs = axs.flatten()



for i, ax in enumerate(axs):
    if i >= patch.shape[2]:
        ax.imshow(np.zeros((TILE_SIZE, TILE_SIZE), dtype=np.uint8))
        ax.set_title("Empty")
    else:
        ax.imshow(patch[:, :, i])

plt.show()