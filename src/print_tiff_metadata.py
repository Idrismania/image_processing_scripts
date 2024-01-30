"""Run script with input file argument to print metadata"""

import argparse
import tifffile

def main() -> None:

    parser = argparse.ArgumentParser(
            description="File path parser",
            epilog="Example: src/print_tiff_metadata.py input.tiff")

    parser.add_argument("input", help="Path to the first input image")

    args = parser.parse_args()


    file_path = args.input

    # Open the TIFF file and read its metadata
    with tifffile.TiffFile(file_path) as tif:

        # Metadata is stored in tags
        for page in tif.pages:

            # Access tags for each page
            tags = page.tags

            # Print metadata for each page
            print(f"Metadata for page {page.index}:")
            
            for tag in tags.values():
                print(tag)

if __name__ == "__main__":
    main()
