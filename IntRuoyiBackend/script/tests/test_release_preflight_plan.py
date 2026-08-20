import json
import hashlib

from script.release.release_preflight_plan import build_preflight_plan, main


def migration(**overrides: object) -> dict[str, object]:
    base: dict[str, object] = {
        "migrationId": "m1",
        "file": "sql/mysql/m1.sql",
        "sha256": "a" * 64,
        "type": "schema",
        "allowedEnvironments": ["test", "backup"],
        "dependsOn": [],
    }
    base.update(overrides)
    return base


def test_preflight_skips_already_applied_checksum_match() -> None:
    plan = build_preflight_plan(
        [migration()],
        {"m1": {"sha256": "a" * 64, "status": "APPLIED"}},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "SKIP_ALREADY_APPLIED"


def test_preflight_reapplies_checksum_mismatch() -> None:
    plan = build_preflight_plan(
        [migration()],
        {"m1": {"sha256": "b" * 64, "status": "APPLIED"}},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "APPLY"


def test_preflight_skips_environment_not_allowed() -> None:
    plan = build_preflight_plan([migration()], {}, target_environment="prod", publish_scope="with-data")

    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "SKIP_ENV_NOT_ALLOWED"
    assert "not allowed" in plan["items"][0]["reason"]


def test_preflight_blocks_dependency_when_parent_is_skipped_for_environment() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="parent"),
            migration(
                migrationId="child",
                allowedEnvironments=["prod"],
                dependsOn=["parent"],
            ),
        ],
        {},
        target_environment="prod",
        publish_scope="with-data",
    )

    assert plan["status"] == "blocked"
    assert [item["action"] for item in plan["items"]] == [
        "SKIP_ENV_NOT_ALLOWED",
        "BLOCKED_DEPENDENCY_MISSING",
    ]
    assert "skipped for environment" in plan["items"][1]["reason"]


def test_preflight_blocks_missing_dependency() -> None:
    plan = build_preflight_plan(
        [migration(migrationId="child", dependsOn=["parent"])],
        {},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "blocked"
    assert plan["items"][0]["action"] == "BLOCKED_DEPENDENCY_MISSING"


def test_preflight_accepts_dependency_applied_earlier_in_same_plan() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="parent"),
            migration(migrationId="child", dependsOn=["parent"]),
        ],
        {},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert [item["action"] for item in plan["items"]] == ["APPLY", "APPLY"]


def test_preflight_accepts_dependency_planned_later_in_same_plan() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="child", dependsOn=["parent"]),
            migration(migrationId="parent"),
        ],
        {},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert [item["migrationId"] for item in plan["items"]] == ["parent", "child"]
    assert [item["action"] for item in plan["items"]] == ["APPLY", "APPLY"]


def test_code_only_blocks_transitive_pending_data_dependency_with_path() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="data-root", type="data"),
            migration(migrationId="menu-bridge", type="menu", dependsOn=["data-root"]),
            migration(migrationId="schema-child", dependsOn=["menu-bridge"]),
        ],
        {},
        target_environment="test",
        publish_scope="code-only",
    )

    assert plan["status"] == "blocked"
    assert [item["action"] for item in plan["items"]] == [
        "SKIP_SCOPE_EXCLUDED",
        "BLOCKED_SCOPE_DEPENDENCY",
        "BLOCKED_SCOPE_DEPENDENCY",
    ]
    assert "schema-child -> menu-bridge -> data-root" in plan["items"][2]["reason"]


def test_code_only_accepts_data_dependency_already_applied_with_matching_checksum() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="data-root", type="data"),
            migration(migrationId="schema-child", dependsOn=["data-root"]),
        ],
        {"data-root": {"sha256": "a" * 64, "status": "APPLIED"}},
        target_environment="test",
        publish_scope="code-only",
    )

    assert plan["status"] == "passed"
    assert [item["action"] for item in plan["items"]] == ["SKIP_ALREADY_APPLIED", "APPLY"]


def test_code_only_blocks_data_dependency_when_applied_checksum_drift_requires_reapply() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="data-root", type="data"),
            migration(migrationId="schema-child", dependsOn=["data-root"]),
        ],
        {"data-root": {"sha256": "b" * 64, "status": "APPLIED"}},
        target_environment="test",
        publish_scope="code-only",
    )

    assert plan["status"] == "blocked"
    assert [item["action"] for item in plan["items"]] == [
        "SKIP_SCOPE_EXCLUDED",
        "BLOCKED_SCOPE_DEPENDENCY",
    ]
    assert "schema-child -> data-root" in plan["items"][1]["reason"]


def test_code_only_scope_excludes_unreferenced_pending_data_without_blocking() -> None:
    plan = build_preflight_plan(
        [migration(type="data")],
        {},
        target_environment="test",
        publish_scope="code-only",
    )

    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "SKIP_SCOPE_EXCLUDED"


def test_with_data_scope_applies_data_dependency_and_child() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="data-root", type="data"),
            migration(migrationId="schema-child", dependsOn=["data-root"]),
        ],
        {},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert [item["action"] for item in plan["items"]] == ["APPLY", "APPLY"]


def test_preflight_preserves_manifest_order_when_dependencies_become_ready() -> None:
    plan = build_preflight_plan(
        [
            migration(migrationId="binding-dependency"),
            migration(migrationId="cleanup-dependency"),
            migration(migrationId="cleanup", dependsOn=["cleanup-dependency"]),
            migration(migrationId="binding", dependsOn=["binding-dependency"]),
        ],
        {},
        target_environment="test",
        publish_scope="with-data",
    )

    assert plan["status"] == "passed"
    assert [item["migrationId"] for item in plan["items"]] == [
        "binding-dependency",
        "cleanup-dependency",
        "cleanup",
        "binding",
    ]


def test_preflight_outputs_apply_when_safe() -> None:
    plan = build_preflight_plan([migration()], {}, target_environment="test", publish_scope="with-data")

    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "APPLY"


def test_cli_generates_plan_from_manifest_schema_migrations(tmp_path) -> None:
    manifest = tmp_path / "manifest.json"
    target_state = tmp_path / "target-state.json"
    output = tmp_path / "preflight-plan.json"
    manifest.write_text(json.dumps({"schemaMigrations": [migration(sha256="sha256:" + "a" * 64)]}), encoding="utf-8")
    target_state.write_text(json.dumps({"m1": {"sha256": "a" * 64, "status": "APPLIED"}}), encoding="utf-8")

    assert main([
        "--manifest", str(manifest),
        "--target-state", str(target_state),
        "--target-environment", "test",
        "--publish-scope", "with-data",
        "--output", str(output),
    ]) == 0

    plan = json.loads(output.read_text(encoding="utf-8"))
    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "SKIP_ALREADY_APPLIED"


def test_cli_generates_plan_from_manifest_v1_database_schema_migrations(tmp_path) -> None:
    manifest = tmp_path / "manifest.json"
    target_state = tmp_path / "target-state.json"
    output = tmp_path / "preflight-plan.json"
    manifest.write_text(
        json.dumps({"database": {"schemaMigrations": [migration(sha256="sha256:" + "c" * 64)]}}),
        encoding="utf-8",
    )
    target_state.write_text(json.dumps({}), encoding="utf-8")

    assert main([
        "--manifest", str(manifest),
        "--target-state", str(target_state),
        "--target-environment", "test",
        "--publish-scope", "with-data",
        "--output", str(output),
    ]) == 0

    plan = json.loads(output.read_text(encoding="utf-8"))
    assert plan["status"] == "passed"
    assert plan["items"][0]["migrationId"] == "m1"
    assert plan["items"][0]["action"] == "APPLY"


def test_cli_accepts_release_metadata_only_checksum_drift(tmp_path) -> None:
    manifest = tmp_path / "manifest.json"
    target_state = tmp_path / "target-state.json"
    output = tmp_path / "preflight-plan.json"
    sql_dir = tmp_path / "required-sql"
    sql_dir.mkdir()
    sql_body = b"CREATE TABLE demo_metadata_only (id bigint);\n"
    sql_with_metadata = (
        b"-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium\n"
        + sql_body
    )
    (sql_dir / "m1.sql").write_bytes(sql_with_metadata)
    manifest.write_text(
        json.dumps({
            "database": {
                "schemaMigrations": [
                    migration(
                        file="required-sql/m1.sql",
                        sha256="sha256:" + hashlib.sha256(sql_with_metadata).hexdigest(),
                    )
                ]
            }
        }),
        encoding="utf-8",
    )
    target_state.write_text(
        json.dumps({"m1": {"sha256": hashlib.sha256(sql_body).hexdigest(), "status": "APPLIED"}}),
        encoding="utf-8",
    )

    assert main([
        "--manifest", str(manifest),
        "--target-state", str(target_state),
        "--target-environment", "test",
        "--publish-scope", "with-data",
        "--output", str(output),
    ]) == 0

    plan = json.loads(output.read_text(encoding="utf-8"))
    assert plan["status"] == "passed"
    assert plan["items"][0]["action"] == "SKIP_ALREADY_APPLIED"
