from script.e2e.dcc_external_file_review_r07_e2e import run_r07_external_file_review_e2e


def test_r07_external_file_review_real_frontend_path() -> None:
    evidence = run_r07_external_file_review_e2e()
    assert evidence["processDefinitionKey"] == "dcc-external-file-review"
    assert evidence["ordinaryProcessDefinitionKey"] != evidence["processDefinitionKey"]
    assert evidence["closed"] is True
