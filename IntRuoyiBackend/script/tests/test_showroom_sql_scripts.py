from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_v1_schema_declares_required_business_tables_only() -> None:
    schema_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_v1_schema.sql"
    text = schema_path.read_text(encoding="utf-8")

    required_tables = [
        "showroom_company",
        "showroom_company_revision",
        "showroom_product",
        "showroom_product_revision",
        "showroom_product_revision_relation",
        "showroom_hall",
        "showroom_hall_product",
        "showroom_change_request",
        "showroom_change_request_item",
        "showroom_version_audit",
        "showroom_field_assignment",
        "showroom_product_comment",
        "showroom_narration_version",
        "showroom_preview_asset_version",
        "showroom_image_prompt_version",
    ]

    for table in required_tables:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in text

    assert "CREATE TABLE IF NOT EXISTS `system_notify_" not in text
    assert "CREATE TABLE IF NOT EXISTS `bpm_" not in text
    assert "CREATE TABLE IF NOT EXISTS `infra_file" not in text
    assert "CREATE TABLE IF NOT EXISTS `ai_knowledge" not in text


def test_showroom_v1_schema_keeps_foundation_constraints() -> None:
    schema_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_v1_schema.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "UNIQUE KEY `uk_showroom_company_code` (`tenant_id`, `company_code`)" in text
    assert "UNIQUE KEY `uk_showroom_product_code` (`tenant_id`, `product_code`)" in text
    assert "UNIQUE KEY `uk_showroom_hall_code` (`tenant_id`, `hall_code`)" in text
    assert "UNIQUE KEY `uk_showroom_hall_product` (`tenant_id`, `hall_id`, `product_id`)" in text
    assert "KEY `idx_showroom_change_request_process` (`process_instance_id`)" in text
    assert "`notify_message_id` bigint NOT NULL" in text
    assert "`name_cn` varchar(255) DEFAULT NULL" in text
    assert "`name_en` varchar(255) DEFAULT NULL" in text
    assert "`target_market` text DEFAULT NULL" in text
    assert "`target_market` varchar(255)" not in text
    assert text.count("`cover_image` text DEFAULT NULL") >= 2
    assert "`registration_certificate` text DEFAULT NULL" in text
    assert "`clinical_effect` text DEFAULT NULL" in text
    assert "`fim_status` varchar(64) DEFAULT NULL" in text
    assert "`voice` varchar(64) DEFAULT NULL" in text
    assert "`submitter_dept_id` bigint DEFAULT NULL" in text
    assert "CREATE TABLE IF NOT EXISTS `showroom_product_cover_batch_task`" in text
    assert "CREATE TABLE IF NOT EXISTS `showroom_product_cover_batch_task_item`" in text
    assert "CREATE TABLE IF NOT EXISTS `showroom_image_prompt_version`" in text


def test_showroom_hall_product_canvas_layout_migration_adds_normalized_rect_columns() -> None:
    script_path = REPO_ROOT / "sql" / "showroom" / "20260606_showroom_hall_product_canvas_layout.sql"
    assert script_path.exists()
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "ALTER TABLE `showroom_hall_product`",
        "information_schema.COLUMNS",
        "TABLE_SCHEMA = DATABASE()",
        "PREPARE showroom_hall_product_layout_x_stmt",
        "PREPARE showroom_hall_product_layout_y_stmt",
        "PREPARE showroom_hall_product_layout_width_stmt",
        "PREPARE showroom_hall_product_layout_height_stmt",
        "`layout_x` decimal(8,6) DEFAULT NULL",
        "`layout_y` decimal(8,6) DEFAULT NULL",
        "`layout_width` decimal(8,6) DEFAULT NULL",
        "`layout_height` decimal(8,6) DEFAULT NULL",
        "Normalized canvas rectangle",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "ADD COLUMN IF NOT EXISTS" not in text


def test_showroom_hall_canvas_background_migration_adds_nullable_auxiliary_url() -> None:
    script_path = REPO_ROOT / "sql" / "showroom" / "20260615_showroom_hall_canvas_background.sql"
    assert script_path.exists()
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "INFORMATION_SCHEMA.COLUMNS",
        "TABLE_SCHEMA = DATABASE()",
        "TABLE_NAME = 'showroom_hall'",
        "COLUMN_NAME = 'canvas_background_image_url'",
        "ALTER TABLE `showroom_hall` ADD COLUMN `canvas_background_image_url` varchar(1024) DEFAULT NULL",
        "AFTER `description_en`",
        "PREPARE stmt FROM @ddl",
        "DEALLOCATE PREPARE stmt",
    ]

    for snippet in required_snippets:
        assert snippet in text

    upper_text = text.upper()
    assert "ADD COLUMN IF NOT EXISTS" not in upper_text
    assert "DROP TABLE" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
    assert "DELETE FROM" not in upper_text
    assert "UPDATE `SHOWROOM_HALL`" not in upper_text


def test_showroom_cover_batch_task_mysql_script_declares_required_tables_and_indexes() -> None:
    script_path = REPO_ROOT / "sql" / "mysql" / "20260522_showroom_product_cover_batch_task.sql"
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "CREATE TABLE IF NOT EXISTS `showroom_product_cover_batch_task`",
        "CREATE TABLE IF NOT EXISTS `showroom_product_cover_batch_task_item`",
        "KEY `idx_showroom_cover_batch_task_status` (`status`, `next_check_at`)",
        "KEY `idx_showroom_cover_batch_task_item_status` (`task_id`, `status`)",
        "UNIQUE KEY `uk_showroom_cover_batch_task_item` (`task_id`, `product_id`)",
        "`cover_generation_mode` varchar(32) NOT NULL",
        "`prompt_version_id` bigint DEFAULT NULL",
        "`remaining_pending_count` int NOT NULL DEFAULT 0",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_showroom_release_delivery_schema_declares_release_tables() -> None:
    script_path = REPO_ROOT / "sql" / "showroom" / "20260523_showroom_release_delivery_v1_schema.sql"
    text = script_path.read_text(encoding="utf-8")

    required_tables = [
        "showroom_public_site_binding",
        "showroom_release",
        "showroom_release_source_snapshot",
        "showroom_release_document",
        "showroom_release_asset",
        "showroom_release_asset_ref",
        "showroom_release_pointer",
        "showroom_release_legacy_projection",
        "showroom_release_tombstone",
    ]

    for table in required_tables:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in text

    required_constraints = [
        "UNIQUE KEY `uk_showroom_release_id` (`release_id`)",
        "UNIQUE KEY `uk_showroom_release_snapshot_release` (`release_id`)",
        "UNIQUE KEY `uk_showroom_release_document` (`release_id`, `document_id`)",
        "UNIQUE KEY `uk_showroom_release_asset` (`tenant_id`, `site_key`, `stage`, `asset_id`, `content_hash`)",
        "UNIQUE KEY `uk_showroom_release_pointer_scope` (`tenant_id`, `site_key`, `stage`, `pointer_key`)",
        "UNIQUE KEY `uk_showroom_release_legacy_projection_release` (`release_id`)",
        "UNIQUE KEY `uk_showroom_release_tombstone` (`tenant_id`, `site_key`, `stage`, `resource_type`, `resource_key`)",
        "UNIQUE KEY `uk_showroom_public_site_stage` (`site_key`, `stage`)",
        "`binary_content` longblob NOT NULL",
    ]

    for constraint in required_constraints:
        assert constraint in text


def test_showroom_tenant_isolation_migration_assigns_existing_mutable_rows_and_rekeys_uniques() -> None:
    script_path = REPO_ROOT / "sql" / "showroom" / "20260527_showroom_tenant_isolation_constraints.sql"
    text = script_path.read_text(encoding="utf-8")

    required_updates = [
        "UPDATE `showroom_company` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
        "UPDATE `showroom_product` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
        "UPDATE `showroom_hall` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
        "UPDATE `showroom_narration_version` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
        "UPDATE `showroom_preview_asset_version` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
        "UPDATE `showroom_version_bundle` SET `tenant_id` = 1 WHERE `tenant_id` = 0;",
    ]
    required_keys = [
        "ADD UNIQUE KEY `uk_showroom_company_code` (`tenant_id`, `company_code`)",
        "ADD UNIQUE KEY `uk_showroom_product_code` (`tenant_id`, `product_code`)",
        "ADD UNIQUE KEY `uk_showroom_hall_code` (`tenant_id`, `hall_code`)",
        "ADD UNIQUE KEY `uk_showroom_hall_product` (`tenant_id`, `hall_id`, `product_id`)",
        "ADD UNIQUE KEY `uk_showroom_narration_version_no`",
        "(`tenant_id`, `target_type`, `target_id`, `audience_type`, `language`, `version_no`)",
        "ADD UNIQUE KEY `uk_showroom_preview_asset_version_no`",
        "(`tenant_id`, `target_type`, `target_id`, `version_no`)",
        "ADD UNIQUE KEY `uk_showroom_version_bundle_revision`",
        "(`tenant_id`, `target_type`, `target_id`, `revision_id`)",
    ]

    for snippet in required_updates + required_keys:
        assert snippet in text

    assert "Runtime code must not copy tenant 1 rows or read tenant_id = 0 as a fallback." in text


def test_showroom_menu_seed_declares_manageable_backoffice_tabs() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    required_snippets = [
        "INSERT INTO `system_menu`",
        "'展厅'",
        "'showroom'",
        "'公司信息'",
        "'产品管理'",
        "'展厅管理'",
        "'审批中心'",
        "'版本历史'",
        "'补充指派'",
        "'产品讨论'",
        "'提示管理'",
        "'讲解工作台'",
        "'showroom-admin/index'",
        "'ShowroomAdminCompany'",
        "'ShowroomAdminProduct'",
        "'ShowroomAdminHall'",
        "'ShowroomAdminApproval'",
        "'ShowroomAdminHistory'",
        "'ShowroomAdminAssignment'",
        "'ShowroomAdminDiscussion'",
        "'ShowroomAdminPrompt'",
        "'ShowroomAdminNarration'",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "INSERT INTO system_role_menu" not in text


def test_showroom_menu_seed_declares_manageable_frontstage_screen_tabs() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    required_snippets = [
        "'前台大屏'",
        "'display/screen/home'",
        "'ShowroomDisplayScreenHome'",
        "'大屏公司'",
        "'display/screen/company'",
        "'ShowroomDisplayScreenCompany'",
        "'大屏展柜'",
        "'display/screen/hall/:hallId(\\\\d+)'",
        "'ShowroomDisplayScreenHall'",
        "'大屏产品详情'",
        "'display/screen/product/:productId(\\\\d+)'",
        "'ShowroomDisplayScreenProduct'",
        "'大屏设置'",
        "'display/screen/settings'",
        "'ShowroomDisplayScreenSettings'",
        "'大屏讲解播放'",
        "'display/screen/narration'",
        "'ShowroomDisplayScreenNarration'",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "INSERT INTO system_role_menu" not in text
    assert "'display/pad/home'" not in text
    assert "'display/mobile/home'" not in text


def test_showroom_notify_template_seed_declares_pending_published_and_rejected_templates() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260520_showroom_notify_template_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    required_snippets = [
        "SHOWROOM_APPROVAL_PENDING",
        "SHOWROOM_APPROVAL_PUBLISHED",
        "SHOWROOM_APPROVAL_REJECTED",
        "SHOWROOM_ASSIGNMENT",
        "展厅审批待办通知",
        "展厅发布完成通知",
        "展厅审批驳回通知",
        "展厅补充指派通知",
        "{targetTypeText}",
        "{targetName}",
        "{approvalStage}",
        "{rejectionReason}",
        "{fieldCode}",
        "{targetType}",
        "{targetId}",
        "{assignedBy}",
        "showroom approval pending notify seed",
        "showroom approval published notify seed",
        "showroom approval rejected notify seed",
        "showroom assignment notify seed",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "INSERT INTO `system_role_menu`" not in text


def test_showroom_whole_assignment_test_tenant_seed_declares_editor_user_and_role_binding() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260520_showroom_assignment_test_tenant_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    required_snippets = [
        "展厅编辑",
        "'EDITOR'",
        "'showroomeditor'",
        "'showroom whole assignment verification seed'",
        "'tenant_admin'",
        "INSERT INTO `system_user_role`",
        "system_role",
        "system_users",
        "system_user_role",
        "$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_showroom_hall_name_suffix_patch_updates_only_name_suffixes() -> None:
    script_path = REPO_ROOT / "sql" / "mysql" / "20260525_showroom_hall_name_suffix_showcase.sql"
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "UPDATE `showroom_hall`",
        "SET `name` = CONCAT(",
        "CHAR_LENGTH('展厅')",
        "'展柜'",
        "RIGHT(`name`, CHAR_LENGTH('展厅')) = '展厅'",
        "SET `name_en` = CONCAT(",
        "CHAR_LENGTH('Hall')",
        "'Showcase'",
        "RIGHT(`name_en`, CHAR_LENGTH('Hall')) = 'Hall'",
        "`deleted` = b'0'",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "`description`" not in text
    assert "`description_en`" not in text


def test_showroom_product_target_market_migration_widens_column_without_data_rewrite() -> None:
    script_path = REPO_ROOT / "sql" / "showroom" / "20260703_showroom_product_target_market_text.sql"
    assert script_path.exists()
    text = script_path.read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260519_showroom_v1_schema; type=schema; riskLevel=medium"
    )
    assert "ALTER TABLE `showroom_product_revision`" in text
    assert "MODIFY COLUMN `target_market` text DEFAULT NULL" in text
    assert "UPDATE " not in text.upper()
    assert "DELETE " not in text.upper()
    assert "DROP " not in text.upper()


def test_showroom_hall_description_patch_populates_all_seeded_showcases_without_overwriting_custom_copy() -> None:
    script_path = REPO_ROOT / "sql" / "mysql" / "20260601_showroom_hall_descriptions.sql"
    text = script_path.read_text(encoding="utf-8")

    for hall_code in [f"hall_{index:02d}" for index in range(1, 9)]:
        assert f"`hall_code` = '{hall_code}'" in text

    required_snippets = [
        "UPDATE `showroom_hall`",
        "`description` = CASE",
        "`description_en` = CASE",
        "TRIM(`description`) = ''",
        "TRIM(`description_en`) = ''",
        "ELSE `description`",
        "ELSE `description_en`",
        "`deleted` = b'0'",
        "心内介植入相关产品",
        "cardiac interventional implant products",
        "医疗器械标准件与基础组件",
        "standard medical device components",
    ]

    for snippet in required_snippets:
        assert snippet in text
