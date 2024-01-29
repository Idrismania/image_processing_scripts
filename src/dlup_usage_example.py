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
from pathlib import Path

wsi = dlup.SlideImage.from_file_path(Path("data", "core_YB_b_14", "HE_2.ome.tiff"))
