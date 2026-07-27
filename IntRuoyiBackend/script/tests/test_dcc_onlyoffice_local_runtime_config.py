from __future__ import annotations

import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
EXPECTED_LOCAL_ONLYOFFICE_URL = "http://127.0.0.1:8080"
EXPECTED_LOCAL_ONLYOFFICE_PUBLIC_FILE_URL = "http://host.docker.internal:${server.port}"
STALE_ONLYOFFICE_URL = "http://127.0.0.1:8082"
STALE_ONLYOFFICE_PUBLIC_FILE_URL = "http://127.0.0.1:${server.port}"


def read_text(relative_path: str) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


def onlyoffice_base_url_default(yaml_text: str) -> str:
    match = re.search(r"base-url:\s*\$\{DCC_ONLYOFFICE_BASE_URL:([^}]+)\}", yaml_text)
    assert match, "DCC_ONLYOFFICE_BASE_URL placeholder must be present"
    return match.group(1)


def onlyoffice_public_file_base_url_default(yaml_text: str) -> str:
    match = re.search(r"public-file-base-url:\s*\$\{DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL:([^}]+)\}", yaml_text)
    assert match, "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL placeholder must be present"
    return match.group(1)


def test_local_and_dev_onlyoffice_defaults_match_local_document_server_port() -> None:
    for relative_path in (
        "yudao-server/src/main/resources/application-local.yaml",
        "yudao-server/src/main/resources/application-dev.yaml",
    ):
        text = read_text(relative_path)
        assert onlyoffice_base_url_default(text) == EXPECTED_LOCAL_ONLYOFFICE_URL
        assert STALE_ONLYOFFICE_URL not in text


def test_local_onlyoffice_public_file_default_is_reachable_from_docker_document_server() -> None:
    text = read_text("yudao-server/src/main/resources/application-local.yaml")
    assert onlyoffice_public_file_base_url_default(text) == EXPECTED_LOCAL_ONLYOFFICE_PUBLIC_FILE_URL
    assert STALE_ONLYOFFICE_PUBLIC_FILE_URL not in text


def test_local_restart_script_and_yaml_use_the_same_onlyoffice_base_url() -> None:
    restart_script = read_text("script/deploy/restart-int-ruoyi-local.ps1")
    assert f"$OnlyOfficeBaseUrl = '{EXPECTED_LOCAL_ONLYOFFICE_URL}'" in restart_script
