import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("menu_gate", ROOT / "scripts/preflight/check-menu-migrations.py")
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)
SOURCE = "IntRuoyiFronted/src/views/mes/example/workbench.vue"
PAGE = "IntRuoyiFronted/src/views/mes/example/index.vue"
ROUTER = "IntRuoyiFronted/src/router/modules/example.ts"
SQL = "IntRuoyiBackend/sql/mysql/20261021_example_menu.sql"
META = "-- release-migration: allowedEnvironments=test,prod,backup; dependsOn=; type=menu; riskLevel=low\n"
MIGRATION = META + """
INSERT INTO system_menu (id, permission, component) VALUES (1, 'mes:example:query', 'mes/example/index');
INSERT INTO system_role_menu (role_id, menu_id) VALUES (2, 1);
UPDATE system_tenant_package SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', 1) WHERE id = 3;
"""


class MenuMigrationGateTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="menu-gate-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.git("init", "-q")
        self.git("config", "user.email", "menu-gate-test@example.invalid")
        self.git("config", "user.name", "Menu gate test")
        self.git("config", "core.hooksPath", ".git/test-no-hooks")
        self.git("config", "core.autocrlf", "false")
        for name in ("release_migration_manifest.py", "release_migration_policy_gate.py"):
            self.write("IntRuoyiBackend/script/release/" + name,
                       (ROOT / "IntRuoyiBackend/script/release" / name).read_text(encoding="utf-8"))
        self.write(SOURCE, "<template>Existing workbench</template>\n")
        self.write(ROUTER, "export default []\n")
        self.decisions([])
        self.git("add", ".")
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "Baseline")
        self.baseline = self.git("rev-parse", "HEAD").strip()

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.root), *args], check=True,
                              capture_output=True, encoding="utf-8").stdout

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")

    def decisions(self, items):
        self.write(gate.CONTRACT, json.dumps({"changes": items}))

    def permission(self):
        self.write(SOURCE, "<button v-hasPermi=\"['mes:example:query']\">Query</button>\n")
        return {"source": SOURCE, "kind": "permission", "value": "mes:example:query"}

    def check(self, **kwargs):
        return gate.check(self.root, baseline=self.baseline, **kwargs)

    def cover(self, finding):
        self.write(SQL, MIGRATION)
        self.decisions([{**finding, "migrations": [SQL]}])

    def test_missing_migration_is_detected_even_after_an_unchecked_commit(self):
        self.permission()
        self.git("add", ".")
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "Unchecked feature")
        with self.assertRaisesRegex(gate.GateError, "Missing migration decision"):
            self.check()

    def test_declared_historical_original_is_required_in_package(self):
        self.cover(self.permission())
        self.write(SQL, MIGRATION.replace('dependsOn=;', 'dependsOn=20260101_base;'))
        reviewed = gate.SQL_ROOT + '20260101_base.sql'
        original = gate.SQL_ROOT + 'historical/20260101_base.sql'
        self.write(reviewed, META.replace('type=menu', 'type=schema') + 'SELECT 1;')
        self.write(original, META.replace('type=menu', 'type=schema') + 'SELECT 2;')
        with self.assertRaisesRegex(gate.GateError, 'Dependency missing or ambiguous'):
            self.check()
        self.write(gate.SQL_ROOT + 'release-sources.json', json.dumps({
            'schemaVersion': 'release-sql-sources/v2',
            'historicalMigrations': [{'file': '20260101_base.sql', 'original': 'historical/20260101_base.sql'}],
        }))
        result = self.check(packaged_sql={SQL, original})
        self.assertEqual(set(result['requiredSql']), {SQL, original})
        with self.assertRaisesRegex(gate.GateError, 'Migrations omitted from release package'):
            self.check(packaged_sql={SQL, reviewed})

    def test_catalog_does_not_hide_extra_duplicate_or_missing_original(self):
        self.cover(self.permission())
        self.write(SQL, MIGRATION.replace('dependsOn=;', 'dependsOn=20260101_base;'))
        reviewed = gate.SQL_ROOT + '20260101_base.sql'
        original = gate.SQL_ROOT + 'historical/20260101_base.sql'
        self.write(reviewed, META + 'SELECT 1;')
        self.write(gate.SQL_ROOT + 'release-sources.json', json.dumps({
            'schemaVersion': 'release-sql-sources/v2',
            'historicalMigrations': [{'file': '20260101_base.sql', 'original': 'historical/20260101_base.sql'}],
        }))
        with self.assertRaisesRegex(gate.GateError, 'Historical migration files missing or ambiguous'):
            self.check()
        self.write(original, META + 'SELECT 1;')
        self.write(gate.SQL_ROOT + 'other/20260101_base.sql', META + 'SELECT 1;')
        with self.assertRaisesRegex(gate.GateError, 'Historical migration files missing or ambiguous'):
            self.check()

    def test_permission_and_page_link_to_same_valid_migration(self):
        permission = self.permission()
        self.write(PAGE, "<template>New page</template>")
        self.write(SQL, MIGRATION)
        self.decisions([{**permission, "migrations": [SQL]},
                        {"source": PAGE, "kind": "page", "value": "mes/example/index", "migrations": [SQL]}])
        result = self.check(packaged_sql={SQL})
        self.assertEqual(result["signalCount"], 2)
        self.assertEqual(result["requiredSql"], [SQL])

    def test_sql_present_only_in_worktree_does_not_cover_staged_code(self):
        self.cover(self.permission())
        self.git("add", SOURCE, gate.CONTRACT)
        with self.assertRaisesRegex(gate.GateError, "Migration missing"):
            self.check(staged=True)
        self.git("add", SQL)
        self.assertEqual(self.check(staged=True)["status"], "passed")

    def test_worktree_decision_cannot_hide_missing_staged_decision(self):
        finding = self.permission()
        self.git("add", SOURCE)
        self.decisions([{**finding, "reason": "Reuses existing query menu"}])
        with self.assertRaisesRegex(gate.GateError, "Missing migration decision"):
            self.check(staged=True)

    def test_uncommitted_sql_cannot_cover_pushed_commit(self):
        finding = self.permission()
        self.git("add", SOURCE)
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "Unchecked permission")
        revision = self.git("rev-parse", "HEAD").strip()
        self.cover(finding)
        self.assertEqual(self.check()["status"], "passed")
        with self.assertRaisesRegex(gate.GateError, "Missing migration decision"):
            self.check(revision=revision)

    def test_concrete_reuse_reason_is_accepted_but_empty_reason_is_not(self):
        finding = self.permission()
        self.decisions([{**finding, "reason": "Reuses existing query menu"}])
        self.assertEqual(self.check()["status"], "passed")
        self.decisions([{**finding, "reason": "  "}])
        with self.assertRaisesRegex(gate.GateError, "supply migrations OR"):
            self.check()

    def test_missing_metadata_and_dependency_are_rejected(self):
        self.cover(self.permission())
        self.write(SQL, MIGRATION.split("\n", 1)[1])
        with self.assertRaisesRegex(gate.GateError, "Missing release-migration metadata"):
            self.check()
        self.write(SQL, MIGRATION.replace("dependsOn=;", "dependsOn=20261020_missing;"))
        with self.assertRaisesRegex(gate.GateError, "Dependency missing"):
            self.check()

    def test_dependencies_must_also_be_packaged(self):
        self.cover(self.permission())
        dependency = "IntRuoyiBackend/sql/mysql/20261020_dependency.sql"
        self.write(dependency, META.replace("type=menu", "type=schema") + "SELECT 1;\n")
        self.write(SQL, MIGRATION.replace("dependsOn=;", "dependsOn=20261020_dependency;"))
        with self.assertRaisesRegex(gate.GateError, "omitted from release package"):
            self.check(packaged_sql={SQL})
        self.assertEqual(self.check(packaged_sql={SQL, dependency})["status"], "passed")

    def test_comments_cannot_fake_menu_grants_or_permission_binding(self):
        self.cover(self.permission())
        self.write(SQL, META + "/* " + MIGRATION.split("\n", 1)[1] + " */\nSELECT 1;")
        with self.assertRaisesRegex(gate.GateError, "SQL does not contain"):
            self.check()
        self.write(SQL, MIGRATION.replace("INSERT INTO system_role_menu", "-- INSERT INTO system_role_menu"))
        with self.assertRaisesRegex(gate.GateError, "must write system_role_menu"):
            self.check()

    def test_unchanged_historical_sql_does_not_count_as_new_migration(self):
        self.write(SQL, MIGRATION)
        self.git("add", SQL)
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "Historical SQL")
        self.baseline = self.git("rev-parse", "HEAD").strip()
        self.cover(self.permission())
        with self.assertRaisesRegex(gate.GateError, "unchanged historical"):
            self.check()
        self.write(SQL, MIGRATION + "\nSELECT 2;\n")
        with self.assertRaisesRegex(gate.GateError, "historical SQL"):
            self.check()

    def test_router_changes_require_fresh_decision_and_comments_do_not_trigger(self):
        old = (self.root / ROUTER).read_text()
        new = "export default [{path: '/example', component: 'mes/example/index'}]\n"
        self.write(ROUTER, new)
        finding = gate.signals(ROUTER, old, new)[0]
        self.decisions([{**finding, "reason": "Existing menu, route points to its detail page"}])
        self.assertEqual(self.check()["status"], "passed")
        self.write(ROUTER, new.replace("/example", "/example-next"))
        with self.assertRaisesRegex(gate.GateError, "Missing migration decision"):
            self.check()
        self.write(ROUTER, old + "// permission 'mes:example:query'\n")
        self.assertEqual(self.check()["signalCount"], 0)

    def test_frontend_only_package_cannot_drop_required_sql(self):
        self.cover(self.permission())
        with self.assertRaisesRegex(gate.GateError, "omitted from release package"):
            self.check(packaged_sql=set())

    def test_test_only_migration_is_not_a_three_environment_migration(self):
        self.cover(self.permission())
        self.write(SQL, MIGRATION.replace("test,prod,backup", "test"))
        with self.assertRaisesRegex(gate.GateError, "must support test,prod,backup"):
            self.check()

    def test_hundreds_of_staged_sources_use_one_batch_not_one_process_per_file(self):
        for number in range(700):
            self.write(f"IntRuoyiFronted/src/components/batch-{number}.ts", "export const label = 'Example';\n")
        self.git("add", ".")
        with patch.object(gate.subprocess, "run", wraps=gate.subprocess.run) as measured:
            result = self.check(staged=True)
        self.assertEqual(result, {"status": "passed", "signalCount": 0, "requiredSql": []})
        commands = [call.args[0][3:] for call in measured.call_args_list]
        self.assertFalse(any(command[:1] == ["show"] for command in commands))
        self.assertEqual(sum("--batch" in command for command in commands), 1)
        self.assertLessEqual(len(commands), 5)

    def test_batch_reader_handles_unicode_newline_paths_and_byte_lengths(self):
        path = "batch/中文文件.txt"
        self.write(path, "")
        (self.root / path).write_bytes("\ufeff第一行\r\n第二行\0正文\n".encode("utf-8"))
        self.write("batch/empty.txt", "")
        self.git("add", "batch")
        blob = self.git("rev-parse", ":" + path).strip()
        # Git trees can contain newline paths even though Windows cannot check them out.
        tree = subprocess.run(["git", "-C", str(self.root), "mktree", "-z"],
                              input=f"100644 blob {blob}\t中文\n文件.txt\0".encode("utf-8"),
                              check=True, capture_output=True).stdout.decode().strip()
        newline_expression = tree + ":中文\n文件.txt"
        self.write(path, "Unstaged changes must not be read")
        result = gate.read_git_objects(self.root, [":" + path, ":batch/empty.txt", ":" + path, newline_expression])
        self.assertEqual(result, {":" + path: "第一行\n第二行\0正文\n", ":batch/empty.txt": "",
                                  newline_expression: "第一行\n第二行\0正文\n"})
        with self.assertRaisesRegex(gate.GateError, "Cannot read Git blob"):
            gate.read_git_objects(self.root, [":batch/not-present.txt"])

    def test_shared_migration_is_read_and_validated_once_per_check(self):
        finding = self.permission()
        self.write(PAGE, "<template>New page</template>")
        self.write(SQL, MIGRATION)
        self.decisions([{**finding, "migrations": [SQL]},
                        {"source": PAGE, "kind": "page", "value": "mes/example/index", "migrations": [SQL]}])
        self.git("add", ".")
        with patch.object(gate, "read_git_objects", wraps=gate.read_git_objects) as reads, \
                patch.object(gate, "validate_migrations", wraps=gate.validate_migrations) as validations:
            result = self.check(staged=True, packaged_sql={SQL})
        self.assertEqual(result["signalCount"], 2)
        self.assertEqual(validations.call_count, 1)
        self.assertEqual(sum(call.args[1].count(":" + SQL) for call in reads.call_args_list), 1)

    def test_committed_snapshot_keeps_valid_sql_despite_worktree_changes(self):
        self.cover(self.permission())
        self.git("add", ".")
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "Covered permission")
        revision = self.git("rev-parse", "HEAD").strip()
        self.write(SQL, "Broken uncommitted SQL")
        self.decisions([])
        self.write(SOURCE, "<button v-hasPermi=\"['mes:other:query']\">Other</button>")
        self.assertEqual(self.check(revision=revision, packaged_sql={SQL}),
                         {"status": "passed", "signalCount": 1, "requiredSql": [SQL]})

    @unittest.skipUnless(shutil.which("powershell.exe"), "Windows release entry uses PowerShell")
    def test_release_entry_passes_actual_package_list_and_stops_on_failure(self):
        self.cover(self.permission())
        checker = (ROOT / "scripts/preflight/check-menu-migrations.py").read_text(encoding="utf-8")
        self.write("scripts/preflight/check-menu-migrations.py", checker.replace(gate.BASELINE, self.baseline))
        publisher = (ROOT / "IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1").read_text(encoding="utf-8")
        helper = publisher.split("function Invoke-MenuMigrationDevelopmentGate {", 1)[1].split("function Invoke-ReleaseMigrationPolicyGate", 1)[0]
        helper = "function Invoke-MenuMigrationDevelopmentGate {" + helper
        harness = """
param([string]$Root, [string]$PackageSql)
$ErrorActionPreference = 'Continue'
$backendRepo = Join-Path $Root 'IntRuoyiBackend'
$frontendDir = Join-Path $Root 'IntRuoyiFronted'
$publishBackend = $true
$requiredDatabaseSqlScripts = @()
if ($PackageSql) { $requiredDatabaseSqlScripts = @(@{Path = $PackageSql}) }
function Fail([string]$Message) { throw $Message }
function Info([string]$Message) { Write-Output $Message }
function Invoke-ProcessCapture($FilePath, $ArgumentList, $WorkingDirectory) {
    Push-Location $WorkingDirectory
    try {
        $output = & $FilePath @ArgumentList 2>&1 | Out-String
        return @{ExitCode = $LASTEXITCODE; StdOut = $output; StdErr = ''}
    } finally { Pop-Location }
}
""" + helper + "\ntry { Invoke-MenuMigrationDevelopmentGate } catch { Write-Output $_; exit 1 }\n"
        self.write("release-entry-test.ps1", harness)

        def run(package_sql):
            return subprocess.run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                                   str(self.root / "release-entry-test.ps1"), "-Root", str(self.root),
                                   "-PackageSql", package_sql], capture_output=True, encoding="utf-8", errors="replace")

        missing = run("sql/mysql/20261020_other.sql")
        self.assertNotEqual(missing.returncode, 0, missing.stdout + missing.stderr)
        self.assertIn("omitted from release package", missing.stdout + missing.stderr)
        complete = run(SQL.removeprefix("IntRuoyiBackend/"))
        self.assertEqual(complete.returncode, 0, complete.stdout + complete.stderr)


if __name__ == "__main__":
    unittest.main()
