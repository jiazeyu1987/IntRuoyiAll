"""Execute generated production approval statements against local MySQL (read only)."""
from pathlib import Path
import subprocess
import sys
queries=Path(sys.argv[1]).read_text(encoding='utf-8').splitlines()
if not queries:raise RuntimeError('Missing generated production queries')
for index,sql in enumerate(queries):
 if not sql.lstrip().upper().startswith('SELECT ') or ';' in sql:raise RuntimeError('Expected a single SELECT')
 # EXPLAIN resolves every full production join and expression without altering data.
 result=subprocess.run(['docker','exec','-i','int-ruoyi-mysql','sh','-c',
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --database=ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names'],input='EXPLAIN '+sql,encoding='utf-8',capture_output=True,timeout=30)
 if result.returncode:raise RuntimeError(f'Production query {index+1} failed: '+result.stderr)
print(f'PASS: {len(queries)} complete production SELECT plans resolved against real MySQL schema')
