from pathlib import Path
import subprocess,json,hashlib

ROOT=Path('C:/IntRuoyiAll-int_main')
INTEGRATED=Path('C:/IntRuoyi/20261001-dcc-integration')
TASK=ROOT/'doc/tasks/20261001-dcc-integration-unblock'
BASE='a801dc8b91579241e221d129ab34343997673f40'
snapshot=json.loads((TASK/'integration-manifest.json').read_text(encoding='utf-8'))
origins={e['path']:e for e in snapshot['files']}
previous_path=TASK/'dependency-sync-manifest.json'
previous=json.loads(previous_path.read_text(encoding='utf-8')) if previous_path.exists() else None
previous_hashes={e['path']:e['sha256'] for e in previous['files']} if previous else {}
entries=subprocess.check_output(['git','-C',str(INTEGRATED),'status','--porcelain=v1','-z','--untracked-files=all',
                                '--','IntRuoyiBackend','IntRuoyiFronted']).decode().split('\0')
payload={}
for entry in entries:
    if not entry:continue
    if entry[:2].strip() in ['D','R']:raise RuntimeError('Deletion/rename needs separate review')
    rel=entry[3:];payload[rel]=(INTEGRATED/rel).read_bytes()

# Validate every worker/file before any writes. Preserve changes made after the frozen handoff.
for module in 'abcd':
    worker=Path(f'C:/IntRuoyi/20260930-dcc-{module}')
    assert subprocess.check_output(['git','-C',str(worker),'rev-parse','HEAD'],text=True).strip()==BASE
    for rel,raw in payload.items():
        destination=worker/rel
        if rel in previous_hashes:
            if not destination.is_file() or hashlib.sha256(destination.read_bytes()).hexdigest()!=previous_hashes[rel]:
                raise RuntimeError(f'{module}: owner changed since last manager synchronization: {rel}')
        elif rel in origins and module in origins[rel]['sources']:
            expected=origins[rel]['sources'][module]
            if not destination.is_file() or hashlib.sha256(destination.read_bytes()).hexdigest()!=expected:
                raise RuntimeError(f'{module}: owner changed since handoff, refusing overwrite: {rel}')
        else:
            base=subprocess.run(['git','-C',str(ROOT),'show',BASE+':'+rel],capture_output=True)
            if base.returncode==0:
                expected=base.stdout.decode('utf-8-sig').replace('\r\n','\n').replace('\r','\n')
                if not destination.is_file() or destination.read_text(encoding='utf-8-sig')!=expected:
                    raise RuntimeError(f'{module}: non-owner pending edit would be overwritten: {rel}')
            elif destination.exists():
                raise RuntimeError(f'{module}: unrelated untracked file already exists: {rel}')

report={'source':str(INTEGRATED),'base':BASE,'files':[{ 'path':p,'sha256':hashlib.sha256(r).hexdigest() } for p,r in payload.items()],
        'workers':[],'services_started':False,'git_commits_or_merges':False}
for module in 'abcd':
    worker=Path(f'C:/IntRuoyi/20260930-dcc-{module}')
    for rel,raw in payload.items():
        destination=worker/rel;assert destination.resolve().is_relative_to(worker.resolve())
        destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(raw)
    report['workers'].append({'module':module,'path':str(worker),'files_synchronized':len(payload)})
(TASK/'dependency-sync-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'result':'PASS','files_per_worker':len(payload),'workers':4,'owner_changes_preserved':True,'git_commit_merge':False}))
