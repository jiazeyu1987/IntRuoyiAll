from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]

SQL_CONTRACTS = [
    {
        "sql": "20260717_bpm_form_center.sql",
        "test": "test_bpm_form_center_sql.py",
        "requires_signal": True,
        "literals": [
            "bpm_form_action_policy",
            "bpm_form_action_instance",
            "bpm_form_task_permission",
            "bpm_form_effect_execution",
            "ensure_bpm_form_center_menu",
        ],
    },
    {
        "sql": "20260722_form_center_business_action_page_retire.sql",
        "test": "test_form_center_business_action_page_retire_sql.py",
        "requires_signal": True,
        "literals": [
            "605071209",
            "system_role_menu",
            "system_tenant_package",
            "JSON_CONTAINS",
        ],
    },
    {
        "sql": "20260719_business_approval_policy.sql",
        "test": "test_business_approval_policy_sql_contract.py",
        "requires_signal": False,
        "literals": [
            "bpm_business_approval_policy",
            "bpm_business_approval_request",
            "BPM_REQUIRED",
            "DIRECT",
            "effect_executor_code",
        ],
    },
    {
        "sql": "20260719_business_approval_policy_menu.sql",
        "test": "test_business_approval_policy_menu_sql.py",
        "requires_signal": True,
        "literals": [
            "business-approval-policy",
            "bpm:business-approval-policy:query",
            "BpmBusinessApprovalPolicy",
            "system_tenant_package",
        ],
    },
    {
        "sql": "20260719_dcc_upload_form_policy_seed.sql",
        "test": "test_dcc_upload_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'DCC'",
            "'CONTROLLED_FILE'",
            "'UPLOAD'",
            "'DRAFT'",
            "'DCC_UPLOAD'",
            "dcc-controlled-file-approval",
        ],
    },
    {
        "sql": "20260719_dcc_obsolete_form_policy_seed.sql",
        "test": "test_dcc_obsolete_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'DCC'",
            "'CONTROLLED_FILE'",
            "'OBSOLETE'",
            "'ACTIVE'",
            "'DCC_OBSOLETE'",
        ],
    },
    {
        "sql": "20260720_dcc_obsolete_approval_bpm_seed.sql",
        "test": "test_dcc_obsolete_approval_bpm_seed.py",
        "requires_signal": False,
        "literals": [
            "dcc-controlled-file-obsolete-approval",
            "DCC_OBSOLETE",
            "tenant-122",
            "bpm_process_definition_info",
        ],
    },
    {
        "sql": "20260720_dcc_publish_form_policy_seed.sql",
        "test": "test_dcc_publish_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'DCC'",
            "'CONTROLLED_FILE'",
            "'PUBLISH'",
            "'READY_TO_PUBLISH'",
            "'DCC_PUBLISH'",
        ],
    },
    {
        "sql": "20260720_edhr_release_void_form_policy_seed.sql",
        "test": "test_edhr_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'MES'",
            "'EDHR_BATCH_EXECUTION'",
            "'RELEASE'",
            "'EDHR_RELEASE'",
            "'VOID'",
            "'EDHR_BATCH_VOID'",
            "mes-edhr-approval-v1",
            "mes-edhr-batch-execution-void-v1",
        ],
    },
    {
        "sql": "20260722_edhr_release_form_policy_retire.sql",
        "test": "test_edhr_release_form_policy_retire_sql.py",
        "requires_signal": True,
        "literals": [
            "'MES'",
            "'EDHR_BATCH_EXECUTION'",
            "'RELEASE'",
            "'PRECHECK_PASSED'",
            "'EDHR_RELEASE'",
            "'RETIRED'",
        ],
    },
    {
        "sql": "20260720_mes_schedule_replan_approval_bpm_seed.sql",
        "test": "test_mes_schedule_replan_approval_bpm_seed.py",
        "requires_signal": False,
        "literals": [
            "mes-schedule-replan-approval-v1",
            "scheduleReplanApprove",
            "<flowable:candidateStrategy>10</flowable:candidateStrategy>",
            "1 AS tenant_id",
            "122 AS tenant_id",
            "bpm_process_definition_info",
        ],
    },
    {
        "sql": "20260720_mes_schedule_replan_form_policy_seed.sql",
        "test": "test_mes_schedule_replan_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'MES'",
            "'SCHEDULE_REPLAN_SCOPE'",
            "'REPLAN'",
            "'READY'",
            "'MES_SCHEDULE_REPLAN'",
            "'mes-schedule-replan-approval-v1'",
        ],
    },
    {
        "sql": "20260721_mes_schedule_replan_approval_retire.sql",
        "test": "test_mes_schedule_replan_form_policy_seed_sql.py",
        "requires_signal": True,
        "literals": [
            "'MES'",
            "'SCHEDULE_REPLAN_SCOPE'",
            "'REPLAN'",
            "'READY'",
            "'MES_SCHEDULE_REPLAN'",
            "'RETIRED'",
        ],
    },
    {
        "sql": "20260721_form_template_upgrade_bpm_seed.sql",
        "test": "test_form_template_upgrade_bpm_seed.py",
        "requires_signal": True,
        "literals": [
            "'FORM_CENTER'",
            "'FORM_TEMPLATE'",
            "'UPGRADE'",
            "'DRAFT'",
            "'FORM_TEMPLATE_UPGRADE'",
            "form-template-upgrade-v1",
        ],
    },
    {
        "sql": "20260723_mes_route_form_business_approval_policy_backfill.sql",
        "test": "test_mes_route_form_business_approval_policy_backfill_sql.py",
        "requires_signal": True,
        "literals": [
            "'MES'",
            "'EDHR_ROUTE_FORM'",
            "'PUBLISHED'",
            "MES_EDHR_ROUTE_FORM_FILL",
            "ensure_mes_route_form_business_approval_policy",
        ],
    },
]


def read_repo_file(*parts: str) -> str:
    return ROOT.joinpath(*parts).read_text(encoding="utf-8")


def test_m7_release_inventory_has_contract_test_for_every_release_sql() -> None:
    for contract in SQL_CONTRACTS:
        sql_path = ROOT / "sql" / "mysql" / contract["sql"]
        test_path = ROOT / "script" / "tests" / contract["test"]

        assert sql_path.exists(), f"missing release SQL: {contract['sql']}"
        assert test_path.exists(), f"missing release SQL contract test: {contract['test']}"
        assert contract["sql"] in test_path.read_text(encoding="utf-8")


def test_m7_release_sql_declares_metadata_fail_fast_and_no_destructive_dml() -> None:
    for contract in SQL_CONTRACTS:
        sql = read_repo_file("sql", "mysql", contract["sql"])
        upper_sql = sql.upper()

        assert sql.startswith("-- release-migration:"), contract["sql"]
        assert "allowedEnvironments=test,backup,prod" in sql, contract["sql"]
        assert "riskLevel=" in sql, contract["sql"]
        assert "SET NAMES utf8mb4" in sql, contract["sql"]
        if contract["requires_signal"]:
            assert "SIGNAL SQLSTATE '45000'" in sql, contract["sql"]

        assert "DROP TABLE" not in upper_sql, contract["sql"]
        assert "TRUNCATE TABLE" not in upper_sql, contract["sql"]
        assert "DELETE FROM" not in upper_sql, contract["sql"]
        assert "fallback" not in sql.lower(), contract["sql"]


def test_m7_release_inventory_covers_all_platform_business_actions() -> None:
    combined_sql = "\n".join(
        read_repo_file("sql", "mysql", contract["sql"]) for contract in SQL_CONTRACTS
    )

    for contract in SQL_CONTRACTS:
        for literal in contract["literals"]:
            assert literal in combined_sql, f"missing {literal} for {contract['sql']}"

    for effect_executor in [
        "DCC_UPLOAD",
        "DCC_OBSOLETE",
        "DCC_PUBLISH",
        "EDHR_RELEASE",
        "EDHR_BATCH_VOID",
        "FORM_TEMPLATE_UPGRADE",
    ]:
        assert combined_sql.count(effect_executor) >= 2, effect_executor


def test_m7_schedule_replan_retirement_is_after_historical_approval_seed_in_inventory() -> None:
    sql_names = [contract["sql"] for contract in SQL_CONTRACTS]

    assert sql_names.index("20260720_mes_schedule_replan_form_policy_seed.sql") < sql_names.index(
        "20260721_mes_schedule_replan_approval_retire.sql"
    )


def test_m7_release_inventory_keeps_package_manifests_out_of_backend_sql_gate() -> None:
    backend_manifest_changes = [
        path
        for path in [
            ROOT / "package.json",
            ROOT / "pnpm-lock.yaml",
            ROOT / "release-manifest.json",
        ]
        if path.exists()
    ]

    assert backend_manifest_changes == []
