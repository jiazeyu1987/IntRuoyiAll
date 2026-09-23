"""Actual Linux entry + filesystem/flock; Docker/network are isolated unit boundaries."""
from pathlib import Path
import os
import subprocess
import uuid

ENTRY = Path(__file__).resolve().parents[1] / 'linux/backup_ops_linux.py'


def run_scenario(scenario):
    module = ENTRY.parent / 'runtime_resource_lease.py'
    module_source = module.read_text(encoding='utf-8') if module.exists() else ''
    root = '/tmp/native-lease-test-' + uuid.uuid4().hex
    source = ENTRY.read_text(encoding='utf-8-sig')
    script = f'''import json,pathlib,sys,shutil,types
scope={{'__name__':'native_lease_test'}}
if {bool(module_source)!r}:
    module=types.ModuleType('runtime_resource_lease');exec({module_source!r},module.__dict__);sys.modules['runtime_resource_lease']=module
exec({source!r},scope)
root=pathlib.Path({root!r});runtime=root/'runtime';runtime.mkdir(parents=True)
(runtime/'.env').write_text('IMAGE_TAG=old\\nBACKEND_HOST_PORT=48081\\nFRONTEND_HOST_PORT=8081\\nMYSQL_ROOT_PASSWORD=unit-fixture\\n')
(runtime/'docker-compose.yml').write_text('services: unit-boundary')
packages=root/'packages';(packages/'new').mkdir(parents=True)
(packages/'new'/'rollback-compatibility.json').write_text('{{"status":"COMPATIBLE"}}')
config={{'environment':'backup','console':{{'logRoot':str(root/'logs')}},'servers':{{'production':{{'host':'172.30.30.59','appDir':str(runtime),'tmpRoot':str(root/'tmp')}},'test':{{'backupPointsRoot':str(root/'backups')}},'backup':{{'releasePackagesRoot':str(packages)}}}}}}
(root/'backups').mkdir()
runner=scope['Runner'](config,'rollback-app')
runner.expected_target_host='172.30.30.59';runner.expected_runtime_dir=str(runtime)
if {bool(module_source)!r}:
    module.verify_physical_runtime=lambda *args,**kwargs:None
    module.collect_acceptance=lambda *args,**kwargs:{{'imageTag':'new','backendImage':'intruoyi-backend:new','frontendImage':'intruoyi-frontend:new','healthStatus':'UP','frontendHttp':200}}
scope['wait_http_ok']=lambda *args,**kwargs:None
original_run=scope['subprocess'].run
class Success:
    stdout='';returncode=0
scope['subprocess'].run=lambda *args,**kwargs:Success()
try:
{''.join('    '+line+'\n' for line in scenario.splitlines())}
finally:
    scope['subprocess'].run=original_run
    shutil.rmtree(root)
'''
    result = subprocess.run(['wsl.exe','-d','Ubuntu','--exec','env','WSL_UTF8=1','python3','-'],input=script.encode(),capture_output=True,
                            timeout=30,env={**os.environ,'WSL_UTF8':'1'})
    assert result.returncode == 0, result.stderr.decode('utf-8')


def test_actual_rollback_entry_cannot_write_while_publish_owner_exists():
    run_scenario('''owner=runtime/'.runtime-control-resource.owner';owner.write_text('c'*32)
before=(runtime/'.env').read_bytes()
try:scope['rollback_app'](config,runner,'new')
except Exception:pass
else:raise AssertionError('native rollback bypassed active publish owner')
assert (runtime/'.env').read_bytes()==before
assert owner.read_text()=='c'*32''')


def test_actual_rollback_entry_records_acceptance_before_releasing_owner():
    run_scenario('''context=scope['rollback_app'](config,runner,'new')
assert context['imageTag']=='new'
assert not (runtime/'.runtime-control-resource.owner').exists()
proofs=list((runtime/'.runtime-control-resource.native-results').glob('*.json'))
assert len(proofs)==1 and json.loads(proofs[0].read_text())['status']=='VERIFIED' ''')


def test_failed_native_entry_retains_persistent_isolation():
    run_scenario('''def failed(*args,**kwargs):raise RuntimeError('injected Docker failure')
runner.run=failed
try:scope['rollback_app'](config,runner,'new')
except RuntimeError:pass
else:raise AssertionError('injected failure was ignored')
assert (runtime/'.runtime-control-resource.owner').is_file()''')
