"""Forward migration contract: old APPLIED files stay immutable."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
SQL=ROOT/'sql/mysql'
NAME='20261101_dcc_source_governance_all_environments.sql'


def verify_governance_tables_are_a_registered_all_environment_forward_migration():
    assert (SQL/NAME).is_file(), 'Missing all-environment forward migration'
    text=(SQL/NAME).read_text('utf-8')
    assert text.startswith('-- release-migration: allowedEnvironments=test,prod,backup;')
    catalog=json.loads((SQL/'release-sources.json').read_text('utf-8'))
    assert any(NAME in values for values in catalog.values() if isinstance(values,list)), 'Migration missing from explicit source catalog'
    for suffix in ('governance_batch','governance_item','global_claim'):
        assert 'CREATE TABLE IF NOT EXISTS `dcc_controlled_file_source_'+suffix+'`' in text
    assert '`tenant_id` bigint NOT NULL DEFAULT 0' in text
    lowered=text.lower()
    assert 'drop table' not in lowered and 'truncate' not in lowered
    assert 'insert into' not in lowered and 'update `' not in lowered and 'delete from' not in lowered


def verify_previously_applied_governance_sql_bytes_are_unchanged():
    expected={'20260905_dcc_source_governance.sql':'3decb9660edf63673c559d3bbe2b2d72adb81d8881743c422378eb2f3d528104',
              '20260922_dcc_source_governance_batch_tenant_id.sql':'dcfd74e6305794aca4573abee191e3e004624d9e54ed2ae311017429135bf8c2'}
    for name,digest in expected.items():
        assert hashlib.sha256((SQL/name).read_bytes()).hexdigest()==digest


class GovernanceSchemaTest(unittest.TestCase):
    def test_forward_migration(self):
        verify_governance_tables_are_a_registered_all_environment_forward_migration()

    def test_applied_history_is_immutable(self):
        verify_previously_applied_governance_sql_bytes_are_unchanged()
