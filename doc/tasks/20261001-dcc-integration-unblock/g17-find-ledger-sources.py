"""Read Git objects to identify exact historical SQL recorded by the local ledger. No DB writes."""
from pathlib import Path
import hashlib
import json
import subprocess

repo = Path(__file__).resolve().parents[3]
task = Path(__file__).resolve().parent
ledger = {}
for line in (task / 'g13-runtime-complete-ledger.log').read_text(encoding='utf-8-sig').splitlines():
    columns = line.split('\t')
    if len(columns) == 4:
        ledger[columns[0]] = columns[1]
results = []
for name in ['20260513_dcc_base_schema', '20260710_dcc_product_catalog_database']:
    path = f'IntRuoyiBackend/sql/mysql/{name}.sql'
    revisions = subprocess.run(['git', 'log', '--format=%H', '--', path], cwd=repo,
                               check=True, capture_output=True, text=True).stdout.splitlines()
    candidates = []
    for revision in revisions:
        blob = subprocess.run(['git', 'show', f'{revision}:{path}'], cwd=repo,
                              check=True, capture_output=True).stdout
        normalized = blob.replace(b'\r\n', b'\n')
        hashes = {'git_blob': hashlib.sha256(blob).hexdigest(),
                  'utf8_lf': hashlib.sha256(normalized).hexdigest(),
                  'utf8_crlf': hashlib.sha256(normalized.replace(b'\n', b'\r\n')).hexdigest()}
        candidates.append({'revision': revision, 'hashes': hashes,
                           'matchingFormat': next((key for key, value in hashes.items() if value == ledger[name]), None)})
    results.append({'migrationId': name, 'ledgerSha256': ledger[name], 'path': path,
                    'exactSourceFound': any(row['matchingFormat'] for row in candidates), 'gitCandidates': candidates})
(task / 'g17-ledger-source-search.json').write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps([{'migrationId': row['migrationId'], 'exactSourceFound': row['exactSourceFound'],
                   'matchingSources': [candidate for candidate in row['gitCandidates'] if candidate['matchingFormat']]}
                  for row in results], ensure_ascii=False))
