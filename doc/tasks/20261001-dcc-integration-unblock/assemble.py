from pathlib import Path
import subprocess, json, hashlib, difflib

ROOT = Path('C:/IntRuoyiAll-int_main')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
workers = {m: Path(f'C:/IntRuoyi/20260930-dcc-{m}') for m in 'abcd'}

def normalized(raw):
    return raw.decode('utf-8-sig').replace('\r\n','\n').replace('\r','\n')

inventory = {}
for module, worker in workers.items():
    assert subprocess.check_output(['git','-C',str(worker),'rev-parse','HEAD'],text=True).strip()==BASE
    entries = subprocess.check_output(['git','-C',str(worker),'status','--porcelain=v1','-z',
                                      '--untracked-files=all','--','IntRuoyiBackend','IntRuoyiFronted']).decode().split('\0')
    for entry in entries:
        if not entry: continue
        path = entry[3:]
        if entry[:2].strip() in ['D','R']: raise RuntimeError('Review deletion/rename separately: '+path)
        raw = (worker/path).read_bytes()
        inventory.setdefault(path,[]).append((module,raw))

manifest = {'base':BASE,'target':str(TARGET),'files':[]}
outputs = {}
for path, sources in inventory.items():
    merged = sources[0][1]
    if len(sources)>1:
        base = normalized(subprocess.check_output(['git','-C',str(ROOT),'show',BASE+':'+path]))
        lines = base.splitlines(keepends=True)
        edits=[]
        for module,raw in sources:
            current=normalized(raw).splitlines(keepends=True)
            for tag,start,end,nstart,nend in difflib.SequenceMatcher(a=lines,b=current,autojunk=False).get_opcodes():
                if tag=='equal':continue
                replacement=current[nstart:nend]
                exact=next((e for e in edits if e[0]==start and e[1]==end and e[2]==replacement),None)
                if exact:continue
                for previous in edits:
                    a,b,_,owner=previous
                    if (max(start,a)<min(end,b)) or (start==end and a<start<b) or (a==b and start<a<end):
                        raise RuntimeError(f'Overlapping non-identical edits in {path}: {owner}/{module} {a}:{b}/{start}:{end}')
                edits.append((start,end,replacement,module))
        rebuilt=[];cursor=0
        for start,end,replacement,module in sorted(edits,key=lambda e:(e[0],e[1],e[3])):
            if start<cursor:raise RuntimeError('Cannot merge ordered edits: '+path)
            rebuilt.extend(lines[cursor:start]);rebuilt.extend(replacement);cursor=end
        rebuilt.extend(lines[cursor:])
        merged=''.join(rebuilt).encode('utf-8')
    outputs[path]=merged
    manifest['files'].append({'path':path,'owners':[m for m,_ in sources],
                             'sources':{m:hashlib.sha256(raw).hexdigest() for m,raw in sources},
                             'assembled_sha256':hashlib.sha256(merged).hexdigest()})

# All reviewable edits are calculated before writing the isolated target.
for path,raw in outputs.items():
    destination=TARGET/path
    assert destination.resolve().is_relative_to(TARGET.resolve())
    destination.parent.mkdir(parents=True,exist_ok=True)
    destination.write_bytes(raw)
(TASK/'integration-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'result':'PASS','assembled_files':len(outputs),'overlap_files':sum(len(v)>1 for v in inventory.values()),'target':str(TARGET)}))
