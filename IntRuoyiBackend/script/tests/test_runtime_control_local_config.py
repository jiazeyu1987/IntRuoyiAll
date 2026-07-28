from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
LOCAL_CONFIG = REPO_ROOT / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml"


def test_local_runtime_control_log_dir_matches_spring_log_root():
    text = LOCAL_CONFIG.read_text(encoding="utf-8")

    runtime_log_dir = "../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs"

    assert (
        "name: ${INTRUOYI_BACKEND_LOG_FILE:${INTRUOYI_RUNTIME_LOG_DIR:"
        f"{runtime_log_dir}" + "}/${spring.application.name}.log}"
    ) in text
    assert "storage-guard:" in text
    assert (
        "log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${INTRUOYI_RUNTIME_LOG_DIR:"
        f"{runtime_log_dir}" + "}}"
    ) in text
