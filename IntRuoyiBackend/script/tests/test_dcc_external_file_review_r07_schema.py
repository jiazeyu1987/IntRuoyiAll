from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_r07_external_review_schema_and_process_key_are_declared() -> None:
    schema = (REPO_ROOT / "sql" / "mysql" / "20260527_dcc_external_file_review.sql").read_text(encoding="utf-8")
    assert "CREATE TABLE IF NOT EXISTS `dcc_external_file_review`" in schema
    for column in [
        "controlled_file_id",
        "external_source",
        "external_owner",
        "review_reason",
        "participant_user_ids",
        "review_conclusion",
        "conclusion_comment",
        "output_file_id",
        "closed_time",
    ]:
        assert f"`{column}`" in schema
    assert "dcc-external-file-review" in schema
    assert "dcc-controlled-file-approval" not in schema
