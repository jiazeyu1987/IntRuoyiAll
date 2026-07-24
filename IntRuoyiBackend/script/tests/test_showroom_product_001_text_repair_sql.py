from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_product_001_text_repair_sql_targets_expected_product_and_fields() -> None:
    script_path = (
        REPO_ROOT / "sql" / "showroom" / "20260521_showroom_product_001_text_repair.sql"
    )
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "product_code = 'product_001'",
        "good.revision_no = 2",
        "bad.revision_no IN (3, 4, 5, 6)",
        "bad.revision_no = 3",
        "bad.registration_certificate LIKE '%?%'",
        "bad.indication_content LIKE '%?%'",
        "bad.name_cn LIKE '%?%'",
        "bad.registration_certificate = good.registration_certificate",
        "bad.indication_content = good.indication_content",
        "bad.name_cn = good.name_cn",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "DELETE FROM showroom_product_revision" not in text
    assert "DELETE FROM showroom_product" not in text
