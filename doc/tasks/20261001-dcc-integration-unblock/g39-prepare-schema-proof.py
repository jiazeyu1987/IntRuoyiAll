"""Create the Root proof from completed real migration journals and current SHOW CREATE."""
from pathlib import Path
import hashlib
import importlib.util
import json
import re

ROOT = Path(__file__).resolve().parent
TASK = Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-detail-integration')
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def desc(path):
    return {'path': str(path.resolve()), 'sha256': sha(path), 'bytes': path.stat().st_size}


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


driver = load('g39_actual_schema_proof_driver', TASK / 'g39-driver.py')
support = load('g39_actual_schema_proof_support', ROOT / 'g21_mysql_support.py')
assert sha(TASK / 'g39-driver.py') == '667a81edf468e1212afef8a01ae5cb996bd62737cfba7db6138e30259e0db2d4'
journal = PROTECTED / 'g39-source-migration-run/driver-receipt.json'
driver.validate_completed_journal(json.loads(journal.read_text()), driver.SOURCE)
destination = PROTECTED / 'g39-source-schema-proof'
assert not destination.exists()
database = support.LocalMysql(driver.SOURCE)
shapes, definitions = {}, {}
for table in ['dcc_legacy_source_name_scope', 'dcc_legacy_source_name_evidence', 'dcc_source_name_reservation']:
    raw = database.read('SHOW CREATE TABLE `' + table + '`;\n')
    name, separator, ddl = raw.partition('\t')
    assert separator and name == table and ddl.startswith('CREATE TABLE ')
    ddl = ddl.removesuffix('\n')
    assert 'ENGINE=InnoDB' in ddl
    normalized = re.sub(r'(?i) AUTO_INCREMENT=[0-9]+(?=\s|$)', '', ddl)
    shapes[table] = hashlib.sha256(normalized.encode('utf-8')).hexdigest()
    definitions[table] = ddl
contract = TASK / 'g28-schema-contract.json'
assert sha(contract) == 'f1ec2ec8207b34f0427d021f717c4859d6b71715fbdc436706263ed0fd7e9f0a'
destination.mkdir()
copied = destination / 'validated-schema-contract.json'
copied.write_bytes(contract.read_bytes())
(destination / 'current-show-create.json').write_text(json.dumps(definitions, indent=2) + '\n', encoding='utf-8')
validator = {'status': 'ROOT_REPLAYED_CURRENT_SCHEMA_DRIVER_PASS', 'database': driver.SOURCE,
             'serverUuid': driver.schema.UUID, 'sourceJournalSha256': sha(journal), 'schemaContractSha256': sha(contract),
             'showCreateTableSha256': shapes, 'validatorSourceSha256': sha(TASK / 'g39-driver.py')}
validator_path = destination / 'validator-receipt.json'
validator_path.write_text(json.dumps(validator, indent=2) + '\n', encoding='utf-8')
proof = {'status': 'ACTUAL_SOURCE_SIDECAR_SCHEMA_FIRST_REPEAT_VERIFIED', 'database': driver.SOURCE,
         'serverUuid': driver.schema.UUID, 'migrationId': driver.MIGRATION, 'migrationSqlSha256': driver.SQL_SHA,
         'validatedContract': desc(copied), 'completedSourceJournal': desc(journal),
         'validatorReceipt': desc(validator_path), 'showCreateTableSha256': shapes}
path = destination / 'schema-proof.json'
path.write_text(json.dumps(proof, indent=2) + '\n', encoding='utf-8')
result = {'status': 'ACTUAL_COMPLETED_SOURCE_SCHEMA_PROOF_SEALED_NOT_LEGACY_ACTIVATION',
          'proof': desc(path), 'validator': desc(validator_path), 'currentTables': 3,
          'actualReadOnlyShowCreateQueries': 3, 'databaseWrites': False, 'actualQualityApproved': False}
(ROOT / 'g39-schema-proof-root-review.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': result['status'], 'currentTables': 3, 'databaseWrites': False}))
