"""This script generates and saves an overview of your tiling settings."""

import os

with os.add_dll_directory(r"C:\Users\idris\PycharmProjects\ChangeGamersScripts\backends\openslide\bin"):
    import openslide

with os.add_dll_directory(r"C:\Users\idris\PycharmProjects\ChangeGamersScripts\backends\pyvips\bin"):
    import pyvips

import dlup
import matplotlib.pyplot as plt
import numpy as np
from dlup.data.dataset import TiledROIsSlideImageDataset
from dlup.background import get_mask
from pathlib import Path
from PIL import Image, ImageDraw


INPUT_FILE_PATH = Path("data", "core_YB_b_14_pyramidal", "C00_HE.ome.tiff")
TARGET_MPP = 0.5
TILE_SIZE = (512, 512)

slide_image = dlup.SlideImage.from_file_path(INPUT_FILE_PATH)

mask = get_mask(slide_image)
scaled_region_view = slide_image.get_scaled_view(slide_image.get_scaling(TARGET_MPP))

dataset = TiledROIsSlideImageDataset.from_standard_tiling(INPUT_FILE_PATH, TARGET_MPP, TILE_SIZE, (0, 0), mask=mask)
output = Image.new("RGBA", tuple(scaled_region_view.size), (255, 255, 255, 255))


for d in dataset:
    tile = d["image"]
    coords = np.array(d["coordinates"])
    box = tuple(np.array((*coords, *(coords + TILE_SIZE))).astype(int))
    output.paste(tile, box)
    #draw = ImageDraw.Draw(output)
    #draw.rectangle(box, outline="white", width=5)

output.save(Path("output", "tiling_example_no_line.png"))