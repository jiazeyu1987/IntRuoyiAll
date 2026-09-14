"""Dependency-free runner for the flow-repair-11 contract tests."""

from __future__ import annotations

import importlib.util
from pathlib import Path


def main() -> int:
    script_dir = Path(__file__).resolve().parent
    test_path = script_dir / "tests" / "test_flow_repair_11_migration.py"
    spec = importlib.util.spec_from_file_location("flow_repair_11_contract_tests", test_path)
    if spec is None or spec.loader is None:
        raise RuntimeError("FLOW11_TEST_MODULE_UNAVAILABLE")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    names = sorted(name for name in dir(module) if name.startswith("test_"))
    for name in names:
        getattr(module, name)()
    print(f"PASS flow11_contract_runner {len(names)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
