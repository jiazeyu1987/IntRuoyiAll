from pathlib import Path

from script.release.release_migration_manifest import METADATA_PATTERN


def test_batch_record_cell_link_rule_has_release_metadata():
    repo_root = Path(__file__).resolve().parents[2]
    migration = repo_root / "sql" / "mysql" / "20260711_mes_batch_record_cell_link_rule.sql"

    first_line = migration.read_text(encoding="utf-8").splitlines()[0].strip()

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )


def test_release_migration_depends_on_uses_migration_ids_without_sql_suffix():
    repo_root = Path(__file__).resolve().parents[2]
    sql_root = repo_root / "sql" / "mysql"

    offenders: list[str] = []
    for migration in sorted(sql_root.glob("20*.sql")):
        text = migration.read_text(encoding="utf-8")
        match = METADATA_PATTERN.search(text)
        if not match:
            continue
        metadata = {}
        for segment in match.group(1).split(";"):
            if "=" not in segment:
                continue
            key, value = [part.strip() for part in segment.split("=", 1)]
            metadata[key] = value
        depends_on = metadata.get("dependsOn", "")
        for dependency in [item.strip() for item in depends_on.split(",") if item.strip()]:
            if dependency.endswith(".sql"):
                offenders.append(f"{migration.name}: {dependency}")

    assert offenders == []
