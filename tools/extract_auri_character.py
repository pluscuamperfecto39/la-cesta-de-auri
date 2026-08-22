"""Create the transparent Auri character asset from the supplied reference image."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image
from rembg import new_session, remove


def extract_character(source_path: Path, output_path: Path, model: str) -> None:
    source = Image.open(source_path).convert("RGBA")
    width, height = source.size

    # The caption starts below the character. Cropping it away first keeps the
    # text out of the segmentation input and leaves a small safety margin.
    crop_box = (
        round(width * 0.085),
        round(height * 0.075),
        round(width * 0.915),
        round(height * 0.875),
    )
    character_area = source.crop(crop_box)

    cutout = remove(
        character_area,
        session=new_session(model),
        post_process_mask=True,
    ).convert("RGBA")

    alpha = cutout.getchannel("A")
    bounds = alpha.point(lambda value: 255 if value > 8 else 0).getbbox()
    if bounds is None:
        raise RuntimeError("The background-removal model returned an empty image.")

    cutout = cutout.crop(bounds)
    padding = max(12, round(max(cutout.size) * 0.035))
    canvas = Image.new(
        "RGBA",
        (cutout.width + padding * 2, cutout.height + padding * 2),
        (0, 0, 0, 0),
    )
    canvas.alpha_composite(cutout, (padding, padding))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output_path, optimize=True)
    print(f"Saved {output_path} ({canvas.width}x{canvas.height})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--model", default="isnet-general-use")
    args = parser.parse_args()
    extract_character(args.source, args.output, args.model)


if __name__ == "__main__":
    main()
