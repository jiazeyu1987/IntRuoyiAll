from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "showroom" / "20260616_showroom_company_honor_hall.sql"


def read_sql() -> str:
    assert SQL_PATH.exists()
    return SQL_PATH.read_text(encoding="utf-8")


def test_company_honor_hall_script_has_target_tenant_guard() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260613_showroom_award_and_hall_item_schema; type=data; riskLevel=medium"
    )
    assert "SET @showroom_company_honor_target_tenant_id = IFNULL(@showroom_company_honor_target_tenant_id, 0);" in text
    assert text.count("@showroom_company_honor_target_tenant_id = 0") >= 2
    assert text.count("`tenant_id` = @showroom_company_honor_target_tenant_id") >= 2
    assert "CREATE TEMPORARY TABLE `tmp_showroom_company_honor_tenants` AS" in text
    assert text.count("JOIN `tmp_showroom_company_honor_tenants`") >= 5
    assert "FROM `showroom_award`" in text
    assert "`current_revision_id` IS NOT NULL" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "SHOWROOM_ENTERPRISE_HONOR_AWARD_MISSING" in text


def test_company_honor_hall_script_upserts_split_halls() -> None:
    text = read_sql()

    required_snippets = [
        "UPDATE `showroom_hall` h",
        "WHERE h.`hall_code` IN ('hall_09', 'hall_10')",
        "INSERT INTO `showroom_hall`",
        "'hall_09'",
        "'企业荣誉展柜1'",
        "'Corporate Honors Showcase 1'",
        "'企业荣誉展柜1集中呈现公司荣誉体系的第一组奖项，涵盖社会贡献、总部认定、专精特新、创新总部、商业单项冠军、高新技术、知识产权与质量体系等代表性成果，展示企业在规范经营、技术创新和行业认可方面的持续积累。'",
        "'Corporate Honors Showcase 1 presents the first group of enterprise awards, covering social contribution, headquarters recognition, specialized and innovative enterprise honors, innovation headquarters, single-champion recognition, high-tech capability, intellectual property, and quality-system achievements. It highlights the company''s sustained progress in compliant operations, technology innovation, and industry recognition.'",
        "'hall_10'",
        "'企业荣誉展柜2'",
        "'Corporate Honors Showcase 2'",
        "'企业荣誉展柜2集中呈现公司荣誉体系的第二组奖项，延续展示品牌影响力、技术创新、产品质量、行业资质、社会责任和市场信任等成果，承接 Excel 奖项页签后半部分奖项信息，体现企业长期稳健发展的综合实力。'",
        "'Corporate Honors Showcase 2 presents the second group of enterprise awards, continuing the record of brand influence, technology innovation, product quality, industry qualifications, social responsibility, and market trust. It carries the latter half of the Excel Awards sheet and reflects the company''s sustained and balanced growth.'",
        "LEFT JOIN `showroom_hall` h",
        "AND h.`hall_code` = d.`hall_code`",
        "WHERE h.`id` IS NULL",
    ]
    for snippet in required_snippets:
        assert snippet in text


def test_company_honor_hall_script_keeps_awards_only_in_split_honor_halls() -> None:
    text = read_sql()

    required_snippets = [
        "DELETE hp",
        "FROM `showroom_hall_product` hp",
        "h.`hall_code` IN ('hall_09', 'hall_10')",
        "DELETE hi",
        "FROM `showroom_hall_item` hi",
        "WHERE hi.`item_type` = 'AWARD'",
        "AND h.`hall_code` NOT IN ('hall_09', 'hall_10')",
        "WHERE h.`hall_code` = 'company_honor'",
        "h.`deleted` = b'1'",
        "INSERT INTO `showroom_hall_item`",
        "'AWARD'",
        "CASE WHEN ranked.`award_sort_no` <= CEIL(ranked.`total_count` / 2)",
        "ROW_NUMBER() OVER (PARTITION BY ranked.`tenant_id`, ranked.`target_hall_code` ORDER BY ranked.`award_sort_no`) AS `display_order`",
    ]
    for snippet in required_snippets:
        assert snippet in text


def test_company_honor_hall_script_rebuilds_complete_layout_with_temp_tables() -> None:
    text = read_sql()

    required_snippets = [
        "DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_awards`",
        "CREATE TEMPORARY TABLE `tmp_showroom_company_honor_awards` AS",
        "`target_hall_code`",
        "ORDER BY a.`award_code`, a.`id`",
        "DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_layout`",
        "CREATE TEMPORARY TABLE `tmp_showroom_company_honor_layout` AS",
        "`layout_x`",
        "`layout_y`",
        "`layout_width`",
        "`layout_height`",
        "DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_layout`;",
        "DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_awards`;",
    ]
    for snippet in required_snippets:
        assert snippet in text


def test_company_honor_hall_script_does_not_touch_system_or_file_tables() -> None:
    text = read_sql()

    forbidden_snippets = [
        "`system_",
        "`infra_file",
        "`bpm_",
        "`ai_",
        "TRUNCATE",
        "DROP TABLE `showroom_",
    ]
    for snippet in forbidden_snippets:
        assert snippet not in text
