"""Read-only current asset inventory; conservative staging recommendations, no Git mutation."""
from pathlib import Path
import collections
import hashlib
import json
import re
import subprocess
from datetime import datetime, timezone

MAIN=Path('C:/IntRuoyiAll-int_main')
INTEGRATION=Path('C:/IntRuoyi/20261001-dcc-integration')
ROOT=Path(__file__).resolve().parent
OLD=INTEGRATION/'doc/tasks/20261002-dcc-public-browser/g21-git-candidate-manifest.json'
RAW=re.compile(r'\.(?:log|png|jpe?g|webp|zip|jar|class|dumpstream|args|bak|tmp)$|/(?:target|dist|node_modules|__pycache__|.*-runtime|e2e-artifacts|isolated-[^/]+)(?:/|$)',re.I)

def git(root,*args):
    r=subprocess.run(['git','-c','core.quotepath=false',*args],cwd=root,capture_output=True,check=True)
    return r.stdout.decode('utf-8')

def keep_paths(root):
    result=set()
    task_names=['20261001-dcc-integration-unblock'] if root==MAIN else ['20261002-dcc-detail-integration','20261002-dcc-public-backend-completion','20261002-dcc-public-browser','20261003-g19-job-startup-sync']
    for name in task_names:
        p=root/'doc/tasks'/name/'task.md'
        if not p.exists():continue
        s=p.read_text(encoding='utf-8-sig');match=re.search(r'^## Cleanup Keep\s*\n([\s\S]*?)(?=^## |\Z)',s,re.M)
        if match:
            for value in re.findall(r'^- ([^\n]+)',match.group(1),re.M):
                value=value.strip()
                if value.startswith('doc/tasks/') and not any(c in value for c in ('`','*')):result.add(value.replace('\\','/'))
        result.update('doc/tasks/'+name+'/'+f for f in ('task.md','execution-log.md','verification-report.md'))
    return result

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    old=json.loads(OLD.read_text(encoding='utf-8'));protected=json.loads((ROOT/'goal-preserved-nontask-assets.json').read_text())['preservedAssets']
    final=INTEGRATION/'doc/tasks/20261002-dcc-detail-integration/g27-legacy-final-delivery-fingerprints.json'
    final_assets={x['path']:x['sha256'] for x in json.loads(final.read_text())['files']}
    verified_protected=[]
    for x in protected:
        actual=sha(Path(x['workspace'])/x['path']);assert actual==x['sha256']
        verified_protected.append({**x,'actualSha256':actual,'unchanged':True})
    sections=[]
    for workspace in (INTEGRATION,MAIN):
        prior=next(s for s in old['workspaces'] if Path(s['workspace'])==workspace)
        prior_rows={x['path']:x for x in prior['entries']};preserve={x['path'] for x in protected if Path(x['workspace'])==workspace}
        keep=keep_paths(workspace)
        raw=git(workspace,'status','--porcelain=v1','-z','--untracked-files=all')
        entries=[]
        for item in filter(None,raw.split('\0')):
            status,path=item[:2],item[3:];p=workspace/path
            previous=prior_rows.get(path,{});owner='main supervisor' if path.startswith('doc/tasks/20261001-dcc-integration-unblock/') and workspace==MAIN else None
            if path in preserve:decision='preserve_non_task_asset'
            elif RAW.search(path):decision='exclude_generated_or_runtime_asset'
            elif workspace==INTEGRATION and path in final_assets:
                decision='candidate_final_reviewed_software' if p.exists() and sha(p)==final_assets[path] else 'manual_final_source_drift'
            elif path in keep:decision='candidate_exact_cleanup_keep_requires_secret_review'
            elif previous.get('decision','').startswith('candidate_'):
                decision='candidate_previous_scope_revalidate_current_hash'
            elif previous.get('decision','').startswith('excluded_'):decision=previous['decision']
            else:decision='manual_unfrozen_or_unproven_asset'
            entries.append({'path':path,'status':status,'decision':decision,'previousDecision':previous.get('decision'),
                            'owner':owner,'bytes':p.stat().st_size if p.is_file() else None,'sha256':sha(p) if p.is_file() else None})
        staged=[p for p in git(workspace,'diff','--cached','--name-only','-z').split('\0') if p]
        ignored=git(workspace,'ls-files','--others','--ignored','--exclude-standard','-z','--',*sorted(keep)).split('\0')
        ignored=[p for p in ignored if p and p in keep and not RAW.search(p)]
        sections.append({'workspace':str(workspace),'branch':git(workspace,'branch','--show-current').strip(),
                         'head':git(workspace,'rev-parse','HEAD').strip(),'stagedPaths':staged,'entries':entries,
                         'counts':dict(collections.Counter(x['decision'] for x in entries)),
                         'exactIgnoredKeepPathsNeedingForceAddReview':ignored})
    assert sections[0]['branch']=='codex/20261001-dcc-integration' and sections[1]['branch']=='int_qms'
    result={'status':'READ_ONLY_CANDIDATE_REVIEW_NOT_READY_TO_STAGE_OR_MERGE','generatedAtUtc':datetime.now(timezone.utc).isoformat(),
            'gitMutation':False,'sourceInventoryMayChangeDuringG28Preparation':True,'oldCandidateReceiptSha256':sha(OLD),
            'finalSoftwareManifestSha256':sha(final),'protected':verified_protected,'workspaces':sections,
            'limitations':['Candidate classification cannot prove business E2E, deployment or QA approval.','Review current manual assets and secrets before staging.','Existing unknown assets and unapproved intermediate runtime states must remain outside int_qms.']}
    path=ROOT/'g28-git-candidate-inventory.json';path.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'protectedUnchanged':len(verified_protected),'gitMutation':False,'workspaces':[{'branch':s['branch'],'counts':s['counts'],'stagedCount':len(s['stagedPaths']),'ignoredKeepCount':len(s['exactIgnoredKeepPathsNeedingForceAddReview'])} for s in sections]}))

if __name__=='__main__':main()
