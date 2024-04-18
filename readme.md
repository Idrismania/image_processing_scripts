### Image Processing Scripts
This repository contains scripts and pipelines useful for generating data for CODEX/H&E overlaying protocols.
.groovy scripts are for QuPath scripting

All scripts can be found in the `scripts/` folder. To use PyVips and OpenSlide backends,
ensure that appropriate bin files for these are included in the follow structure at the project root:

<pre>
backends/
├─ openslide/
│  └─ bin/
│     ├─ filename.dll
│     └─ filename.dll
└─ pyvips/
   └─ bin/
      ├─ filename.dll
      └─ filename.dll
</pre>
Backends can be downloaded at:
- Openslide:   https://openslide.org/download/
- PyVips:      https://github.com/libvips/libvips/releases
