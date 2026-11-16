"""Prevent production dependencies from leaking back into the equipment build."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class EquipmentBoundaryTest(unittest.TestCase):
    def test_equipment_reactor_does_not_require_mes(self):
        graph = {}
        pending = [ROOT / "pom.xml"]
        while pending:
            pom = pending.pop()
            doc = ET.parse(pom).getroot()
            pending.extend(pom.parent / item.text / "pom.xml"
                           for item in doc.findall("m:modules/m:module", NS))
            name = doc.findtext("m:artifactId", namespaces=NS)
            graph[name] = [d.findtext("m:artifactId", namespaces=NS)
                           for d in doc.findall("m:dependencies/m:dependency", NS)]
        visited = set()
        def visit(name):
            if name in visited:
                return
            visited.add(name)
            for dependency in graph.get(name, []):
                visit(dependency)
        visit("yudao-module-mes-device")
        self.assertNotIn("yudao-module-mes", visited)
        self.assertIn("yudao-module-mes-common", visited)

    def test_equipment_and_common_have_no_production_source_imports(self):
        owners = {}
        for module in ["mes", "mes-common", "mes-device"]:
            source = ROOT / ("yudao-module-" + module) / "src/main/java"
            for java in source.rglob("*.java"):
                name = ".".join(java.relative_to(source).with_suffix("").parts)
                self.assertNotIn(name, owners, "Duplicated Java class: " + name)
                owners[name] = module
        for module in ["mes-common", "mes-device"]:
            for java in (ROOT / ("yudao-module-" + module) / "src/main/java").rglob("*.java"):
                for name in re.findall(r"cn\.iocoder\.yudao\.module\.mes\.[\w.]+", java.read_text(encoding="utf8")):
                    while name and name not in owners:
                        name = name.rpartition(".")[0]
                    if name:
                        self.assertNotEqual(owners[name], "mes", str(java) + " imports " + name)
                        if module == "mes-common":
                            self.assertEqual(owners[name], "mes-common", str(java) + " imports " + name)


if __name__ == "__main__":
    unittest.main()
