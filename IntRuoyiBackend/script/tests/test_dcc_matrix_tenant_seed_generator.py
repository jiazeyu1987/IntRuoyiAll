import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_matrix_tenant_seed_generator.py"


def test_generator_builds_tenant_scoped_idempotent_seed(tmp_path):
    output = tmp_path / "seed.sql"
    cp = subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), "--tenant-id", "122", "--output-sql", str(output)],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    assert cp.returncode == 0, cp.stderr
    assert "categories=59" in cp.stdout
    sql = output.read_text(encoding="utf-8")
    assert "tenant 122" in sql
    assert "tenant_id = tmp.tenant_id AND existing.code = tmp.code" in sql
    assert "INSERT INTO dcc_file_category" in sql
    assert "INSERT INTO system_role" in sql
    assert "COLLATE=utf8mb4_unicode_ci" in sql
    assert "tenant_id bigint NOT NULL" in sql
    assert "COMMIT;" in sql


def test_generator_outputs_encoding_safe_repair_sql(tmp_path):
    output = tmp_path / "seed.sql"
    cp = subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), "--tenant-id", "122", "--output-sql", str(output)],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    assert cp.returncode == 0, cp.stderr
    sql = output.read_text(encoding="utf-8")
    assert "CONVERT(UNHEX(" in sql
    assert "USING utf8mb4) COLLATE utf8mb4_unicode_ci" in sql
    assert "UPDATE dcc_file_category existing" in sql
    assert "UPDATE system_role existing" in sql
    assert "市场调研报告" not in sql
    assert "DCC矩阵" not in sql
    assert "部门主管及以上" not in sql
