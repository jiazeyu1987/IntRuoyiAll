"""Pure offline legacy manifest builder. Incomplete real sources produce diagnostics only."""
import argparse,hashlib,json,re
from datetime import datetime,timezone,timedelta
from pathlib import Path
from urllib.parse import urlsplit

UUID='92ca05d0-aec8-11f1-a944-02b4e226a5ef'
CANONICAL_PROTOCOL='DccLegacyNameVerifiedScope.rowHash/BootJackson.v1'
def require(ok,message):
 if not ok:raise ValueError(message)
def strict(blob):
 def pairs(items):
  obj={}
  for key,value in items:require(key not in obj,'duplicate JSON field');obj[key]=value
  return obj
 return json.loads(blob,object_pairs_hook=pairs,parse_constant=lambda x:(_ for _ in ()).throw(ValueError('nonfinite JSON')))
def sha(blob):return hashlib.sha256(blob).hexdigest()
def ident(value):require(isinstance(value,str) and re.fullmatch(r'[1-9][0-9]*',value) and int(value)<=9223372036854775807,'exact positive Long string required');return value
def digest(value):require(isinstance(value,str) and re.fullmatch(r'[0-9a-f]{64}',value),'exact lowerhex source SHA required');return value
def facts_index(facts):
 rows=[strict(line) for line in facts.decode('utf-8').splitlines() if line];runtime=[r for r in rows if r.get('kind')=='runtime']
 require(len(runtime)==1 and runtime[0].get('database')=='ruoyi-vue-pro' and runtime[0].get('serverUuid')==UUID and runtime[0].get('tenantId')=='1','actual source runtime scope required')
 groups={}
 for row in rows:
  kind=row.get('kind')
  if kind=='runtime':continue
  require(kind in {'claim','master','version','storage','ownership','ticket','reference'},'unknown fact row')
  key=ident(row.get('id'));group=groups.setdefault(kind,{})
  require(key not in group,'duplicate actual fact identity');group[key]=row
  if kind!='storage':require(row.get('tenantId')=='1','foreign fact tenant')
  require(row.get('deleted',0)==0,'deleted original fact')
 return groups,runtime[0]
def verify_bytes(facts,receipt_blob,results_blob):
 groups,runtime=facts_index(facts);receipt=strict(receipt_blob)
 versions=groups.get('version',{});require(versions,'version evidence required');sources={ident(v['sourceFileId']):v for v in versions.values()}
 require(len(sources)==len(versions),'ambiguous shared source scope outside reviewed bundle')
 require(receipt.get('source')=={'database':'ruoyi-vue-pro','serverUuid':UUID} and receipt.get('sourceFactsSha256')==sha(facts) and receipt.get('resultsSha256')==sha(results_blob),'actual receipt/facts/results raw SHA identity differs')
 seen=set();missing=[];matches=0
 for line in results_blob.decode('utf-8').splitlines():
  row=strict(line);require(set(row)=={'id','status','actualSha256','actualLength','httpStatus','errorCode'},'exact reader output protocol required')
  key=ident(row['id']);require(key in sources and key not in seen,'source output ID scope duplicate/foreign');seen.add(key)
  if row['status']=='MATCH':
   digest(row['actualSha256']);require(row['actualSha256']==digest(sources[key]['sourceSha256']) and type(row['actualLength']) is int and row['actualLength']>=0 and row['httpStatus']==200 and row['errorCode'] is None,'actual MATCH proof contradicts expected source')
   storage=groups.get('storage',{}).get(key);require(storage is not None and str(row['actualLength'])==storage.get('size'),'actual source length differs');matches+=1
  else:
   require(row['status'] in {'HTTP_ERROR','READ_ERROR','MISMATCH'},'unknown source verification status');missing.append(key)
 require(seen==set(sources) and receipt.get('objectsRead')==len(sources) and receipt.get('matches')==matches,'actual complete reader coverage/count differs')
 complete=not missing
 require(receipt.get('readerExitCode')==(0 if complete else 1) and receipt.get('sourceBytesVerified') is complete and receipt.get('status')==('SOURCE_BYTES_VERIFIED' if complete else 'SOURCE_BYTES_NOT_VERIFIED'),'actual status/exit contradict coverage')
 return {'status':'COMPLETE_MATCH_SOURCES' if complete else 'BLOCKED_INCOMPLETE_SOURCES','matches':matches,'sources':len(sources),'missingSourceIds':sorted(missing,key=int),'activationManifestGenerated':False}
def build(facts,receipt_blob,results_blob,decision_blob,preimages,scope_id,reason,request_id):
 byte_status=verify_bytes(facts,receipt_blob,results_blob);require(byte_status['status']=='COMPLETE_MATCH_SOURCES','incomplete sources forbid activation manifest')
 groups,runtime=facts_index(facts);receipt=strict(receipt_blob);decision=strict(decision_blob)
 policy=decision.get('historicalNamesDecision',{});require(all(policy.get(k) is True for k in ['preserveHistoricalFilesVersionsNamesAndSignatures','verifiedHistoricalOriginalNamesRemainOccupied','rejectFutureNewExactDuplicateNames']) and policy.get('autoRenameMergeOrDeleteHistoricalRecords') is False,'actual historical user decision required')
 require(re.fullmatch(r'[A-Za-z0-9_-]{1,64}',scope_id or '') and isinstance(reason,str) and 0<len(reason.strip())<=500 and isinstance(request_id,str) and 0<len(request_id.strip())<=128,'scope/reason/request identity invalid')
 require(preimages.get('status')=='ACTUAL_JDBC_JAVA_ROW_HASH_RECEIPT' and preimages.get('database')=='ruoyi-vue-pro' and preimages.get('serverUuid')==UUID and preimages.get('tenantId')=='1' and preimages.get('factsSha256')==sha(facts) and preimages.get('canonicalProtocol')==CANONICAL_PROTOCOL,'actual Java canonical preimage collector receipt required, SQL snapshot hash is different')
 require(preimages.get('actualReadOnlyConnection') is True and preimages.get('snapshotIsolation')=='REPEATABLE_READ' and preimages.get('jdbcRuntimeVersion')=='8.0.40' and preimages.get('mapperNullProbe')=='{"nil":null}','actual JDBC and runtime canonical profile proof required')
 captured=preimages.get('capturedAtUtc');require(isinstance(captured,str),'actual preimage capture UTC time required');captured_time=datetime.fromisoformat(captured.replace('Z','+00:00'));require(captured_time.tzinfo is not None and captured_time.utcoffset()==timedelta(0) and 0<=(datetime.now(timezone.utc)-captured_time).total_seconds()<=900,'fresh actual preimage snapshot required, not authorization expiry')
 hashes={}
 for row in preimages.get('rows',[]):
  key=(row.get('kind'),ident(row.get('id')));require(key not in hashes and key[0] in {'claim','master','version','storage'},'canonical preimage row duplicate/unknown');hashes[key]=digest(row.get('rowHash'))
  require(isinstance(row.get('identity'),dict),'actual JDBC immutable identity projection required')
  fact=groups.get(key[0],{}).get(key[1]);require(fact is not None,'preimage identity outside actual facts')
  expected={'claim':{'tenant_id':'tenantId','master_id':'masterId','normalized_name':'normalizedName','dcc_project_code_id':'projectId','file_type_taxonomy_leaf_id':'leafId','normalized_file_number':'normalizedNumber'},'master':{'tenant_id':'tenantId','dcc_project_code_id':'projectId','file_type_taxonomy_leaf_id':'leafId','normalized_file_number':'normalizedNumber'},'version':{'tenant_id':'tenantId','master_id':'masterId','source_file_id':'sourceFileId','source_sha256':'sourceSha256','version_no':'versionNo','process_instance_id':'processInstanceId'},'storage':{'config_id':'configId','name':'name','size':'size'}}[key[0]]
  require(all(row['identity'].get(field)==fact.get(source) for field,source in expected.items()),'current JDBC preimage identity differs from frozen facts')
  if key[0] in {'claim','version'}:require(row['identity'].get('source_original_file_name') is None,'historical original-name field must remain NULL')
 for kind in ['claim','master','version','storage']:require({key[1] for key in hashes if key[0]==kind}==set(groups.get(kind,{})),'full Java raw row scope differs: '+kind)
 storage_rows={}
 for row in preimages.get('storage',[]):key=ident(row['id']);require(key not in storage_rows,'duplicate actual locator row');storage_rows[key]=row
 require(set(storage_rows)==set(groups['storage']),'actual storage locator coverage differs')
 config=preimages.get('config',{});endpoint=urlsplit(config.get('endpoint',''))
 require(config.get('id')=='28' and config.get('storage')==20 and config.get('pathStyle') is True and config.get('region')=='us-east-1' and isinstance(config.get('bucket'),str) and config['bucket'].strip()
  and endpoint.scheme in {'http','https'} and endpoint.hostname in {'127.0.0.1','localhost'} and endpoint.port==9000 and endpoint.path in {'','/'} and not endpoint.username and not endpoint.password and not endpoint.query and not endpoint.fragment,'actual storage locator is outside reviewed localconfig28')
 claims_by_master={}
 for claim in groups['claim'].values():
  master=ident(claim['masterId']);require(master not in claims_by_master,'claim owner master ambiguity');claims_by_master[master]=claim
 require(set(claims_by_master)==set(groups['master']),'claim/master scope incomplete')
 evidence=[];owned=set();ticketed=set()
 for version in sorted(groups['version'].values(),key=lambda r:int(r['id'])):
  master_id=ident(version['masterId']);require(master_id in claims_by_master,'version not in scoped legacy master');claim=claims_by_master[master_id];master=groups['master'][master_id];source=ident(version['sourceFileId']);storage=groups['storage'].get(source);locator=storage_rows.get(source)
  require(storage and locator and storage.get('configId')=='28' and locator.get('configId')=='28' and locator.get('name')==storage.get('name') and locator.get('size')==storage.get('size') and isinstance(locator.get('path'),str) and locator['path'],'actual original storage name/size/path/config differs')
  name=storage['name'];require(isinstance(name,str) and 0<len(name)<=256 and '/' not in name and '\\' not in name and name.encode().hex().upper()==storage.get('nameHex'),'exact raw source basename bytes required')
  ownership=[r for r in groups.get('ownership',{}).values() if r.get('controlledFileId')==version['id']]
  require(len(ownership)==1 and ownership[0].get('sourceFileId')==source and ownership[0].get('sourceSha256')==version['sourceSha256'],'exact original ownership relation differs');owned.add(ownership[0]['id'])
  tickets=[r for r in groups.get('ticket',{}).values() if r.get('controlledFileId')==version['id'] and r.get('storageFileId')==source and r.get('purpose')=='SOURCE']
  require(tickets and all(r.get('status')=='BOUND' and r.get('cleanupStatus')=='BOUND' and r.get('fileSha256')==version['sourceSha256'] and r.get('originalName')==name and r.get('nameHex')==storage['nameHex'] and r.get('size')==storage['size'] for r in tickets),'exact bound source ticket relation differs');ticketed.update(r['id'] for r in tickets)
  for row in [claim,master]:
   for key in ['projectId','leafId']:
    if row.get(key) is not None:ident(row[key])
  require(all(claim.get(k) is None for k in ['projectId','leafId','normalizedNumber']),'oldclaim formal fields must remain NULL in reviewed scope')
  evidence.append({'claimId':claim['id'],'masterId':master_id,'fileId':version['id'],'sourceFileId':source,'configId':'28','versionNo':version['versionNo'],'sourceName':name,'sourcePath':locator['path'],'sourceSha256':digest(version['sourceSha256']),'sourceSize':int(storage['size']),'storageType':20,'storageEndpoint':config['endpoint'],'storageBucket':config['bucket'],'storageRegion':config['region'],'storagePathStyle':True,'claimName':claim['normalizedName'],'claimProjectId':None,'claimLeafId':None,'claimNumber':None,'masterProjectId':master.get('projectId'),'masterLeafId':master.get('leafId'),'masterNumber':master.get('normalizedNumber'),'processInstanceId':version.get('processInstanceId'),'filePreimageSha256':hashes[('version',version['id'])],'masterPreimageSha256':hashes[('master',master_id)],'claimPreimageSha256':hashes[('claim',claim['id'])],'storagePreimageSha256':hashes[('storage',source)]})
 require(owned==set(groups.get('ownership',{})),'unmatched ownership facts')
 identity=[{'claimId':e['claimId'],'masterId':e['masterId'],'fileId':e['fileId'],'sourceFileId':e['sourceFileId'],'sourceNameHex':e['sourceName'].encode().hex(),'sourceSha256':e['sourceSha256'],'sourceSize':str(e['sourceSize'])} for e in evidence]
 canonical=json.dumps(identity,sort_keys=True,ensure_ascii=False,separators=(',',':'))
 time_text=receipt.get('finishedAtUtc');require(isinstance(time_text,str) and re.search(r'(?:Z|\+00:00)$',time_text),'actual UTC byte read finished time required');finished=datetime.fromisoformat(time_text.replace('Z','+00:00'));verified=finished.astimezone(timezone(timedelta(hours=8))).replace(tzinfo=None).isoformat(timespec='microseconds')
 return {'schemaVersion':1,'database':'ruoyi-vue-pro','serverUuid':UUID,'tenantId':'1','scopeId':scope_id,'factsSha256':sha(facts),'bytesReceiptSha256':sha(receipt_blob),'userDecisionSha256':sha(decision_blob),'scopeIdentitySha256':sha(canonical.encode('utf-8')),'verifiedAt':verified,'reason':reason,'requestId':request_id,'evidence':evidence}

def reviewed_scope_contract(facts):
 groups,runtime=facts_index(facts)
 require(len(facts.decode('utf-8').splitlines())==283,'exact reviewed 283 original fact rows required')
 require({kind:len(groups.get(kind,{})) for kind in ['claim','master','version','storage','ownership','ticket','reference']}=={'claim':25,'master':25,'version':39,'storage':39,'ownership':39,'ticket':76,'reference':39},'exact reviewed original 25/25/39 scope differs')
 require(runtime.get('activeClaimCount')==25,'exact actual legacy claim scope differs')
 return {'factsSha256':sha(facts),'exactScopeIds':{kind:sorted(groups[kind],key=int) for kind in ['claim','master','version','storage']},'database':'ruoyi-vue-pro','serverUuid':UUID,'tenantId':'1'}

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--facts',type=Path,required=True);parser.add_argument('--receipt',type=Path,required=True);parser.add_argument('--results',type=Path,required=True);parser.add_argument('--decision',type=Path);parser.add_argument('--preimages',type=Path);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--scope-id');parser.add_argument('--reason');parser.add_argument('--request-id');args=parser.parse_args()
 require(not args.output.exists(),'new output directory required; never overwrite earlier bundle');facts=args.facts.read_bytes();receipt=args.receipt.read_bytes();results=args.results.read_bytes();reviewed_scope_contract(facts);status=verify_bytes(facts,receipt,results);args.output.mkdir()
 if status['status']!='COMPLETE_MATCH_SOURCES':(args.output/'blocked-source-diagnostic.json').write_bytes((json.dumps(status,indent=2)+'\n').encode());print(status['status']);return
 require(args.decision and args.preimages,'actual decision and Java preimages required');manifest=build(facts,receipt,results,args.decision.read_bytes(),strict(args.preimages.read_bytes()),args.scope_id,args.reason,args.request_id);(args.output/'activation-manifest.json').write_bytes((json.dumps(manifest,ensure_ascii=False,separators=(',',':'))+'\n').encode());print('MANIFEST_PREPARED_NOT_ACTIVATED')
if __name__=='__main__':main()
