"""Real owned two-engine fixture; no host Docker socket or live peer is passed."""
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import uuid

APP = Path(__file__).resolve().parents[4]
PREFIX = 'owned-external-callers-' + uuid.uuid4().hex[:12]
OUTPUT = Path('D:/ProjectPackage/Int/IntRuoyiMaintance/doc/tasks/20260927-three-environment-release')
NATIVE='sha256:070da5e6fd4a490e009ce13c042339c673724b577520f624a1bc0c4b85a7b10d'


def docker(*args, timeout=90):
    result = subprocess.run(['docker', *args], capture_output=True, timeout=timeout)
    if result.returncode: raise RuntimeError('OWNED_CALLER_OUTER_DOCKER_FAILED')
    return result.stdout


containers = []; volumes = []
with tempfile.TemporaryDirectory(prefix=PREFIX) as temporary:
    root = Path(temporary)
    try:
        (root/'health-nginx.conf').write_text('server { listen 48081; location = /actuator/health { default_type application/json; return 200 \'{"status":"UP"}\'; } }\n')
        docker('save', '-o', str(root / 'images.tar'), 'nginx:1.27-alpine','intruoyi-owned-nft-fixture:20261003')
        assert docker('image','inspect','--format','{{.Id}}','intruoyi-owned-nft-fixture:20261003').decode().strip()==NATIVE
        for role in ('test', 'backup'):
            name = PREFIX + '-' + role
            for suffix in ('data', 'socket'):
                volume = name + '-' + suffix
                docker('volume', 'create', '--label', 'task-owner=' + PREFIX, volume); volumes.append(volume)
            docker('run', '-d', '--name', name, '--hostname', name, '--label', 'task-owner=' + PREFIX,
                '--privileged', '--network', 'none', '-e', 'DOCKER_TLS_CERTDIR=',
                '--mount', 'type=volume,src=' + name + '-data,dst=/var/lib/docker',
                '--mount', 'type=volume,src=' + name + '-socket,dst=/var/run',
                '--mount', 'type=bind,src=' + str(root) + ',dst=/fixtures,readonly',
                '--mount', 'type=bind,src=' + str(APP) + ',dst=/work,readonly',
                'docker:26.1.4-dind', 'dockerd', '--host', 'unix:///var/run/docker.sock')
            containers.append(name)
        docker('cp', containers[0] + ':/usr/local/bin/docker', str(root / 'docker'))
        driver = PREFIX + '-driver'
        docker('create', '--name', driver, '--label', 'task-owner=' + PREFIX, '--network', 'none',
            '-e', 'OWNED_CALLER_PREFIX=' + PREFIX, '--tmpfs', '/owned:rw,size=32m,mode=700',
            '--mount', 'type=bind,src=' + str(APP) + ',dst=/work,readonly',
            '--mount', 'type=bind,src=' + str(root) + ',dst=/fixtures,readonly',
            '--mount', 'type=bind,src=' + str(root / 'docker') + ',dst=/usr/local/bin/docker,readonly',
            '--mount', 'type=volume,src=' + PREFIX + '-test-socket,dst=/engines/test',
            '--mount', 'type=volume,src=' + PREFIX + '-backup-socket,dst=/engines/backup',
            'aa88ecce96d1', 'python3',
            '/work/IntRuoyiBackend/script/backup-ops/tests/fixtures/external_callers_real_driver.py')
        containers.append(driver)
        result = subprocess.run(['docker', 'start', '-a', driver], capture_output=True, timeout=240)
        code = docker('inspect', '--format', '{{.State.ExitCode}}', driver).decode().strip()
        if result.returncode or code != '0':
            # No raw output: retain only the exception class and line counts.
            lines = result.stderr.decode(errors='replace').splitlines()
            codes=[line.split(': ',1)[1] for line in lines if line.startswith('formal_snapshot_linux.FormalSnapshotError: FORMAL_')]
            print(json.dumps({'state': 'OWNED_CALLER_FIXTURE_FAILED', 'driverExit': code,
                'exceptionType': lines[-1].split(':', 1)[0] if lines else None, 'fixedCodes':codes,'stderrLines': len(lines)}))
            raise RuntimeError('OWNED_CALLER_DRIVER_FAILED')
        receipt = json.loads(result.stdout); retained = receipt.pop('retained')
        target = OUTPUT / (PREFIX + '-retained'); target.mkdir()
        hashes = {}
        for name, encoded in retained.items():
            assert Path(name).name == name and name.endswith('.json')
            raw = base64.b64decode(encoded, validate=True); (target / name).write_bytes(raw)
            hashes[name] = hashlib.sha256(raw).hexdigest()
        receipt['retained'] = {'root': str(target), 'sha256': hashes}
        print(json.dumps(receipt))
    finally:
        for name in reversed(containers):
            assert docker('inspect', '--format', '{{index .Config.Labels "task-owner"}}', name).decode().strip() == PREFIX
            if docker('inspect', '--format', '{{.State.Running}}', name).decode().strip() == 'true':
                docker('stop', '--time', '20', name)
            docker('rm', name)
        for name in reversed(volumes):
            assert docker('volume', 'inspect', '--format', '{{index .Labels "task-owner"}}', name).decode().strip() == PREFIX
            docker('volume', 'rm', name)
        print(json.dumps({'ownedFixtureCleanupComplete': True, 'prefix': PREFIX}))
