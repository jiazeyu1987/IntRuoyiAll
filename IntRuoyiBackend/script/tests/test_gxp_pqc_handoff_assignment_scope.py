"""PQC rule save has specialized audit; exact R1 scope must not imply unified GxP coverage."""
import hashlib
import json
from pathlib import Path

import pytest
import yaml

from test_gxp_audit_method_boundaries import fixture, gate

ROOT = gate.source_root_path(Path(__file__).resolve().parents[3])
CONFIG = ROOT / 'IntRuoyiBackend/config'
SERVICE = 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesPqcHandoffAssignmentService.java'
LOCATOR = 'cn.iocoder.yudao.module.mes.service.pro.handoff.MesPqcHandoffAssignmentService#save'
SAVE_REVIEW = 'PQC-HANDOFF-RULE-SAVE-01'
FILE_REVIEW = 'PQC-HANDOFF-RULE-F01'
OPERATIONS_CONTRACT_SHA256 = 'fca56b73a9ae63052b8d2a96316f948069f4e395cae0fca498ee5a507e3b061f'


def documents():
    policy = yaml.safe_load((CONFIG / 'gxp-audit-policy.yaml').read_text(encoding='utf-8'))
    methods = json.loads((CONFIG / 'gxp-audit-method-scope-review.json').read_text(encoding='utf-8'))
    files = json.loads((CONFIG / 'gxp-audit-file-scope-review.json').read_text(encoding='utf-8'))
    records = [json.loads(line) for line in (CONFIG / 'gxp-audit-boundary-exclusions.jsonl').read_text(encoding='utf-8').splitlines()]
    return policy, methods, files, records


def test_new_rule_writer_is_exact_specialized_gap_not_a_fake_gxp_operation():
    policy, methods, files, records = documents()
    assert policy['policyVersion'] == '20261005-manual-handoff-02'
    assert policy['approvalReference'] == 'EDHR-20261005-MANUAL-HANDOFF02'
    assert policy['coverageScope']['mode'] == 'R1'
    assert len(policy['operations']) == 61
    assert hashlib.sha256(json.dumps(policy['operations'],sort_keys=True,ensure_ascii=False,
                                    separators=(',',':')).encode('utf-8')).hexdigest() == OPERATIONS_CONTRACT_SHA256
    assert not any(LOCATOR in op['sourceLocators'] for op in policy['operations'])
    review = methods[SAVE_REVIEW]
    assert review['sourceLocator'] == LOCATOR and review['decision'] == 'OUT_OF_RELEASE_SCOPE'
    assert review['candidate'] == SERVICE and review['candidateSha256'] == gate.source_sha256(ROOT / SERVICE)
    assert 'recordInCallerTransaction' in review['analysis'] and 'WORK_TASK_RULE_SAVE' in review['analysis']
    assert not any(entry['evidenceReference'].endswith('#'+SAVE_REVIEW) for entry in policy['coverageScope']['excludedReferences'])
    # This service is not one of the operation-owner files: the file gate consumes its exact scope.
    matching = [r for r in records if r['candidate'] == SERVICE and r['sourceType'] == 'DOMAIN_SERVICE']
    assert len(matching) == 1 and matching[0]['decision'] == 'REVIEWED_OUT_OF_RELEASE_SCOPE'
    assert matching[0]['evidenceReference'].endswith('#'+FILE_REVIEW)
    assert files[FILE_REVIEW]['memberScopeEvidence'] == 'IntRuoyiBackend/config/gxp-audit-method-scope-review.json#'+SAVE_REVIEW
    gate.validate_file_scope_review(ROOT, policy, matching[0])


def test_actual_controller_save_has_exact_gap_and_no_new_approval_capability():
    policy, _, files, records = documents()
    controller = SERVICE.replace('service/pro/handoff/MesPqcHandoffAssignmentService.java',
                                 'controller/admin/pro/handoff/MesActiveOrderHandoffController.java')
    source = (ROOT/controller).read_text(encoding='utf-8')
    assert '@PostMapping("/pqc-assignment")' in source and 'pqcAssignment.save(' in source
    assert "@ss.hasPermission('mes:pro-edhr-work-task-rule:update')" in source
    review = files['MANUAL-HANDOFF-F01']
    assert any(effect['effect'] == 'PQC responsibility rule persistence and same-transaction specialized audit'
               for effect in review['outOfScopeEffects'])
    assert any(dep['candidate'] == SERVICE and dep['candidateSha256'] == gate.source_sha256(ROOT/SERVICE)
               for dep in review['dependentSources'])
    matching = [r for r in records if r['candidate'] == controller and r['sourceType'] == 'CONTROLLER']
    assert len(matching) == 1
    gate.validate_file_scope_review(ROOT,policy,matching[0])


def test_actual_new_audit_transaction_and_existing_qa_gap_remain_distinct():
    _, _, files, _ = documents()
    pqc = (ROOT / SERVICE).read_text(encoding='utf-8')
    qa = (ROOT / SERVICE.replace('MesPqcHandoffAssignmentService', 'MesQaHandoffAssignmentService')).read_text(encoding='utf-8')
    assert 'operationAudit.recordInCallerTransaction(' in pqc and '.setOperationType("WORK_TASK_RULE_SAVE")' in pqc
    assert 'operationAudit.record(' in qa
    assert 'REQUIRES_NEW' in files['MANUAL-HANDOFF-F04']['analysis']
    assert 'recordInCallerTransaction' in files[FILE_REVIEW]['analysis']
    assert files['MANUAL-HANDOFF-F04']['decision'] == 'REVIEWED_OUT_OF_RELEASE_SCOPE'


def test_changed_java_fixtures_keep_their_actual_import_boundary():
    _, _, _, records = documents()
    sources = [SERVICE.replace('src/main/java', 'src/test/java').replace(
        'MesPqcHandoffAssignmentService.java','MesActiveOrderHandoffLifecycleTest.java'),
        SERVICE.replace('src/main/java', 'src/test/java').replace(
        'handoff/MesPqcHandoffAssignmentService.java','processpool/team/MesCorrectionSignatureEvidenceReaderTest.java')]
    for source in sources:
        assert gate.BOUNDARY_PATTERNS['IMPORT'].search((ROOT/source).read_text(encoding='utf-8'))
        matching = [r for r in records if r['candidate'] == source and r['sourceType'] == 'IMPORT']
        assert len(matching) == 1 and matching[0]['decision'] == 'APPROVED_EXCLUSION'
        assert matching[0]['candidateSha256'] == gate.source_sha256(ROOT/source)
        assert 'fixture' in matching[0]['reason'] and 'business PASS' in matching[0]['reason']


def test_additive_audit_index_is_a_real_schema_gap_with_separate_static_fixture():
    policy, _, files, records = documents()
    sql = 'IntRuoyiBackend/sql/mysql/20261005_gxp_signature_audit_lookup_index.sql'
    test = 'IntRuoyiBackend/script/tests/test_gxp_signature_audit_lookup_migration.py'
    matching = [r for r in records if r['candidate'] == sql and r['sourceType'] == 'MIGRATION']
    assert len(matching) == 1 and matching[0]['decision'] == 'REVIEWED_OUT_OF_RELEASE_SCOPE'
    assert matching[0]['candidateSha256'] == gate.source_sha256(ROOT/sql)
    review = files['SIGNATURE-LOOKUP-INDEX-F01']
    assert 'ALTER TABLE' in review['analysis'] and 'CREATE/CALL/DROP PROCEDURE' in review['analysis']
    assert 'not READ_ONLY' in review['analysis'] and 'no unified Gxp append' in review['analysis']
    gate.validate_file_scope_review(ROOT,policy,matching[0])
    fixture = [r for r in records if r['candidate'] == test and r['sourceType'] == 'OPS_SCRIPT']
    assert len(fixture) == 1 and fixture[0]['decision'] == 'APPROVED_EXCLUSION'
    assert fixture[0]['candidateSha256'] == gate.source_sha256(ROOT/test)
    assert 'not execute SQL' in fixture[0]['reason']


def exact_specialized_fixture(tmp_path):
    source, policy, _ = fixture(tmp_path, 'public void save() { mapper.update(rule); operationAudit.recordInCallerTransaction(command); }')
    policy.update(approvalReference='FIXTURE-REVIEW')
    policy['operations'] = []
    policy['coverageScope']['mode'] = 'R1'
    relative = source.relative_to(tmp_path).as_posix()
    review = dict(candidate=relative, candidateSha256=gate.source_sha256(source), decision='REVIEWED_OUT_OF_RELEASE_SCOPE',
                  approvedBy='fixture-reviewer', approvalReference='FIXTURE-REVIEW', futureOwnerRole='fixture-owner',
                  futureTaskReference='FIXTURE-SPECIALIZED-GAP', analysis='Real rule insert/update plus specialized audit; no unified operation.')
    (tmp_path/'scope.json').write_text(json.dumps({'SAVE':review}),encoding='utf-8')
    record = {k:v for k,v in review.items() if k!='analysis'}
    record.update(sourceType='DOMAIN_SERVICE', reasonCode='SPECIALIZED_GAP',
                  reason=relative+' SHA256='+review['candidateSha256']+': real rule update and specialized audit, no unified Gxp operation.',
                  evidenceReference='scope.json#SAVE')
    (tmp_path/'approved.jsonl').write_text(json.dumps(record)+'\n',encoding='utf-8')
    return source, policy


def test_unified_full_coverage_cannot_be_claimed_from_specialized_rule_audit(tmp_path):
    _, policy = exact_specialized_fixture(tmp_path)
    scan = gate.validate_boundary_scan(tmp_path, policy, [])
    assert scan[4][0]['decision'] == 'REVIEWED_OUT_OF_RELEASE_SCOPE'
    policy['coverageScope']['mode'] = 'FULL_COVERAGE'
    with pytest.raises(SystemExit,match='FULL_COVERAGE'):
        gate.validate_boundary_scan(tmp_path, policy, [])


def test_changed_unreviewed_rule_source_cannot_reuse_scope_approval(tmp_path):
    source, policy = exact_specialized_fixture(tmp_path)
    gate.validate_boundary_scan(tmp_path, policy, [])
    source.write_text(source.read_text(encoding='utf-8')+'\n// changed source: purge rule\n',encoding='utf-8')
    with pytest.raises(SystemExit,match='SHA-256 changed'):
        gate.validate_boundary_scan(tmp_path, policy, [])


def test_adjacent_new_controller_writer_requires_its_own_scope(tmp_path):
    source, policy = exact_specialized_fixture(tmp_path)
    controller = source.parent/'NewController.java'
    controller.write_text('package demo; public class NewController { @PostMapping public void save(){ service.save(); } }',encoding='utf-8')
    for category in policy['coverageScope']['writeBoundaryScan']['categories']:
        if category['sourceType'] == 'CONTROLLER': category['paths']=[controller.relative_to(tmp_path).as_posix()]
    with pytest.raises(SystemExit,match='NewController.java is not present'):
        gate.validate_boundary_scan(tmp_path, policy, [])
