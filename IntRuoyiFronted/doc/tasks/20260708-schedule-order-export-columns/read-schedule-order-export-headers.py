import json
import re
import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree


NS = {
    "main": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "rel": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "pkgrel": "http://schemas.openxmlformats.org/package/2006/relationships",
}


def col_index(cell_ref: str) -> int:
    letters = re.match(r"[A-Z]+", cell_ref or "")
    value = 0
    for char in letters.group(0) if letters else "":
        value = value * 26 + ord(char) - ord("A") + 1
    return value - 1


def text_of(element) -> str:
    return "".join(element.itertext()) if element is not None else ""


def read_headers(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        workbook = ElementTree.fromstring(archive.read("xl/workbook.xml"))
        rels = ElementTree.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        first_sheet = workbook.find("main:sheets/main:sheet", NS)
        if first_sheet is None:
            raise AssertionError("workbook has no sheet")
        rel_id = first_sheet.attrib[f"{{{NS['rel']}}}id"]
        target = None
        for rel in rels.findall("pkgrel:Relationship", NS):
            if rel.attrib.get("Id") == rel_id:
                target = rel.attrib.get("Target")
                break
        if not target:
            raise AssertionError(f"missing relationship for {rel_id}")
        sheet_path = "xl/" + target.lstrip("/").removeprefix("xl/")
        if sheet_path not in names:
            raise AssertionError(f"missing sheet xml: {sheet_path}")

        shared_strings = []
        if "xl/sharedStrings.xml" in names:
            shared_root = ElementTree.fromstring(archive.read("xl/sharedStrings.xml"))
            shared_strings = [text_of(item) for item in shared_root.findall("main:si", NS)]

        sheet = ElementTree.fromstring(archive.read(sheet_path))
        first_row = sheet.find(".//main:sheetData/main:row[@r='1']", NS)
        if first_row is None:
            raise AssertionError("sheet has no header row")
        cells = []
        for cell in first_row.findall("main:c", NS):
            ref = cell.attrib.get("r", "")
            value = cell.find("main:v", NS)
            inline = cell.find("main:is", NS)
            if cell.attrib.get("t") == "s":
                text = shared_strings[int(value.text)] if value is not None and value.text else ""
            elif inline is not None:
                text = text_of(inline)
            else:
                text = value.text if value is not None and value.text is not None else ""
            cells.append((col_index(ref), text))
        headers = [text for _, text in sorted(cells)]
        row_count = len(sheet.findall(".//main:sheetData/main:row", NS))
        return {
            "path": str(path),
            "is_zip": True,
            "entries": sorted(names)[:20],
            "headers": headers,
            "rowCount": row_count,
        }


if __name__ == "__main__":
    result = read_headers(Path(sys.argv[1]))
    print(json.dumps(result, ensure_ascii=False, indent=2))
