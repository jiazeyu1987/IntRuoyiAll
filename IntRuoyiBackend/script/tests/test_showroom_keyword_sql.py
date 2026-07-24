from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
RUNTIME_SCHEMA_SQL = REPO_ROOT / "sql" / "mysql" / "20260626_showroom_keyword_schema_seed_runtime.sql"

KEYWORD_ROWS = [
    ("上海瑛泰医疗器械自动化有限公司", "Shanghai INT Medical Instruments Automation Co., Ltd."),
    ("珠海德瑞医疗器械有限公司", "Zhuhai Derui Medical Instruments Co., Ltd."),
    ("上海璞康医疗器械有限公司", "Shanghai Pukon Medical Instruments Co., Ltd."),
    ("上海七木医疗器械有限公司", "Shanghai Qimu Medical Instruments Co., Ltd."),
    ("上海璞慧医疗器械有限公司", "Shanghai Puhui Medical Instruments Co., Ltd."),
    ("上海翰凌医疗器械有限公司", "Shanghai Healing Medical Instruments Co., Ltd."),
    ("香港瑛泰医疗器械有限公司", "Hongkong INT Medical Instruments Company Limited"),
    ("上海璞镁医疗器械有限公司", "Shanghai Pumei Medical Instruments Co., Ltd."),
    ("山东瑛泰医疗器械有限公司", "Shandong INT Medical Instruments Co., Ltd."),
    ("上海璞霖医疗器械有限公司", "Shanghai Pulin Medical Instruments Co., Ltd."),
    ("上海璞跃医疗器械有限公司", "Shanghai Puyue Medical Instruments Co., Ltd."),
    ("上海益凯医疗器械有限公司", "Shanghai Yikai Medical Instruments Co., Ltd."),
    ("上海瑛泰生物科技有限公司", "Shanghai INT Biotechnology Co., Ltd."),
    ("上海瑛泰璞润医疗器械有限公司", "Shanghai INT Pureray Medical Instruments Co., Ltd."),
    ("山东瑛盛新材料有限公司", "Shandong Insant New Materials Co., Ltd."),
    ("珠海璞跃医疗器械有限公司", "Zhuhai Puyue Medical Instruments Co., Ltd."),
    ("上海泰嘉瑞医疗科技有限公司", "Shanghai Techarray Medical Technology Co., Ltd."),
    ("山东瑛泰医疗科技有限公司", "Shandong INT Medical Technology Co., Ltd."),
    ("上海瑛泰昇活商贸有限公司", "Shanghai INT Life Co., Ltd."),
    ("珠海璞瑞智能制造有限公司", "Zhuhai Purui Intelligent Manufacturing Co., Ltd."),
    ("上海瑛泰投资管理有限公司", "Shanghai INT Investment Management Co., Ltd."),
    ("上海瑛泰实业有限公司", "Shanghai INT Property Management Co., Ltd."),
    ("杭州唯强医疗科技有限公司", "Hangzhou Endonom Medtech Co., Ltd."),
    ("杭州唯淅医疗科技有限公司", "Hangzhou Weixi Medical Technology Co., Ltd."),
    ("上海瑛泰企业管理有限公司", "Shanghai INT Enterprise Management Co., Ltd."),
    ("上海吉尔邦医学科技有限公司", "Shanghai GelBond Medtech Co., Ltd."),
    ("上海瑛泰医疗科技有限公司", "Shanghai INT Medical Technology Co., Ltd."),
]

HEALING_GLOSSARY_ROWS = [
    ("翰凌", "Healing"),
]

BU_KEYWORD_ROWS = [
    ("心脏电生理BU", "Cardiac Electrophysiology BU"),
    ("神经血管BU", "Neurovascular BU"),
    ("心血管BU", "Cardiovascular BU"),
    ("结构心BU", "Structural Heart BU"),
    ("外周血管BU", "Peripheral Vascular BU"),
    ("非血管BU", "Non-vascular BU"),
]

SHOWROOM_BU_SEED_SQL = REPO_ROOT / "sql" / "showroom" / "20260626_showroom_keyword_bu_seed.sql"
RUNTIME_BU_SEED_SQL = REPO_ROOT / "sql" / "mysql" / "20260626_showroom_keyword_bu_seed_runtime.sql"
SHOWROOM_HEALING_SEED_SQL = REPO_ROOT / "sql" / "showroom" / "20260626_showroom_keyword_healing_seed.sql"
RUNTIME_HEALING_SEED_SQL = REPO_ROOT / "sql" / "mysql" / "20260626_showroom_keyword_healing_seed_runtime.sql"


def test_showroom_keyword_schema_seed_declares_table_constraints_and_rows() -> None:
    sql_path = REPO_ROOT / "sql" / "showroom" / "20260625_showroom_keyword_schema_seed.sql"
    assert sql_path.exists()
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "CREATE TABLE IF NOT EXISTS `showroom_keyword`",
        "`id` bigint NOT NULL AUTO_INCREMENT",
        "`name_zh` varchar(255) NOT NULL",
        "`name_en` varchar(255) NOT NULL",
        "`creator` varchar(64) DEFAULT ''",
        "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
        "`deleted` bit(1) NOT NULL DEFAULT b'0'",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
        "UNIQUE KEY `uk_showroom_keyword_tenant_name_zh` (`tenant_id`, `name_zh`)",
        "FROM `system_role_menu` AS `menu_scope`",
        "WHERE `menu_scope`.`menu_id` = 980102",
        "ORDER BY `tenant_id` ASC, `seed_order` ASC",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert text.count("INSERT INTO `showroom_keyword`") == 1
    for name_zh, name_en in KEYWORD_ROWS:
        assert name_zh in text
        assert name_en in text


def test_showroom_menu_seed_places_keyword_between_product_and_prompt() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    product_row = "(980102, '产品管理'"
    keyword_row = "(980122, '关键词中英对照'"
    prompt_row = "(980119, '提示管理'"
    hall_row = "(980103, '展厅管理'"

    assert product_row in text
    assert keyword_row in text
    assert prompt_row in text
    assert hall_row in text
    assert text.index(product_row) < text.index(keyword_row) < text.index(prompt_row) < text.index(hall_row)
    assert "'keyword'" in text
    assert "'ShowroomAdminKeyword'" in text


def test_showroom_keyword_menu_visibility_patch_copies_product_role_bindings() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260625_showroom_keyword_menu_visibility.sql"
    assert sql_path.exists()
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "-- release-migration:",
        "20260625_showroom_keyword_menu_visibility",
        "980122",
        "关键词中英对照",
        "keyword",
        "ShowroomAdminKeyword",
        "WHERE `source`.`menu_id` = 980102",
        "AND `target_menu`.`parent_id` = 980100",
        "AND `target_menu`.`path` = 'keyword'",
        "AND `target_menu`.`component_name` = 'ShowroomAdminKeyword'",
        "`system_role_menu`",
        "`tenant_id`",
        "NOT EXISTS",
        "showroom-menu-fix",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_showroom_keyword_menu_id_avoids_known_runtime_conflict() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    visibility_path = REPO_ROOT / "sql" / "mysql" / "20260625_showroom_keyword_menu_visibility.sql"
    seed_text = seed_path.read_text(encoding="utf-8")
    visibility_text = visibility_path.read_text(encoding="utf-8")

    assert "(980120, '关键词中英对照'" not in seed_text
    assert "980120,\n    '关键词中英对照'" not in visibility_text
    assert "(980122, '关键词中英对照'" in seed_text
    assert "980122" in visibility_text


def test_showroom_keyword_has_release_scanned_runtime_schema_migration() -> None:
    assert RUNTIME_SCHEMA_SQL.exists()
    text = RUNTIME_SCHEMA_SQL.read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260618_showroom_publicity_role_menu_scope; type=schema; riskLevel=medium"
    )
    required_snippets = [
        "CREATE TABLE IF NOT EXISTS `showroom_keyword`",
        "UNIQUE KEY `uk_showroom_keyword_tenant_name_zh` (`tenant_id`, `name_zh`)",
        "INSERT INTO `showroom_keyword`",
        "WHERE `menu_scope`.`menu_id` = 980102",
        "showroom-keyword-seed",
        "上海瑛泰医疗器械自动化有限公司",
        "Shanghai INT Medical Instruments Automation Co., Ltd.",
    ]
    for snippet in required_snippets:
        assert snippet in text


def test_showroom_keyword_showroom_seed_extension_contains_bu_rows() -> None:
    assert SHOWROOM_BU_SEED_SQL.exists()
    text = SHOWROOM_BU_SEED_SQL.read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260625_showroom_keyword_schema_seed; type=seed; riskLevel=low"
    )
    assert "INSERT INTO `showroom_keyword`" in text
    assert "showroom-keyword-bu-seed" in text
    for name_zh, name_en in BU_KEYWORD_ROWS:
        assert name_zh in text
        assert name_en in text


def test_showroom_keyword_runtime_seed_extension_contains_bu_rows() -> None:
    assert RUNTIME_BU_SEED_SQL.exists()
    text = RUNTIME_BU_SEED_SQL.read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260626_showroom_keyword_schema_seed_runtime; type=seed; riskLevel=low"
    )
    assert "INSERT INTO `showroom_keyword`" in text
    assert "showroom-keyword-bu-seed" in text
    for name_zh, name_en in BU_KEYWORD_ROWS:
        assert name_zh in text
        assert name_en in text


def test_showroom_keyword_incremental_seed_contains_healing_subterm() -> None:
    assert SHOWROOM_HEALING_SEED_SQL.exists()
    showroom_text = SHOWROOM_HEALING_SEED_SQL.read_text(encoding="utf-8")
    assert showroom_text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260626_showroom_keyword_bu_seed; type=seed; riskLevel=low"
    )
    assert "INSERT INTO `showroom_keyword`" in showroom_text
    assert "showroom-keyword-healing-seed" in showroom_text
    for name_zh, name_en in HEALING_GLOSSARY_ROWS:
        assert name_zh in showroom_text
        assert name_en in showroom_text

    assert RUNTIME_HEALING_SEED_SQL.exists()
    runtime_text = RUNTIME_HEALING_SEED_SQL.read_text(encoding="utf-8")
    assert runtime_text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260626_showroom_keyword_bu_seed_runtime; type=seed; riskLevel=low"
    )
    assert "INSERT INTO `showroom_keyword`" in runtime_text
    assert "showroom-keyword-healing-seed" in runtime_text
    for name_zh, name_en in HEALING_GLOSSARY_ROWS:
        assert name_zh in runtime_text
        assert name_en in runtime_text
