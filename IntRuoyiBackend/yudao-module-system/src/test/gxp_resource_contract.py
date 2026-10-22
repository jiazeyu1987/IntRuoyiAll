"""Static resource RED; optional fresh exploded artifact verification (no Maven)."""
import argparse
import hashlib
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

MODULE = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class ResourceContract(unittest.TestCase):
    def test_standard_and_gxp_resource_roots_are_both_packaged(self):
        root = ET.parse(MODULE / "pom.xml").getroot()
        resources = root.findall("m:build/m:resources/m:resource", NS)
        directories = [r.findtext("m:directory", namespaces=NS) for r in resources]
        self.assertTrue(any(d in ("src/main/resources", "${project.basedir}/src/main/resources")
                            for d in directories), "standard captcha images and SPI resource root omitted")
        gxp = [r for r in resources if r.findtext("m:targetPath", namespaces=NS) == "META-INF/gxp"]
        self.assertEqual(1, len(gxp))
        self.assertEqual("${project.basedir}/../config", gxp[0].findtext("m:directory", namespaces=NS))
        includes = {e.text for e in gxp[0].findall("m:includes/m:include", NS)}
        self.assertTrue({"gxp-audit-policy.yaml", "gxp-audit-policy.schema.json"} <= includes)


def verify_artifact(directory):
    directory = directory.resolve(strict=True)
    sources = MODULE / "src/main/resources"
    expected = [(p, p.relative_to(sources)) for p in (sources / "images").rglob("*") if p.is_file()]
    for spi in ("CaptchaCacheService", "CaptchaService"):
        relative = Path("META-INF/services/com.anji.captcha.service." + spi)
        expected.append((sources / relative, relative))
    for name in ("gxp-audit-policy.yaml", "gxp-audit-policy.schema.json"):
        expected.append((MODULE.parent / "config" / name, Path("META-INF/gxp") / name))
    for source, relative in expected:
        actual = directory / relative
        assert actual.is_file(), f"missing artifact resource: {actual}"
        digest = hashlib.sha256(actual.read_bytes()).hexdigest()
        assert digest == hashlib.sha256(source.read_bytes()).hexdigest(), f"resource differs: {actual}"
        print(f"PASS {actual} sha256={digest}")
    print(f"Verified {len(expected)} resources; clean build provenance must be supplied by main agent.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact-dir", type=Path)
    args = parser.parse_args()
    if args.artifact_dir:
        verify_artifact(args.artifact_dir)
    else:
        unittest.main(argv=[__file__])
