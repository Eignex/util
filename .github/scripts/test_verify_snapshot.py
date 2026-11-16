import tempfile
import unittest
from pathlib import Path

from verify_snapshot import publications, snapshot_pin, verify


VERSION = "0.0.1-ci.123.1-SNAPSHOT"
PIN = "0.0.1-ci.123.1-20261116.120023-1"


def metadata(pin=PIN, extensions=("pom", "module", "klib")):
    entries = "".join(
        f"<snapshotVersion><extension>{extension}</extension><value>{pin}</value></snapshotVersion>"
        for extension in extensions
    )
    return (
        f"<metadata><version>{VERSION}</version><versioning><snapshotVersions>"
        f"{entries}</snapshotVersions></versioning></metadata>"
    )


class VerifySnapshotTest(unittest.TestCase):
    def test_all_targets_share_one_pin(self):
        coordinates = [("com.eignex", artifact) for artifact in ("util", "util-jvm", "util-iosarm64")]
        self.assertEqual(PIN, verify(VERSION, coordinates, lambda url: metadata()))

    def test_different_platform_build_numbers_fail(self):
        def fetch(url):
            return metadata(PIN[:-1] + "3" if "/util-iosarm64/" in url else PIN)

        with self.assertRaisesRegex(ValueError, "Publication pins disagree"):
            verify(VERSION, [("com.eignex", "util"), ("com.eignex", "util-iosarm64")], fetch)

    def test_missing_module_metadata_fails(self):
        with self.assertRaisesRegex(ValueError, "incomplete"):
            snapshot_pin(metadata(extensions=("pom", "klib")), VERSION)

    def test_different_base_version_fails(self):
        with self.assertRaisesRegex(ValueError, "wrong base version"):
            snapshot_pin(metadata(), "0.0.1-ci.123.2-SNAPSHOT")

    def test_publications_come_from_generated_poms(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "iosArm64" / "pom-default.xml"
            path.parent.mkdir()
            path.write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0">'
                f"<groupId>com.eignex</groupId><artifactId>util-iosarm64</artifactId><version>{VERSION}</version>"
                "</project>"
            )
            self.assertEqual([("com.eignex", "util-iosarm64")], publications(directory, VERSION))
            with self.assertRaisesRegex(ValueError, "Unexpected publication version"):
                publications(directory, "0.0.1-ci.123.2-SNAPSHOT")

    def test_missing_publications_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "No generated publications"):
                publications(directory, VERSION)


if __name__ == "__main__":
    unittest.main()
