"""Require every generated Maven publication to expose the same immutable snapshot pin."""

import argparse
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path


REPOSITORY = "https://central.sonatype.com/repository/maven-snapshots"
POM_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def publications(directory, version):
    result = set()
    for path in Path(directory).glob("*/pom-default.xml"):
        pom = ET.parse(path).getroot()
        if pom.findtext("m:version", namespaces=POM_NAMESPACE) != version:
            raise ValueError(f"Unexpected publication version in {path}")
        group = pom.findtext("m:groupId", namespaces=POM_NAMESPACE)
        artifact = pom.findtext("m:artifactId", namespaces=POM_NAMESPACE)
        if not group or not artifact:
            raise ValueError(f"Missing publication coordinates in {path}")
        result.add((group, artifact))
    if not result:
        raise ValueError(f"No generated publications found in {directory}")
    return sorted(result)


def snapshot_pin(metadata, version):
    root = ET.fromstring(metadata)
    if root.findtext("version") != version:
        raise ValueError("Snapshot metadata has the wrong base version")
    entries = root.findall("versioning/snapshotVersions/snapshotVersion")
    pins = {entry.findtext("value") for entry in entries}
    extensions = {entry.findtext("extension") for entry in entries}
    if len(pins) != 1 or not {"pom", "module"} <= extensions:
        raise ValueError("Snapshot metadata is incomplete or contains inconsistent pins")
    pin = pins.pop()
    if not pin or not pin.startswith(version.removesuffix("SNAPSHOT")) or pin.endswith("SNAPSHOT"):
        raise ValueError(f"Invalid immutable snapshot pin: {pin}")
    return pin


def verify(version, coordinates, fetch):
    pins = {}
    for group, artifact in coordinates:
        url = f"{REPOSITORY}/{group.replace('.', '/')}/{artifact}/{version}/maven-metadata.xml"
        pins[artifact] = snapshot_pin(fetch(url), version)
    if len(set(pins.values())) != 1:
        raise ValueError(f"Publication pins disagree: {pins}")
    return next(iter(pins.values()))


def fetch(url):
    with urllib.request.urlopen(url, timeout=30) as response:
        return response.read()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("publications")
    args = parser.parse_args()
    coordinates = publications(args.publications, args.version)
    # Repository metadata can take a moment to become visible after the final upload.
    for attempt in range(6):
        try:
            print(verify(args.version, coordinates, fetch))
            return
        except (OSError, ValueError, ET.ParseError):
            if attempt == 5:
                raise
            time.sleep(5)


if __name__ == "__main__":
    main()
