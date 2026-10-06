from pathlib import Path
import importlib.util,json,hashlib,gzip,subprocess
repo=Path(__file__).resolve().parents[3];task=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('local',task/'g21_mysql_support.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
source=mod.LocalMysql('ruoyi-vue-pro');directory=Path('C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime/g70-mysql-rehearsal')
database='dcc_g70_native_menu_rehearsal';tables=['system_menu','system_role_menu','system_role','system_tenant_package']
manifest=json.loads((repo/'doc/tasks/20261002-dcc-public-backend-completion/g70-native-distribution-menu-fingerprints-r2.json').read_text(encoding='utf-8-sig'))
for row in manifest['assets']:
 data=(repo/row['path']).read_bytes();assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],row['path']
sql=(repo/'IntRuoyiBackend/sql/mysql/20261006_dcc_native_distribution_menu.sql').read_bytes()
def call(db,payload,label):
 command=source.command();command[-1]=db
 result=subprocess.run(command,input=payload,capture_output=True)
 (directory/(label+'.stdout')).write_bytes(result.stdout);(directory/(label+'.stderr')).write_bytes(result.stderr)
 if result.returncode: raise RuntimeError(label+' failed; inspect private receipt')
 return result.stdout.decode('utf-8')
def read(db,text):
 assert text.strip().upper().startswith(('SELECT','SHOW'))
 return call(db,text.encode('utf-8'),'read-'+hashlib.sha256((db+text).encode()).hexdigest()[:12])
def snapshot(db):
 result={}
 for table in tables:
  cols=[x.split('\t')[0] for x in read(db,'SHOW COLUMNS FROM '+table+';').splitlines()]
  assert all(c.replace('_','').isalnum() for c in cols)
  expression=','.join("IF(`"+c+"` IS NULL,NULL,HEX(CAST(`"+c+"` AS BINARY)))" for c in cols)
  rows=[json.loads(x) for x in read(db,'SELECT JSON_ARRAY('+expression+') FROM '+table+' ORDER BY id;').splitlines()]
  result[table]={'columns':cols,'rows':rows}
 return result
assert source.read("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='"+database+"';").strip()=='0','Dedicated rehearsal database already exists; no destructive retry'
backup=json.loads((directory/'backup-receipt.json').read_text(encoding='utf-8'));dump=gzip.decompress((directory/'source-four-table-backup.sql.gz').read_bytes());assert hashlib.sha256(dump).hexdigest()==backup['dumpSha256']
source_before=snapshot('ruoyi-vue-pro')
call('ruoyi-vue-pro',('CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;').encode(),'create-dedicated-database')
call(database,dump,'restore-four-tables-only')
clone_before=snapshot(database);assert clone_before==source_before,'Rehearsal baseline is not the backed-up exact source'
existing1=call(database,sql,'existing-ui-first');existing_after=snapshot(database);assert existing_after==clone_before
existing2=call(database,sql,'existing-ui-repeat');assert snapshot(database)==existing_after
# Only task-created menu/binding in the isolated copy are removed to test true absent seeding.
call(database,b"START TRANSACTION; DELETE FROM system_role_menu WHERE tenant_id=1 AND role_id=910233 AND menu_id=605071339; DELETE FROM system_menu WHERE id=605071339 AND BINARY permission=BINARY 'dcc:controlled-file:distribute'; COMMIT;",'isolated-task-fixture-absence')
absent=snapshot(database);first=call(database,sql,'absent-first');seeded=snapshot(database)
for table in tables:
 idpos=absent[table]['columns'].index('id');old={r[idpos]:r for r in absent[table]['rows']};new={r[idpos]:r for r in seeded[table]['rows']}
 assert all(new.get(k)==v for k,v in old.items()),table+' old row changed'
 assert len(new)-len(old)==(1 if table in ['system_menu','system_role_menu'] else 0),table+' mutation ceiling'
second=call(database,sql,'absent-repeat');assert snapshot(database)==seeded
source1=call('ruoyi-vue-pro',sql,'source-exact-ui-noop-first');assert snapshot('ruoyi-vue-pro')==source_before
source2=call('ruoyi-vue-pro',sql,'source-exact-ui-noop-repeat');source_after=snapshot('ruoyi-vue-pro');assert source_after==source_before
for name,value in [('source-before',source_before),('source-after',source_after),('clone-before',clone_before),('clone-absent',absent),('clone-seeded',seeded)]:
 (directory/(name+'.json')).write_text(json.dumps(value,ensure_ascii=False),encoding='utf-8')
receipt={'status':'G70_ACTUAL_MYSQL_ISOLATED_EXISTING_AND_ABSENT_FIRST_REPEAT_SOURCE_UI_NOOP_PASS','scriptSha256':hashlib.sha256(sql).hexdigest(),'rehearsalDatabase':database,'sourceDatabase':'ruoyi-vue-pro','backup':backup,'receipts':{'cloneUiFirst':existing1,'cloneUiRepeat':existing2,'cloneAbsentFirst':first,'cloneAbsentRepeat':second,'sourceUiFirst':source1,'sourceUiRepeat':source2},'sourceOldFourTablesExactUnchanged':True,'businessActions':'Menu and grant already performed through real UI; source SQL replay changes zero rows, no business data writes','databaseKeptForEvidence':True}
(task/'g70-actual-mysql-root-review.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
print(receipt['status'])
