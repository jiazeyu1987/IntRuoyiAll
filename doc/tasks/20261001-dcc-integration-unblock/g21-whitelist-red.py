"""Run the existing planner against real frozen inputs; never connect to a database."""
import json
from pathlib import Path
import sys

task = Path(__file__).resolve().parent
backend = Path('C:/IntRuoyi/20261001-dcc-integration/IntRuoyiBackend')
sys.path.insert(0, str(backend))
from script.release.release_preflight_plan import build_preflight_plan

package = json.loads((task / 'g18-migration-package.json').read_text(encoding='utf-8-sig'))
candidate = json.loads((task / 'g18-approved-scope-candidate.json').read_text(encoding='utf-8-sig'))
state = {}
for line in (task / 'g13-runtime-complete-ledger.log').read_text(encoding='utf-8-sig').splitlines():
    migration, checksum, status, environment = line.split('\t')
    if environment == 'test':
        state[migration] = {'status': 'APPLIED' if status in {'APPLIED', 'SKIPPED_ALREADY_APPLIED'} else status, 'sha256': checksum}
plan = build_preflight_plan(package['executionOrder'], state, target_environment='test', publish_scope='with-data')
items = plan['items']
actual = {item['migrationId'] for item in items if item['action'] == 'APPLY'}
expected = {item['migrationId'] for item in candidate['executionOrder']}
print(json.dumps({'existingPlannerStatus': plan['status'], 'actualApplyCount': len(actual), 'expectedApplyCount': len(expected), 'extraIdentities': sorted(actual - expected)}, indent=2))
assert actual == expected, 'Current generic release plan replays SQL outside the authorized nineteen-item scope'
