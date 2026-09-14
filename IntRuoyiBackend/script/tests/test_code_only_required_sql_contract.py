import json
import subprocess
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "deploy" / "publish-int-ruoyi.ps1"


def read_script() -> str:
    return SCRIPT.read_text(encoding="utf-8")


def run_code_only_filter(
    tmp_path: Path,
    required_sql: list[dict[str, object]],
    plan_items: list[dict[str, object]],
) -> subprocess.CompletedProcess[str]:
    text = read_script()
    function_start = text.index("function Get-ReleasePreflightApplyItems")
    function_end = text.index("function Invoke-ReleaseMigrationStateUpdate", function_start)
    function_text = text[function_start:function_end]
    harness = f"""
$ErrorActionPreference = 'Stop'
function Info([string]$Message) {{}}
function Fail([string]$Message) {{ throw $Message }}
{function_text}
$requiredDatabaseSqlScripts = ConvertFrom-Json @'
{json.dumps(required_sql)}
'@
$preflightPlan = ConvertFrom-Json @'
{json.dumps({"items": plan_items})}
'@
$selected = @(Get-ReleasePreflightApplyItems -PreflightPlan $preflightPlan -PublishScope 'code-only')
[pscustomobject]@{{
    selected = @($selected | ForEach-Object {{ [string]$_.migrationId }})
}} | ConvertTo-Json -Compress
"""
    harness_path = tmp_path / "code-only-filter-harness.ps1"
    harness_path.write_text(harness, encoding="utf-8")
    return subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(harness_path),
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=False,
    )


def test_required_sql_manifest_entries_preserve_type_for_scope_filtering() -> None:
    text = read_script()

    assert "type = [string]$metadata.type" in text
    assert "Type = [string]$_.type" in text
    assert "MigrationId = [string]$_.migrationId" in text
    assert "DependsOn = @($_.dependsOn)" in text


def test_code_only_deploy_skips_data_required_sql_before_remote_mysql_apply() -> None:
    text = read_script()

    assert (
        "Get-ReleasePreflightApplyItems -PreflightPlan $preflightPlan "
        "-PublishScope $releasePublishScope"
    ) in text
    assert "Skipping data required database SQL for code-only release" in text
    assert "$requiredSqlTypeByMigrationId[$migrationId]" in text
    assert (
        "preflight-plan.json APPLY item missing from manifest requiredSql "
        "for code-only scope filtering"
    ) in text
    skip_index = text.index("Skipping data required database SQL for code-only release")
    apply_index = text.index('Info "Applying required database SQL: $fileName"')
    assert skip_index < apply_index


def test_code_only_filter_skips_transitive_dependents_of_data_migrations(tmp_path: Path) -> None:
    required_sql = [
        {"MigrationId": "data-root", "Type": "data", "DependsOn": []},
        {"MigrationId": "seed-child", "Type": "seed", "DependsOn": ["data-root"]},
        {"MigrationId": "menu-grandchild", "Type": "menu", "DependsOn": ["seed-child"]},
        {"MigrationId": "schema-independent", "Type": "schema", "DependsOn": []},
    ]
    plan_items = [
        {"migrationId": item["MigrationId"], "action": "APPLY"}
        for item in required_sql
    ]

    result = run_code_only_filter(tmp_path, required_sql, plan_items)

    assert result.returncode == 0, result.stderr
    assert json.loads(result.stdout)["selected"] == ["schema-independent"]


def test_code_only_filter_fails_fast_on_missing_dependency_metadata(tmp_path: Path) -> None:
    required_sql = [
        {
            "MigrationId": "menu-with-missing-parent",
            "Type": "menu",
            "DependsOn": ["missing-parent"],
        }
    ]
    plan_items = [{"migrationId": "menu-with-missing-parent", "action": "APPLY"}]

    result = run_code_only_filter(tmp_path, required_sql, plan_items)

    assert result.returncode != 0
    assert "dependency missing from manifest requiredSql" in result.stderr
