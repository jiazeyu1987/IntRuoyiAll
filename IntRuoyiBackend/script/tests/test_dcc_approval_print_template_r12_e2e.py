from script.e2e.dcc_approval_print_template_r12_e2e import run_r12_approval_print_template_e2e


def test_r12_approval_print_template_real_frontend_path() -> None:
    evidence = run_r12_approval_print_template_e2e()
    assert evidence["checks"]["validTemplate"]["payload"]["code"] == 0
    assert evidence["files"]["exported"]["file_number"] in evidence["checks"]["exportWord"]["documentXml"]
    assert evidence["checks"]["noPermission"]["payload"]["code"] != 0
