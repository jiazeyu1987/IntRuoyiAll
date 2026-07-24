from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[2]
APPROVAL_SQL = ROOT / "sql" / "mysql" / "20260610_mes_admin_edhr_approval_v1_seed.sql"
DEDUPE_SQL = ROOT / "sql" / "mysql" / "20260610_mes_admin_edhr_task_dedupe.sql"


def require_contains(text: str, needle: str, label: str, errors: list[str]) -> None:
    if needle not in text:
        errors.append(f"{label}: missing `{needle}`")


def main() -> int:
    errors: list[str] = []
    approval_text = APPROVAL_SQL.read_text(encoding="utf-8")
    dedupe_text = DEDUPE_SQL.read_text(encoding="utf-8")

    require_contains(approval_text, "mes-edhr-approval-v1", APPROVAL_SQL.name, errors)
    require_contains(approval_text, "flowable:candidateParam=\"1\"", APPROVAL_SQL.name, errors)
    require_contains(approval_text, "'1'", APPROVAL_SQL.name, errors)
    require_contains(approval_text, "bpm_process_definition_info", APPROVAL_SQL.name, errors)
    require_contains(approval_text, "act_re_procdef", APPROVAL_SQL.name, errors)

    require_contains(dedupe_text, "work_order_id = 903245", DEDUPE_SQL.name, errors)
    require_contains(dedupe_text, "route_id = 900022", DEDUPE_SQL.name, errors)
    require_contains(dedupe_text, "deleted = b'1'", DEDUPE_SQL.name, errors)

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    print("PASS: eDHR admin approval seed SQL guards are present")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
