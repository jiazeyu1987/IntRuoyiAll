from pathlib import Path

from script.release.release_migration_policy_gate import run_migration_policy_gate


REPO_ROOT = Path(__file__).resolve().parents[2]
PUBLISH_SCRIPT = REPO_ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1"
SHOWROOM_HALL_CANVAS_SQL = REPO_ROOT / "sql" / "showroom" / "20260606_showroom_hall_product_canvas_layout.sql"
SHOWROOM_PRODUCT_ATTACHMENT_SQL = REPO_ROOT / "sql" / "showroom" / "20260605_showroom_product_revision_attachment_schema.sql"
SHOWROOM_AWARD_SQL = REPO_ROOT / "sql" / "showroom" / "20260613_showroom_award_and_hall_item_schema.sql"
SHOWROOM_HALL_BACKGROUND_SQL = REPO_ROOT / "sql" / "showroom" / "20260615_showroom_hall_canvas_background.sql"
SHOWROOM_TARGET_MARKET_SQL = REPO_ROOT / "sql" / "showroom" / "20260703_showroom_product_target_market_text.sql"
SHOWROOM_AWARD_DO = (
    REPO_ROOT
    / "yudao-module-showroom"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "showroom"
    / "dal"
    / "dataobject"
    / "content"
    / "ShowroomAwardDO.java"
)


def read_publish_script() -> str:
    return PUBLISH_SCRIPT.read_text(encoding="utf-8")


def extract_function(text: str, function_name: str) -> str:
    start = text.index(f"function {function_name}")
    open_brace = text.index("{", start)
    depth = 0
    for index in range(open_brace, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    raise AssertionError(f"Unable to find complete PowerShell function {function_name}")


def test_showroom_award_schema_is_required_by_runtime_and_has_release_metadata() -> None:
    award_do = SHOWROOM_AWARD_DO.read_text(encoding="utf-8")
    sql = SHOWROOM_AWARD_SQL.read_text(encoding="utf-8")

    assert '@TableName("showroom_award")' in award_do
    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260606_showroom_hall_product_canvas_layout; type=schema; riskLevel=medium"
    )
    for snippet in [
        "CREATE TABLE IF NOT EXISTS `showroom_award`",
        "UNIQUE KEY `uk_showroom_award_code` (`tenant_id`, `award_code`)",
        "CREATE TABLE IF NOT EXISTS `showroom_award_revision`",
        "UNIQUE KEY `uk_showroom_award_revision_no` (`tenant_id`, `award_id`, `revision_no`)",
        "KEY `idx_showroom_award_revision_award` (`tenant_id`, `award_id`, `status`)",
        "CREATE TABLE IF NOT EXISTS `showroom_hall_item`",
        "UNIQUE KEY `uk_showroom_hall_item` (`tenant_id`, `hall_id`, `item_type`, `item_id`)",
        "KEY `idx_showroom_hall_item_order` (`tenant_id`, `hall_id`, `display_order`, `id`)",
        "KEY `idx_showroom_hall_item_item` (`tenant_id`, `item_type`, `item_id`)",
    ]:
        assert snippet in sql


def test_showroom_runtime_schema_gaps_have_release_metadata() -> None:
    product_attachment_sql = SHOWROOM_PRODUCT_ATTACHMENT_SQL.read_text(encoding="utf-8")
    hall_background_sql = SHOWROOM_HALL_BACKGROUND_SQL.read_text(encoding="utf-8")
    target_market_sql = SHOWROOM_TARGET_MARKET_SQL.read_text(encoding="utf-8")

    assert product_attachment_sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )
    assert "CREATE TABLE IF NOT EXISTS `showroom_product_revision_attachment`" in product_attachment_sql
    assert "uk_showroom_product_revision_attachment_file" in product_attachment_sql

    assert hall_background_sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260613_showroom_award_and_hall_item_schema; type=schema; riskLevel=medium"
    )
    assert "COLUMN_NAME = 'canvas_background_image_url'" in hall_background_sql
    assert "ALTER TABLE `showroom_hall` ADD COLUMN `canvas_background_image_url`" in hall_background_sql

    assert target_market_sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260519_showroom_v1_schema; type=schema; riskLevel=medium"
    )
    assert "ALTER TABLE `showroom_product_revision`" in target_market_sql
    assert "MODIFY COLUMN `target_market` text DEFAULT NULL" in target_market_sql


def test_publish_required_sql_contract_includes_showroom_award_schema_source_path() -> None:
    text = read_publish_script()
    roots_body = extract_function(text, "Get-ReleaseDatabaseSqlRoots")
    collect_body = extract_function(text, "Get-ReleaseDatabaseSqlScripts")
    manifest_body = extract_function(text, "New-ReleaseRequiredSqlManifestEntries")
    policy_body = extract_function(text, "Invoke-ReleaseMigrationPolicyGate")

    assert "Get-ReleaseDatabaseSqlRoots" in text
    assert "sql/showroom" in roots_body
    assert "20260605_showroom_product_revision_attachment_schema.sql" in roots_body
    assert "20260613_showroom_award_and_hall_item_schema.sql" in roots_body
    assert "20260615_showroom_hall_canvas_background.sql" in roots_body
    assert "sql/showroom" in roots_body
    assert "Read-ReleaseMigrationMetadata -SqlPath $file.FullName" in collect_body
    assert "sourcePath = $relativePath" in manifest_body
    assert "Read-ReleaseMigrationMetadata" in manifest_body
    assert "allowedEnvironments = @($metadata.allowedEnvironments)" in manifest_body
    assert "dependsOn = @($metadata.dependsOn)" in manifest_body
    assert "riskLevel = [string]$metadata.riskLevel" in manifest_body
    assert "foreach ($root in @(Get-ReleaseDatabaseSqlRoots))" in policy_body
    assert "--sql-root" in policy_body
    assert "--sql-file" in policy_body


def test_policy_gate_accepts_explicit_showroom_award_sql_file_with_source_prefix() -> None:
    report = run_migration_policy_gate(
        REPO_ROOT / "sql" / "showroom",
        sql_paths=[SHOWROOM_HALL_CANVAS_SQL, SHOWROOM_AWARD_SQL],
        file_prefix="sql/showroom",
    )

    assert report["status"] == "passed"
    assert report["migrationCount"] == 2
    migrations = {migration["migrationId"]: migration for migration in report["migrations"]}
    assert migrations["20260606_showroom_hall_product_canvas_layout"]["file"] == (
        "sql/showroom/20260606_showroom_hall_product_canvas_layout.sql"
    )
    assert migrations["20260613_showroom_award_and_hall_item_schema"]["file"] == (
        "sql/showroom/20260613_showroom_award_and_hall_item_schema.sql"
    )
    assert migrations["20260613_showroom_award_and_hall_item_schema"]["dependsOn"] == [
        "20260606_showroom_hall_product_canvas_layout"
    ]


def test_deploy_release_checks_showroom_award_schema_before_backend_start() -> None:
    text = read_publish_script()
    preflight_body = extract_function(text, "Assert-RemoteShowroomAwardSchemaReady")

    for snippet in [
        "showroom_award",
        "showroom_award_revision",
        "showroom_hall_item",
        "showroom_product_revision_attachment",
        "showroom_hall",
        "canvas_background_image_url",
        "uk_showroom_award_code",
        "uk_showroom_award_revision_no",
        "idx_showroom_award_revision_award",
        "uk_showroom_hall_item",
        "idx_showroom_hall_item_order",
        "idx_showroom_hall_item_item",
        "uk_showroom_product_revision_attachment_file",
        "idx_showroom_product_revision_attachment_revision",
        "idx_showroom_product_revision_attachment_product",
        "SHOWROOM_AWARD_SCHEMA_MISSING",
        "information_schema.TABLES",
        "information_schema.STATISTICS",
        "information_schema.COLUMNS",
    ]:
        assert snippet in preflight_body

    required_sql_idx = text.rindex("Invoke-RequiredDatabaseSqlScripts")
    showroom_preflight_idx = text.index("Assert-RemoteShowroomAwardSchemaReady", required_sql_idx)
    backend_start_idx = text.index("Starting application services on the $PublishTargetName server", showroom_preflight_idx)
    assert required_sql_idx < showroom_preflight_idx < backend_start_idx


def test_code_only_deploy_still_runs_required_sql_and_schema_preflight() -> None:
    text = read_publish_script()
    deploy_start = text.index("if ($Mode -eq 'deploy-release') {")
    deploy_block = text[deploy_start : text.index("Set-PublishRuntimeDefaultsForTarget", deploy_start)]
    runtime_block = text[text.index("if ($publishBackend) {", text.index("Starting MySQL and Redis")) :]

    assert "$requiredDatabaseSqlScripts = Get-ReleasePackageDatabaseSqlScripts" in deploy_block
    assert "if ($releasePublishScope -eq 'code-only')" in deploy_block
    assert "Assert-RequiredDatabaseSqlScriptsInRelease" in deploy_block
    assert "Invoke-RequiredDatabaseSqlScripts" in runtime_block
    assert "Assert-RemoteShowroomAwardSchemaReady" in runtime_block
