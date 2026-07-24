from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_utf8(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def java_main_files():
    return list((REPO_ROOT / "yudao-module-infra/src/main/java").rglob("*.java"))


def mysql_files():
    return list((REPO_ROOT / "sql/mysql").glob("*.sql"))


def test_runtime_nightly_release_backend_entrypoints_are_removed():
    forbidden_paths = [
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/job/runtimecontrol/RuntimeNightlyReleaseJob.java",
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeNightlyReleaseService.java",
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeNightlyReleaseServiceImpl.java",
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeNightlyReleaseRunResult.java",
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/runtimecontrol/vo/RuntimeNightlyReleaseStatusRespVO.java",
        REPO_ROOT
        / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/runtimecontrol/vo/RuntimeNightlyReleaseRunRespVO.java",
    ]

    existing = [str(path.relative_to(REPO_ROOT)) for path in forbidden_paths if path.exists()]
    assert existing == [], f"nightly release production entrypoints must be removed: {existing}"

    production_source = "\n".join(read_utf8(path) for path in java_main_files())
    for forbidden in (
        "runtimeNightlyReleaseJob",
        "RuntimeNightlyReleaseService",
        "RuntimeNightlyReleaseStatusRespVO",
        "/nightly-release",
        "夜间定时发布",
    ):
        assert forbidden not in production_source, f"nightly release backend wiring remains: {forbidden}"


def test_runtime_nightly_release_sql_only_disables_existing_job():
    forbidden_sql = REPO_ROOT / "sql/mysql/20260612_runtime_nightly_release_job.sql"
    assert not forbidden_sql.exists(), "nightly release infra_job seed SQL must be removed"

    allowed_disable_sql = REPO_ROOT / "sql/mysql/20260614_disable_runtime_nightly_release_job.sql"
    assert allowed_disable_sql.exists(), "existing environments need an idempotent disable SQL"

    for path in mysql_files():
        sql = read_utf8(path)
        if "runtimeNightlyReleaseJob" not in sql:
            continue
        assert path == allowed_disable_sql, f"unexpected nightly release SQL reference: {path}"
        assert "INSERT INTO `infra_job`" not in sql
        assert "0 0 2 * * ?" not in sql
        assert "`deleted` = b'1'" in sql
        assert "`information_schema`.`TABLES`" in sql
        assert "QRTZ_CRON_TRIGGERS" in sql
        assert "QRTZ_TRIGGERS" in sql
        assert "QRTZ_JOB_DETAILS" in sql
        assert "PREPARE runtime_nightly_release_stmt" in sql
