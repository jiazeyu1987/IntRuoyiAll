import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "verify_tdd_compliance.py"
SPEC = importlib.util.spec_from_file_location("verify_tdd_compliance", MODULE_PATH)
verify_tdd_compliance = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(verify_tdd_compliance)


class VerifyTddComplianceTestCase(unittest.TestCase):
    def test_backend_java_change_requires_matching_java_test_and_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            task_dir = self._write_evidence(repo_root)

            findings = verify_tdd_compliance.evaluate_compliance(
                [
                    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java",
                    "script/tests/test_auto_schedule_demo_tooling.py",
                ],
                repo_root=repo_root,
                task_dir=task_dir,
            )

            self.assertEqual(
                findings,
                [
                    "backend production changes require a changed Java test under src/test/.",
                ],
            )

    def test_script_or_sql_change_requires_script_test_and_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            task_dir = self._write_evidence(repo_root)

            findings = verify_tdd_compliance.evaluate_compliance(
                [
                    "script/shell/mes-auto-schedule-first-loop-demo.ps1",
                    "sql/mysql/mes-auto-schedule-complete-demo-data.sql",
                    "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java",
                ],
                repo_root=repo_root,
                task_dir=task_dir,
            )

            self.assertEqual(
                findings,
                [
                    "repo tooling changes under script/ or sql/mysql/ require a changed script test under script/tests/.",
                ],
            )

    def test_tooling_and_backend_changes_pass_with_matching_tests_and_red_green_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            task_dir = self._write_evidence(repo_root)

            findings = verify_tdd_compliance.evaluate_compliance(
                [
                    "script/shell/mes-auto-schedule-first-loop-demo.ps1",
                    "sql/mysql/mes-auto-schedule-complete-demo-data.sql",
                    "script/tests/test_auto_schedule_demo_tooling.py",
                    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java",
                    "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java",
                    "tool/verify_tdd_compliance.py",
                    "tool/tests/test_verify_tdd_compliance.py",
                ],
                repo_root=repo_root,
                task_dir=task_dir,
            )

            self.assertEqual(findings, [])

    def test_production_change_fails_without_red_green_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            task_dir, task_ref = self._task_dir_pair(repo_root)
            task_dir.mkdir(parents=True, exist_ok=True)
            (task_dir / "execution-log.md").write_text(
                "Only a passing test run is recorded.\n",
                encoding="utf-8",
            )

            findings = verify_tdd_compliance.evaluate_compliance(
                [
                    "script/shell/mes-auto-schedule-first-loop-demo.ps1",
                    "script/tests/test_auto_schedule_demo_tooling.py",
                ],
                repo_root=repo_root,
                task_dir=task_ref,
            )

            self.assertEqual(
                findings,
                [
                    "TDD evidence must include a RED line with FAIL and a GREEN line with PASS in execution-log.md.",
                ],
            )

    def test_non_production_changes_do_not_require_tdd_evidence(self) -> None:
        findings = verify_tdd_compliance.evaluate_compliance(
            ["docs/system/local-auto-schedule-replay.md", "doc/tasks/task-id/task.md"],
            repo_root=Path.cwd(),
            task_dir=None,
        )

        self.assertEqual(findings, [])

    def test_collect_all_changed_paths_includes_untracked_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            self._run_git(repo_root, "init")
            self._run_git(repo_root, "config", "user.email", "tdd@example.test")
            self._run_git(repo_root, "config", "user.name", "TDD Test")
            (repo_root / "README.md").write_text("initial\n", encoding="utf-8")
            self._run_git(repo_root, "add", "README.md")
            self._run_git(repo_root, "commit", "-m", "initial")

            new_tool = repo_root / "tool" / "new_tool.py"
            new_tool.parent.mkdir()
            new_tool.write_text("print('new')\n", encoding="utf-8")

            changed_paths = verify_tdd_compliance.collect_changed_paths(
                repo_root,
                all_changed=True,
            )

            self.assertIn("tool/new_tool.py", changed_paths)

    def test_pre_commit_hook_requires_task_dir_and_runs_staged_gate(self) -> None:
        hook = verify_tdd_compliance.build_pre_commit_hook()

        self.assertIn('if [ -z "$TDD_TASK_DIR" ]; then', hook)
        self.assertIn("set TDD_TASK_DIR to the task directory path", hook)
        self.assertIn('python "tool/verify_tdd_compliance.py" --task-dir "$TDD_TASK_DIR"', hook)
        self.assertNotIn("--all-changed", hook)

    def test_install_pre_commit_hook_writes_versioned_gate(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            self._run_git(repo_root, "init")

            hook_path = verify_tdd_compliance.install_pre_commit_hook(
                repo_root,
                script_path="tool/verify_tdd_compliance.py",
            )

            self.assertEqual(hook_path, repo_root / ".git" / "hooks" / "pre-commit")
            self.assertEqual(hook_path.read_text(encoding="utf-8"), verify_tdd_compliance.build_pre_commit_hook())

    def _write_evidence(self, repo_root: Path) -> Path:
        task_dir, task_ref = self._task_dir_pair(repo_root)
        task_dir.mkdir(parents=True, exist_ok=True)
        (task_dir / "execution-log.md").write_text(
            "\n".join(
                [
                    "## TDD Evidence",
                    "- RED: `python -m unittest discover -s tool/tests -p test_verify_tdd_compliance.py` -> FAIL, verifier missing.",
                    "- GREEN: `python -m unittest discover -s tool/tests -p test_verify_tdd_compliance.py` -> PASS.",
                    "",
                ]
            ),
            encoding="utf-8",
        )
        return task_ref

    def _task_dir_pair(self, repo_root: Path) -> tuple[Path, Path]:
        task_root_name = f"{repo_root.name}-doc"
        return (
            repo_root.parent / task_root_name / "tasks" / "strict-tdd",
            Path("..") / task_root_name / "tasks" / "strict-tdd",
        )

    def _run_git(self, repo_root: Path, *args: str) -> None:
        subprocess.run(
            ["git", *args],
            cwd=repo_root,
            check=True,
            capture_output=True,
            encoding="utf-8",
            text=True,
        )


if __name__ == "__main__":
    unittest.main()
