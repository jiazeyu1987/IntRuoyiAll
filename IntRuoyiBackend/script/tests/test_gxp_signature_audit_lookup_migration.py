"""Contract for the additive formal-signature audit lookup performance repair."""
from pathlib import Path
import re

SQL = Path(__file__).resolve().parents[2] / 'sql/mysql/20261005_gxp_signature_audit_lookup_index.sql'


def test_lookup_preserves_duplicate_detection_and_complete_signature_key():
    text = SQL.read_text(encoding='utf-8')
    assert re.search(r'ADD INDEX `idx_gxp_event_signature_lookup`\s*'
                     r'\(`tenant_id`, `operation_id`, `signature_record_id`, `deleted`\)', text)
    assert not re.search(r'ADD\s+UNIQUE|\b(?:INSERT|UPDATE|DELETE|TRUNCATE)\s', text, re.I)
    assert 'ALGORITHM = INPLACE, LOCK = NONE' in text


def test_replay_rejects_wrong_existing_index_and_missing_prerequisites():
    text = SQL.read_text(encoding='utf-8')
    assert 'required_columns <> 4' in text
    assert 'existing_parts <> 4 OR correct_parts <> 4' in text
    assert "NON_UNIQUE = 1 AND SUB_PART IS NULL AND INDEX_TYPE = 'BTREE' AND IS_VISIBLE = 'YES'" in text
    for position, column in enumerate(['tenant_id', 'operation_id', 'signature_record_id', 'deleted'], 1):
        assert f"SEQ_IN_INDEX = {position} AND COLUMN_NAME = '{column}'" in text
    assert text.count("SIGNAL SQLSTATE '45000'") == 2


def test_procedure_lifecycle_and_formal_dependency_are_explicit():
    text = SQL.read_text(encoding='utf-8')
    assert 'dependsOn=20260924_gxp_audit_event_v2; type=schema; riskLevel=medium' in text
    names = re.findall(r'(?:CREATE PROCEDURE|CALL|DROP PROCEDURE)\s+`([^`]+)`', text)
    assert names == ['ensure_gxp_signature_lookup_index'] * 3
    assert len(names[0]) <= 64
    assert not re.search(r'\bDROP\s+(?:TABLE|INDEX)|\bMODIFY\s+COLUMN', text, re.I)
