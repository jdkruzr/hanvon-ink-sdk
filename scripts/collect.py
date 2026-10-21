#!/usr/bin/env python3
"""Read debug-app recordings using run-as; preserve every capture in a new directory."""
import argparse
import datetime
import json
from pathlib import Path
import subprocess
import tarfile

parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
parser.add_argument("--output", type=Path, default=Path("recordings"))
args = parser.parse_args()
destination = args.output / (datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ") + "-" + args.serial)
destination.mkdir(parents=True, exist_ok=False)
adb = ["adb", "-s", args.serial]
archive = destination / "app-files.tar"
with archive.open("wb") as output:
    subprocess.run(adb + ["exec-out", "run-as", "dev.hanvon.ink.demo", "tar", "-cf", "-", "files"], stdout=output, check=True)
with tarfile.open(archive) as contents:
    contents.extractall(destination, filter="data")
for name, command in {
    "package.txt": ["shell", "dumpsys", "package", "dev.hanvon.ink.demo"],
    "logcat.txt": ["shell", "logcat", "-d", "-s", "HanvonInkLab:I", "AndroidRuntime:E", "wwn:D"],
}.items():
    result = subprocess.run(adb + command, capture_output=True, check=True)
    (destination / name).write_bytes(result.stdout)
print(destination)
