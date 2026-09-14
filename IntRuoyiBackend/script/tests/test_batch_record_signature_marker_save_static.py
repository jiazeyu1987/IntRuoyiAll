from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def extract_method(source: str, signature: str) -> str:
    start = source.find(signature)
    assert start >= 0, f"{signature} must exist"
    body_start = source.find("{", start)
    assert body_start >= 0, f"{signature} must use a block body"
    depth = 0
    for index in range(body_start, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[body_start : index + 1]
    raise AssertionError(f"{signature} block body is not closed")


def test_cell_rule_request_accepts_signature_markers() -> None:
    req_vo = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/"
        "batchrecordreport/vo/BatchRecordReportCellRulesReqVO.java"
    )

    assert "private List<BatchRecordReportSignatureCellMarkerVO> signatureCellMarkers;" in req_vo
    assert req_vo.count("signatureCellMarkers") == 1


def test_save_cell_rules_applies_submitted_signature_markers_before_validation() -> None:
    service = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
        "MesProBatchRecordReportServiceImpl.java"
    )
    save_body = extract_method(
        service,
        "public BatchRecordReportCellRulesRespVO saveCellRules(BatchRecordReportCellRulesReqVO reqVO)",
    )

    apply_index = save_body.find("applySubmittedSignatureMarkers(root, reqVO.getSignatureCellMarkers());")
    stale_index = save_body.find("removeStaleSignatureMarkersForCellRules(root, reqVO.getRules());")
    validate_index = save_body.find("MesProBatchRecordCellRuleSupport.validateRule(rule, cell);")
    assert apply_index >= 0
    assert stale_index > apply_index
    assert validate_index > stale_index


def test_submitted_signature_markers_are_validated_and_persisted() -> None:
    service = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
        "MesProBatchRecordReportServiceImpl.java"
    )
    apply_body = extract_method(
        service,
        "private void applySubmittedSignatureMarkers(JSONObject root, List<BatchRecordReportSignatureCellMarkerVO> markers)",
    )

    assert "if (markers == null)" in apply_body
    assert "clearSignatureMarkers(root);" in apply_body
    assert "validateSignatureMarker(marker);" in apply_body
    assert "cell.put(MesProBatchRecordCellRuleSupport.SIGNATURE_KEY, toSignatureJson(marker, signatureCellKey));" in apply_body


def test_plain_cell_rule_removes_signature_marker() -> None:
    service = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
        "MesProBatchRecordReportServiceImpl.java"
    )
    sync_body = extract_method(
        service,
        "private void syncSignatureMarkerForCellRule(BatchRecordReportCellRuleVO rule, JSONObject cell)",
    )

    assert "if (!isSignatureCellRule(rule))" in sync_body
    assert "cell.remove(MesProBatchRecordCellRuleSupport.SIGNATURE_KEY);" in sync_body
    assert 'signature.put("enabled", true);' in sync_body
    assert 'signature.put("actionType", actionType);' in sync_body
