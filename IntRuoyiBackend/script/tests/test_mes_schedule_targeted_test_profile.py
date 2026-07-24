from __future__ import annotations

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MODULE_POM = REPO_ROOT / "yudao-module-mes" / "pom.xml"
RUNNER = REPO_ROOT / "script" / "tests" / "run_mes_schedule_targeted_tests.py"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
PROFILE_ID = "mes-schedule-targeted-tests"
BASE_PACKAGE = "cn.iocoder.yudao.module.mes"
TARGET_TEST_PATTERNS = [
    "**/MesProScheduleOrderControllerTest.java",
    "**/MesProScheduleOrderRespVOContractTest.java",
    "**/MesProAutoScheduleAlgorithmContractTest.java",
    "**/MesProAutoScheduleContractTest.java",
    "**/MesProAutoScheduleServiceImplTest.java",
    "**/MesProNightlyReplanServiceImplTest.java",
    "**/MesProScheduleCalendarServiceImplTest.java",
    "**/MesProSchedulerWorkbenchFullConfigPackageServiceTest.java",
    "**/MesProSchedulerWorkbenchMapperXmlTest.java",
    "**/MesProSchedulerWorkbenchRouteConfigPackageServiceTest.java",
    "**/MesProSchedulerWorkbenchServiceImplTest.java",
    "**/MesProSchedulerWorkbenchSmokeTestServiceImplTest.java",
    "**/MesKingdeeProductionMaterialListQueryServiceImplTest.java",
    "**/MesProRouteScheduleConfigServiceTest.java",
]
TARGET_TEST_NAMES = [
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
EXCLUDE_PATTERNS = [
    "**/*Edhr*.java",
    "**/approval/**",
    "**/batchrecord/**",
    "**/job/batchrecord/**",
]


def _find_profile() -> ET.Element:
    root = ET.fromstring(MODULE_POM.read_text(encoding="utf-8"))
    for profile in root.findall("./m:profiles/m:profile", NS):
        if profile.findtext("m:id", namespaces=NS) == PROFILE_ID:
            return profile
    raise AssertionError(f"missing Maven profile {PROFILE_ID!r} in {MODULE_POM}")


def _find_plugin(profile: ET.Element, artifact_id: str) -> ET.Element:
    for plugin in profile.findall("./m:build/m:plugins/m:plugin", NS):
        if plugin.findtext("m:artifactId", namespaces=NS) == artifact_id:
            return plugin
    raise AssertionError(f"missing plugin {artifact_id!r} in profile {PROFILE_ID!r}")


def _read_text_list(parent: ET.Element, xpath: str) -> list[str]:
    return [element.text for element in parent.findall(xpath, NS) if element.text]


def test_mes_schedule_targeted_profile_locks_compiler_and_surefire_scope() -> None:
    profile = _find_profile()
    compiler_plugin = _find_plugin(profile, "maven-compiler-plugin")
    surefire_plugin = _find_plugin(profile, "maven-surefire-plugin")

    compiler_includes = _read_text_list(
        compiler_plugin, "./m:configuration/m:testIncludes/m:testInclude"
    )
    compiler_excludes = _read_text_list(
        compiler_plugin, "./m:configuration/m:testExcludes/m:testExclude"
    )
    surefire_includes = _read_text_list(
        surefire_plugin, "./m:configuration/m:includes/m:include"
    )
    surefire_excludes = _read_text_list(
        surefire_plugin, "./m:configuration/m:excludes/m:exclude"
    )

    assert compiler_includes == TARGET_TEST_PATTERNS
    assert surefire_includes == TARGET_TEST_PATTERNS
    assert compiler_excludes == EXCLUDE_PATTERNS
    assert surefire_excludes == EXCLUDE_PATTERNS
    assert (
        surefire_plugin.findtext(
            "./m:configuration/m:systemPropertyVariables/m:yudao.info.base-package",
            namespaces=NS,
        )
        == BASE_PACKAGE
    )


def test_mes_schedule_targeted_runner_uses_profile_and_exact_target_tests() -> None:
    assert RUNNER.exists(), f"missing runner script: {RUNNER}"

    completed = subprocess.run(
        [sys.executable, "-X", "utf8", str(RUNNER), "--dry-run"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=False,
    )

    assert completed.returncode == 0, completed.stderr or completed.stdout
    output = completed.stdout
    assert f"-P{PROFILE_ID}" in output
    assert f"-Dyudao.info.base-package={BASE_PACKAGE}" in output
    for test_name in TARGET_TEST_NAMES:
        assert test_name in output, test_name
