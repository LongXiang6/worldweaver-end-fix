"""Apply two compatibility fixes to WorldWeaver: New Dawn 21.0.31."""

import argparse
from hashlib import file_digest
import os
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parent
EXPECTED = "2800b8f3ba843e8cd24074d49f3d0c32a98a08c38542a47fddf9b7381f5b9cda"
PATCHES = {
    "PatchWorldWeaverEnd.java": "org/betterx/wover/generator/mixin/biomesource/TheEndBiomeSourceMixin.class",
    "PatchWorldWeaverSave.java": "org/betterx/wover/state/impl/WorldConfigImpl.class",
}


def build(args):
    with args.original.open("rb") as stream:
        if file_digest(stream, "sha256").hexdigest() != EXPECTED:
            raise ValueError("Expected the published WorldWeaver: New Dawn 21.0.31 JAR")
    with TemporaryDirectory() as directory, ZipFile(args.original) as source:
        stage = Path(directory)
        patch_data = {}
        for program, entry in PATCHES.items():
            original = stage / f"{program}.original"
            changed = stage / f"{program}.patched"
            original.write_bytes(source.read(entry))
            subprocess.run(
                [args.java, "-cp", os.pathsep.join(map(str, (args.asm, args.asm_tree))),
                 ROOT / "src" / program, original, changed],
                check=True,
            )
            patch_data[entry] = changed.read_bytes()
        resources = ROOT / "resources"
        for file in resources.rglob("*"):
            if file.is_file():
                patch_data[file.relative_to(resources).as_posix()] = file.read_bytes()
        with ZipFile(args.output, "x") as result:
            for info in source.infolist():
                changed = patch_data.pop(info.filename, None)
                result.writestr(info, source.read(info) if changed is None else changed)
            for name, content in sorted(patch_data.items()):
                result.writestr(name, content)
            result.write(ROOT / "LICENSE", "META-INF/LICENSE-worldweaver-end-fix.txt")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("original", type=Path)
    parser.add_argument("--asm", type=Path, required=True)
    parser.add_argument("--asm-tree", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--java", default="java")
    build(parser.parse_args())
