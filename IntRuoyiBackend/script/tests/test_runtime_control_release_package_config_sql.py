from pathlib import Path


SQL = Path("sql/mysql/20260613_runtime_control_release_package_config.sql").read_text(encoding="utf-8")

BACKEND_RUNTIME_BASE_KEYS = [
    "runtime-control.release-package.backend-runtime-base-mode",
    "runtime-control.release-package.backend-runtime-base-tar-path",
    "runtime-control.release-package.backend-runtime-base-tar-sha256",
    "runtime-control.release-package.backend-runtime-base-image",
    "runtime-control.release-package.backend-runtime-base-digest",
    "runtime-control.release-package.backend-runtime-base-version",
]


def test_release_package_config_seed_is_idempotent():
    assert "INSERT INTO `infra_config`" in SQL
    assert "WHERE NOT EXISTS" in SQL
    assert "`config_key` = seed.`config_key`" in SQL
    assert "ON DUPLICATE KEY UPDATE" not in SQL


def test_release_package_config_seed_defines_backend_runtime_base_values():
    for config_key in BACKEND_RUNTIME_BASE_KEYS:
        assert config_key in SQL

    assert "'offline-tar'" in SQL
    assert "D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar" in SQL
    assert "5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603" in SQL
    assert "intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1" in SQL
    assert "sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7" in SQL
    assert "b'1'" in SQL
