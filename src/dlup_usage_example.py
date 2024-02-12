"""Some requirements to use dlup in Windows:
IN ORDER: download binaries for openslide and pyvips and add them with os.add_dll_directory.
pip install pyvips, openslide-python and dlup.
Finally, import dlup only after importing openslide and pyvips through dll.
Note that paths to dll directories have to be absolute paths

Pyvips:     https://github.com/libvips/build-win64-mxe/releases/download/v8.15.1/vips-dev-w64-all-8.15.1.zip
Openslide:  https://github.com/openslide/openslide-bin/releases/download/v20231011/openslide-win64-20231011.zip"""

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


INPUT_PATH = Path("data", "core_YB_b_14_pyramidal", "C00_HE.ome.tiff")
SLIDE_PATH = Path("output", "merged_masks_pyramidal.ome.tiff")
TARGET_MPP = 0.4990
TILE_SIZE = (512, 512)

slide_image = dlup.SlideImage.from_file_path(INPUT_PATH)

mask = get_mask(slide_image)
scaled_region_view = slide_image.get_scaled_view(slide_image.get_scaling(TARGET_MPP))
overview = Image.new("RGBA", tuple(scaled_region_view.size), (255, 255, 255, 255))


dataset = TiledROIsSlideImageDataset.from_standard_tiling(
    path=INPUT_PATH,
    mpp=TARGET_MPP,
    tile_size=TILE_SIZE,
    tile_overlap=(0, 0),
    mask=mask,
    annotations=dlup.SlideImage.from_file_path(SLIDE_PATH).read_region
)


for data in dataset:
    tile = data["image"]

    plt.subplot(1, 2, 1)
    plt.imshow(tile)
    plt.subplot(1, 2, 2)
    plt.imshow(data["annotations"])
    plt.show()

    coords = np.array(data["coordinates"])
    box = tuple(np.array((*coords, *(coords + TILE_SIZE))).astype(int))

    print(data)

    # Draw boxes to the overview
    overview.paste(tile, box)
    draw = ImageDraw.Draw(overview)
    draw.rectangle(box, outline="red", width=8)

