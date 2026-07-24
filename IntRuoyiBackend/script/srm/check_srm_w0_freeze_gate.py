from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


WAVE_TO_FREEZE_PACKAGE = {
    "W1": "W0-01",
    "W2": "W0-02",
    "W3": "W0-03",
    "W4": "W0-04",
    "W5": "W0-05",
    "W0-06": "W0-06",
}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Check whether an SRM implementation wave may start from the W0 freeze pack."
    )
    parser.add_argument("--freeze-pack", required=True, help="Path to docs/srm/srm9-w0-freeze-pack.md")
    parser.add_argument("--wave", required=True, help="Wave to check: W0, W1, W2, W3, W4, W5, or W0-06")
    args = parser.parse_args(argv)

    freeze_pack = Path(args.freeze_pack)
    wave = args.wave.upper()

    if not freeze_pack.exists():
        print(f"Freeze pack not found: {freeze_pack}", file=sys.stderr)
        return 2

    text = freeze_pack.read_text(encoding="utf-8")

    if wave == "W0":
        print(f"W0 freeze pack readable: {freeze_pack}")
        return 0

    package = WAVE_TO_FREEZE_PACKAGE.get(wave)
    if package is None:
        print(f"Unsupported wave: {args.wave}", file=sys.stderr)
        return 2

    status = find_package_status(text, package)
    if status is None:
        print(f"Freeze package status not found: {package}", file=sys.stderr)
        return 2

    if status == "FROZEN":
        print(f"{wave} allowed: {package} is FROZEN")
        return 0

    print(f"{wave} blocked: {package} is {status}")
    return 1


def find_package_status(text: str, package: str) -> str | None:
    # Prefer the top summary table because it is the formal gate matrix.
    for line in text.splitlines():
        if package not in line:
            continue
        match = re.search(r"`(FROZEN|BLOCKED|DEFERRED)`", line)
        if match:
            return match.group(1)
    return None


if __name__ == "__main__":
    raise SystemExit(main())
