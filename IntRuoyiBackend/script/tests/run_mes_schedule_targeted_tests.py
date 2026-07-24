from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
PROFILE_ID = "mes-schedule-targeted-tests"
BASE_PACKAGE = "cn.iocoder.yudao.module.mes"
TARGET_TESTS = [
    "MesProScheduleOrderControllerTest",
    "MesProScheduleOrderRespVOContractTest",
    "MesProAutoScheduleAlgorithmContractTest",
    "MesProAutoScheduleContractTest",
    "MesProAutoScheduleServiceImplTest",
    "MesProNightlyReplanServiceImplTest",
    "MesProScheduleCalendarServiceImplTest",
    "MesProSchedulerWorkbenchFullConfigPackageServiceTest",
    "MesProSchedulerWorkbenchMapperXmlTest",
    "MesProSchedulerWorkbenchRouteConfigPackageServiceTest",
    "MesProSchedulerWorkbenchServiceImplTest",
    "MesProSchedulerWorkbenchSmokeTestServiceImplTest",
    "MesKingdeeProductionMaterialListQueryServiceImplTest",
    "MesProRouteScheduleConfigServiceTest",
]


def build_command() -> list[str]:
    mvn_executable = shutil.which("mvn.cmd") or shutil.which("mvn")
    if not mvn_executable:
        raise FileNotFoundError("Missing mvn.cmd/mvn in PATH")
    return [
        mvn_executable,
        "-pl",
        "yudao-module-mes",
        f"-P{PROFILE_ID}",
        f"-Dyudao.info.base-package={BASE_PACKAGE}",
        f"-Dtest={','.join(TARGET_TESTS)}",
        "-Dsurefire.failIfNoSpecifiedTests=true",
        "test",
    ]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Run MES schedule-targeted tests without pulling unrelated eDHR failures."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the Maven command only.",
    )
    args = parser.parse_args()

    command = build_command()
    print(" ".join(command))
    if args.dry_run:
        return 0

    completed = subprocess.run(
        command,
        cwd=REPO_ROOT,
        check=False,
    )
    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
