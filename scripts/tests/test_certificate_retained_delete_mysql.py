"""Real MySQL acceptance of the disposal contract using private read-only evidence.

Run with --evidence-dir PATH. Creates/removes only its labeled network-isolated
container; captured evidence is read locally and never contacts either server.
"""
import argparse,hashlib,importlib.util,json,os,subprocess,sys,time,contextlib,io,uuid
from pathlib import Path
from unittest.mock import patch
parser=argparse.ArgumentParser(__doc__);parser.add_argument('--evidence-dir',required=True)
args=parser.parse_args();repo=Path(__file__).resolve().parents[2];private=Path(args.evidence_dir)
sys.path.insert(0,str(repo/'scripts/maintenance'));import certificate_retained_delete as worker
container='codex-retained-delete-20261127';label='certificate-retained-delete-20261127'
assert subprocess.run(['docker','inspect',container],capture_output=True).returncode!=0
subprocess.run(['docker','run','--detach','--name',container,'--network','none','--tmpfs','/var/lib/mysql:rw,size=1073741824','--label','intruoyi.task='+label,'-e','MYSQL_ALLOW_EMPTY_PASSWORD=yes','mysql:8.0.39','--innodb-redo-log-capacity=67108864'],check=True,capture_output=True)
checks=[]
def command():return ['docker','exec','-i',container,'mysql','-uroot','--default-character-set=utf8mb4','--batch','--raw','--unbuffered','--skip-column-names']
def query(statement,fail=False):
 p=subprocess.run(command(),input=statement.encode(),capture_output=True,timeout=120)
 if (p.returncode!=0)!=fail:
  (repo/'.runtime/retained-delete-mysql.stderr.private.txt').write_bytes(p.stderr)
  raise AssertionError('Unexpected SQL outcome; private error saved')
 return p.stdout.decode().rstrip('\r\n')
class FixtureDB:
 def query(self,statement):return query('USE retained_fixture;\n'+statement)
def capture(baseline):return worker.capture(FixtureDB(),baseline)
def restore(environment):
 baseline=json.loads((private/(environment+'-full-baseline.private.json')).read_bytes())
 sources=json.loads((private/(environment+'-migration-sources.private.json')).read_bytes())
 baseline['ddl'].update(sources['ddl'])
 for table in sources['ddl']:baseline[table]=sources[table]
 query('DROP DATABASE IF EXISTS retained_fixture; CREATE DATABASE retained_fixture CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
 ddl='USE retained_fixture; SET FOREIGN_KEY_CHECKS=0;\n'+'\n'.join(x.split('\t',1)[1]+';' for x in baseline['ddl'].values())
 query(ddl)
 columns={}
 for line in baseline['columns'].splitlines():
  t,n,typ,extra=line.split('\t');columns.setdefault(t,[]).append((n,typ,extra))
 for table in baseline['ddl']:
  fields=[(n,typ) for n,typ,extra in columns[table] if not any(x in extra for x in ('VIRTUAL GENERATED','STORED GENERATED'))]
  data=worker.rows(baseline[table]);inserts=[]
  for row in data:
   values=[]
   for name,typ in fields:
    val=row[name]
    if typ=='json' and val is not None:val=worker.canonical(val)
    values.append(worker.literal(val))
   inserts.append('INSERT INTO `'+table+'`('+','.join('`'+n+'`' for n,_ in fields)+') VALUES('+','.join(values)+');')
  if inserts:query('USE retained_fixture;SET FOREIGN_KEY_CHECKS=0;\n'+'\n'.join(inserts))
 for trigger in worker.rows(baseline['triggers']):
  query('USE retained_fixture; SET SESSION sql_mode='+worker.literal(trigger['sqlMode'])+';\nDELIMITER $$\nCREATE TRIGGER '+trigger['name']+' '+trigger['timing']+' '+trigger['event']+' ON '+trigger['table']+' FOR EACH ROW\n'+trigger['body']+'$$\nDELIMITER ;')
 baseline['identity']=query('USE retained_fixture;SELECT DATABASE(),@@server_uuid,@@version;')
 return baseline
try:
 for _ in range(60):
  p=subprocess.run(['docker','exec',container,'mysqladmin','-uroot','ping'],capture_output=True)
  if p.returncode==0 and subprocess.check_output(['docker','exec',container,'cat','/proc/1/comm'],text=True).strip()=='mysqld':break
  time.sleep(.5)
 else:raise RuntimeError('MySQL fixture unavailable')
 scope=json.loads((private/'scope.json').read_bytes())
 mid='20261127_dcc_registration_certificate_retained_delete'
 sqlroot=repo/'IntRuoyiBackend/sql/mysql'
 for environment in ('prod','test'):
  baseline=restore(environment);batch=scope[environment];current=capture(baseline)
  old_by_id={x['id']:x for x in worker.rows(baseline[worker.AUDIT])}
  for row in worker.rows(current[worker.AUDIT]):
   old=old_by_id.get(row['id'])
   if row!=old:
    (private/'fixture-audit-diff.private.json').write_text(json.dumps(dict(before=old,after=row),ensure_ascii=False,indent=2),encoding='utf8')
    print('audit field differences',[k for k in row if not old or row[k]!=old[k]],flush=True);break
  assert worker.compare_state(current,baseline,batch,environment)=='ORIGINAL'
  worker.validate_contract(FixtureDB(),baseline,private)
  admission=FixtureDB().query((sqlroot/'target-preflight'/(mid+'.preflight.sql')).read_text('utf8'))
  assert ('UNRESOLVED_PRODUCT' in admission) if environment=='prod' else admission=='TARGET_PREFLIGHT_PASS:'+mid
  certs=worker.certificates(baseline,batch);certificate=certs[0]
  # Original trigger demonstrates the real production defect.
  result=query('USE retained_fixture;UPDATE dcc_registration_certificate SET deleted=1 WHERE id='+str(certificate['id'])+' AND tenant_id='+str(batch['tenantId'])+';',True)
  checks.append(environment+': original formal-delete rejection reproduced; read-only plan and schema checks pass')
  FixtureDB().query((private/'retained-master-trigger.sql').read_text('utf8'))
  online_detail=worker.literal(worker.canonical(dict(commandKind='CERTIFICATE_DELETE',expectedRowVersion=certificate['row_version'])))
  online_insert='INSERT INTO dcc_registration_certificate_audit(tenant_id,owner_company_id,certificate_id,version_id,snapshot_id,event_key,event_type,actor_id,result,result_code,request_trace_id,detail_json,occurred_at,creator) VALUES('+','.join(worker.literal(x) for x in [certificate['tenant_id'],certificate['owner_company_id'],certificate['id'],certificate['current_version_id'],certificate['current_snapshot_id'],'fixture-online-delete','CERTIFICATE_DELETE_SUCCEEDED',99,'SUCCESS','OK','fixture-online-delete'])+','+online_detail+",CURRENT_TIMESTAMP,'99');"
  online_update='UPDATE dcc_registration_certificate SET deleted=1,row_version=row_version+1,updater=\'99\',update_time=CURRENT_TIMESTAMP WHERE id='+str(certificate['id'])+' AND tenant_id='+str(batch['tenantId'])+';'
  FixtureDB().query('START TRANSACTION;'+online_insert+online_update+'ROLLBACK;')
  query('USE retained_fixture;START TRANSACTION;'+online_insert+online_update.replace('row_version=row_version+1','row_version=row_version+2')+'ROLLBACK;',True)
  query('USE retained_fixture;START TRANSACTION;'+online_insert+online_update.replace('row_version=row_version+1','row_version=row_version+1,pending_version_id=987654321')+'ROLLBACK;',True)
  assert worker.compare_state(capture(baseline),baseline,batch,environment)=='ORIGINAL'
  checks.append(environment+': normal UI command audit accepted by actual MySQL; wrong revision and simultaneous pointer change rejected; all trial transactions rolled back')
  for change in ('deleted=1,row_version=row_version+1,updater=\'0\'', 'product_master_id=999999'):
   query('USE retained_fixture;UPDATE dcc_registration_certificate SET '+change+' WHERE id='+str(certificate['id'])+' AND tenant_id='+str(batch['tenantId'])+';',True)
  # Late postcondition failure must roll back every disposal audit and master update.
  script=worker.checks_sql(baseline,batch,environment)
  broken=script.replace('COMMIT;', 'INSERT INTO disposal_assert VALUES(0); COMMIT;')
  query('USE retained_fixture;'+broken,True)
  assert worker.compare_state(capture(baseline),baseline,batch,environment)=='ORIGINAL'
  checks.append(environment+': missing audit, formal identity mutation and late transaction failure all reject; full rollback confirmed')
  FixtureDB().query(script)
  after=capture(baseline)
  assert worker.compare_state(after,baseline,batch,environment)=='COMPLETED'
  assert worker.compare_state(capture(baseline),baseline,batch,environment)=='COMPLETED'
  checks.append(environment+': actual exact '+str(len(certs))+' disposal committed; four pending versions retained; all outside masters, versions, snapshots, files, infra_file, old audit and ledger unchanged; repeated verification does not replay')
  for change in ('deleted=0', 'row_version=row_version+1'):
   query('USE retained_fixture;UPDATE dcc_registration_certificate SET '+change+' WHERE id='+str(certificate['id'])+' AND tenant_id='+str(batch['tenantId'])+';',True)
  query('USE retained_fixture;DELETE FROM dcc_registration_certificate WHERE id='+str(certificate['id'])+' AND tenant_id='+str(batch['tenantId'])+';',True)
  before_snapshots=after['dcc_registration_certificate_snapshot']
  assert FixtureDB().query((sqlroot/'target-preflight'/(mid+'.preflight.sql')).read_text('utf8'))=='TARGET_PREFLIGHT_PASS:'+mid
  FixtureDB().query((sqlroot/(mid+'.sql')).read_text('utf8'))
  contract=(sqlroot/'database-contracts/dcc-certificate-retained-delete.sql').read_text('utf8')
  assert FixtureDB().query(contract)=='DATABASE_CONTRACT_PASS:dcc-certificate-retained-delete'
  upgraded=capture(baseline)
  # Migration may shape live certificates; exactly approved deleted certificates remain byte-equivalent.
  version_ids={x['id'] for x in worker.rows(baseline['dcc_registration_certificate_version']) if x['tenant_id']==batch['tenantId'] and x['certificate_id'] in batch['ids']}
  selected=lambda raw:sorted(worker.canonical(x) for x in worker.rows(raw) if x['tenant_id']==batch['tenantId'] and x['version_id'] in version_ids)
  assert selected(before_snapshots)==selected(upgraded['dcc_registration_certificate_snapshot'])
  FixtureDB().query((sqlroot/(mid+'.sql')).read_text('utf8'))
  checks.append(environment+': corrected forward migration and repeat succeed after disposal; deleted-certificate snapshots remain identical; undelete, later mutation and physical deletion blocked')
  # Changed database/row/evidence must fail the independent verifier.
  for case_label,mutation in [('identity',lambda j:j.update(identity='wrong')),
    ('outside-master',lambda j:j.update(dcc_registration_certificate=j['dcc_registration_certificate'].replace('"row_version": 1','"row_version": 999',1))),
    ('snapshot',lambda j:j.update(dcc_registration_certificate_snapshot=j['dcc_registration_certificate_snapshot']+'\n'+j['dcc_registration_certificate_snapshot'].splitlines()[0]))]:
   damaged=dict(after);mutation(damaged)
   try:worker.compare_state(damaged,baseline,batch,environment)
   except AssertionError:pass
   else:raise AssertionError('Verifier accepted '+case_label)
  checks.append(environment+': verifier rejects wrong database, master revision and snapshot evidence changes')
 # Exercise the actual CLI, durable session handoff and repeat behavior against MySQL.
 baseline=restore('prod');env='prod';batch=scope[env]
 fixture_bundle=repo/('.runtime/retained-delete-cli-fixture-'+uuid.uuid4().hex[:10])
 fixture_bundle.mkdir(exist_ok=True)
 assert not (fixture_bundle/'execution-prod').exists(),'Never overwrite a previous CLI fixture journal'
 for name in ('scope.json','retained-trigger-metadata.json','retained-master-trigger.sql','retained-master-trigger-guard.sql'):
  (fixture_bundle/name).write_bytes((private/name).read_bytes())
 for environment in ('test','prod'):
  original=json.loads((private/(environment+'-full-baseline.private.json')).read_bytes())
  original['identity']=baseline['identity']
  original['components']['database']={'name':container,'id':json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]['Id']}
  (fixture_bundle/(environment+'-full-baseline.private.json')).write_text(json.dumps(original),encoding='utf8')
  source=json.loads((private/(environment+'-migration-sources.private.json')).read_bytes());source['identity']=baseline['identity']
  (fixture_bundle/(environment+'-migration-sources.private.json')).write_text(json.dumps(source),encoding='utf8')
 profiles={e:dict(host={'prod':'172.30.30.57','test':'172.30.30.58'}[e],**{r:(container if r=='database' else baseline['components'][r]['name']) for r in ('database','backend','frontend')}) for e in ('test','prod')}
 (fixture_bundle/'profiles.private.json').write_text(json.dumps(profiles),encoding='utf8')
 (fixture_bundle/'apply-sql-sha256.json').write_text(json.dumps({'prod':hashlib.sha256(worker.checks_sql(baseline,scope['prod'],'prod').encode()).hexdigest()}),encoding='utf8')
 (fixture_bundle/'certificate_retained_delete.py').write_bytes(Path(worker.__file__).read_bytes())
 files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in fixture_bundle.iterdir() if p.is_file() and p.name!='manifest.json'}
 (fixture_bundle/'manifest.json').write_text(json.dumps(dict(files=files)),encoding='utf8')
 class CLIDatabase(worker.Database):
  def command(self):return command()+['retained_fixture']
  def inspect(self,name):
   if name==container:return json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
   role=next(r for r in ('backend','frontend') if baseline['components'][r]['name']==name)
   return dict(Id=baseline['components'][role]['id'],State=dict(Running=False))
 with patch.object(worker,'Database',CLIDatabase):
  for action in ('plan','apply','verify','apply'):
   stream=io.StringIO()
   with patch.object(sys,'argv',['worker',action,'--bundle',str(fixture_bundle),'--environment','prod']),contextlib.redirect_stdout(stream):worker.main()
   report=json.loads(stream.getvalue());assert report['state']==('ORIGINAL' if action=='plan' else 'COMPLETED')
 checks.append('Actual CLI plan/apply/verify/repeated apply passed on local MySQL; session identity durably recorded before writes; repeat produced no extra audits or SQL replay')
 result=dict(passed=True,database='local isolated MySQL 8.0.39',containerNetwork='none',publishedPorts=[],remoteWrites=False,checks=checks)
 (repo/'.runtime/retained-delete-mysql-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
 print(json.dumps(result,ensure_ascii=False))
finally:
 info=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
 assert info['Config']['Labels']['intruoyi.task']==label and info['HostConfig']['NetworkMode']=='none' and not info['HostConfig'].get('PortBindings')
 subprocess.run(['docker','rm','-f',container],check=True,capture_output=True)
