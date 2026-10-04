"""Root actual authenticated dev maintenance after real Playwright login; secrets memory-only."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'
OUTPUT = PROTECTED / 'g43-legacy-activation-run-r5'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def desc(path):
    return {'path': str(path.resolve()), 'sha256': sha(path), 'bytes': path.stat().st_size}


def require_runtime_owner(start, jar):
    pid = start['launcherPid']
    if not isinstance(pid, int) or pid <= 0:
        raise ValueError('Invalid owned task process identity')
    quoted_jar = str(jar.resolve()).replace("'", "''")
    command = "$c=@(Get-NetTCPConnection -LocalPort 48067 -State Listen -ErrorAction Stop);$ids=@($c.OwningProcess|Sort-Object -Unique);if($ids.Count -ne 1 -or $ids[0] -ne " + str(pid) + "){exit 2};$p=Get-CimInstance Win32_Process -Filter 'ProcessId=" + str(pid) + "';if(-not $p -or $p.Name -ne 'java.exe' -or $p.CommandLine -notmatch '(?:^|\\s)-jar\\s+(?:\"([^\"]+)\"|([^\\s]+))'){exit 3};$actual=if($matches[1]){$matches[1]}else{$matches[2]};if([IO.Path]::GetFullPath($actual) -ne '" + quoted_jar + "' -or $p.CommandLine -notmatch '(?:^|\\s)--spring.profiles.active=dcc-local-development(?:$|\\s)'){exit 4};exit 0"
    result = subprocess.run(['pwsh', '-NoProfile', '-Command', command], capture_output=True)
    if result.returncode:
        raise ValueError('Listener PID, executable Jar or development profile does not match task ownership')


def history_snapshot():
    source = ROOT / 'g21_mysql_support.py'
    if sha(source) != '6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331':
        raise ValueError('Sealed read-only history support changed')
    spec = importlib.util.spec_from_file_location('g43_history_readonly_support', source)
    support = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(support)
    transport = support.LocalMysql('ruoyi-vue-pro')
    class ExactReadOnlySource:
        def read(self, sql):
            if re.search(r'INTO\s+(?:OUTFILE|DUMPFILE)|LOAD_FILE|GET_LOCK|RELEASE_LOCK|SLEEP|BENCHMARK', sql, re.I):
                raise ValueError('History query must be read-only')
            raw = transport.read("SELECT JSON_OBJECT('database',DATABASE(),'uuid',@@server_uuid,'version',VERSION());\n" + sql)
            envelope, facts = raw.split('\n', 1)
            if json.loads(envelope) != {'database': 'ruoyi-vue-pro', 'uuid': '92ca05d0-aec8-11f1-a944-02b4e226a5ef', 'version': '8.0.40'}:
                raise ValueError('Actual source history connection differs')
            return facts
    tables = ['dcc_controlled_file', 'dcc_controlled_file_master', 'dcc_controlled_file_name_claim',
              'dcc_controlled_file_source_ownership', 'dcc_controlled_file_signature', 'system_electronic_signature', 'infra_release_migration']
    return support.snapshot_original_rows(ExactReadOnlySource(), tables)


def main():
    if OUTPUT.exists():
        raise ValueError('Existing activation output; review actual outcome before another attempt')
    with urllib.request.urlopen('http://127.0.0.1:48067/actuator/health', timeout=5) as health:
        if health.status != 200 or json.load(health).get('status') != 'UP':
            raise ValueError('Current registered backend is not healthy')
    start = json.loads((ROOT / 'g43-runtime-start-receipt.json').read_text(encoding='utf-8-sig'))
    if start['profile'] != 'dcc-local-development' or start['backendPort'] != 48067:
        raise ValueError('Runtime ownership receipt differs')
    jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
    if sha(jar) != start['jarSha256']:
        raise ValueError('Current task Jar changed')
    require_runtime_owner(start, jar)
    node = shutil.which('node.exe')
    environment = os.environ.copy()
    instructions = (REPO / 'AGENTS.md').read_text(encoding='utf-8-sig')
    usernames = re.findall(r'^用户名\s+(\S+)\s*$', instructions, re.M)
    passwords = re.findall(r'^密码\s+(\S+)\s*$', instructions, re.M)
    if len(usernames) != 1 or len(passwords) != 1:
        raise ValueError('Existing user-provided E2E credentials are unavailable')
    environment.update(DCC_G43_LOGIN_SCOPE='AUTHORIZED_LOCAL_FRONTEND_LOGIN_FOR_MAINTENANCE', DCC_G43_USERNAME=usernames[0], DCC_G43_PASSWORD=passwords[0])
    del instructions, usernames, passwords
    logged = subprocess.run([node, str(ROOT / 'g43-maintenance-login.cjs')], cwd=REPO / 'IntRuoyiFronted', env=environment, capture_output=True)
    if logged.returncode or logged.stderr:
        raise ValueError('Real frontend login failed; no token or diagnostics printed')
    login = json.loads(logged.stdout)
    if login.get('status') != 'ACTUAL_FRONTEND_LOGIN_COMPLETE' or not login.get('accessToken'):
        raise ValueError('Actual frontend authentication absent')
    token = login['accessToken']
    del logged, login, environment
    OUTPUT.mkdir()
    for source, name in [(TASK / 'g43-dev-policy.json', 'development-policy.json'),
                         (ROOT / 'g25-user-authorization.json', 'historical-decision.json'),
                         (REPO / 'IntRuoyiBackend/config/gxp-audit-policy.yaml', 'development-audit-policy.yaml')]:
        (OUTPUT / name).write_bytes(source.read_bytes())
    manifest = PROTECTED / 'g39-legacy-manifest-inputs/activation-manifest.json'
    facts = PROTECTED / 'g23-legacy-name-facts.jsonl'
    body_receipt = PROTECTED / 'g39-all39-source-bytes/receipt.json'
    body_results = PROTECTED / 'g39-all39-source-bytes/source-bytes-results.jsonl'
    sealed = json.loads(manifest.read_text(encoding='utf-8-sig'))
    request = {'schemaVersion': 1, 'authorization': desc(PROTECTED / 'g39-legacy-registration-authorization.json'),
               'manifest': desc(manifest), 'facts': desc(facts), 'bytesReceipt': desc(body_receipt), 'bytesResults': desc(body_results),
               'historicalDecision': desc(OUTPUT / 'historical-decision.json'), 'developmentPolicy': desc(OUTPUT / 'development-policy.json'),
               'policyFile': desc(OUTPUT / 'development-audit-policy.yaml'), 'schemaProof': desc(PROTECTED / 'g39-source-schema-proof/schema-proof.json'),
               'scopeId': sealed['scopeId'], 'reason': sealed['reason'], 'requestId': sealed['requestId']}
    path = OUTPUT / 'request.json'
    path.write_text(json.dumps(request, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    receipt = {'status': 'REAL_FRONTEND_AUTHENTICATED_MAINTENANCE_PREPARED', 'requestSha256': sha(path), 'actualFrontendLogin': True,
               'actualQualityApproved': False, 'developmentOnly': True, 'activationWriteAttempted': False, 'credentialsPersisted': False}
    receipt_path = OUTPUT / 'receipt.json'
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    before = history_snapshot()
    (OUTPUT / 'original-before.json').write_text(json.dumps(before, indent=2) + '\n', encoding='utf-8')
    receipt.update(originalBefore=desc(OUTPUT / 'original-before.json'))
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    # Stop only the verified PID from this task's own latest runtime; keep the frontend available.
    pid = start['launcherPid']
    require_runtime_owner(start, jar)
    command = "$p=Get-CimInstance Win32_Process -Filter \"ProcessId=" + str(pid) + "\";if(-not $p -or $p.Name -ne 'java.exe' -or -not $p.CommandLine.Contains('20261001-dcc-integration') -or -not $p.CommandLine.Contains('--server.port=48067')){exit 2};Stop-Process -Id " + str(pid) + " -ErrorAction Stop;exit 0"
    stop = subprocess.run(['pwsh', '-NoProfile', '-Command', command], capture_output=True)
    if stop.returncode:
        raise ValueError('Task backend PID ownership could not be confirmed')
    spec = importlib.util.spec_from_file_location('g43_runtime_env', ROOT / 'g43-local-runtime.py')
    runtime = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runtime)
    env = runtime.environment()
    arguments = ['C:/IntRuoyiAll-int_main/.runtime/tools/jdk-17/bin/java.exe', '-Dfile.encoding=UTF-8', '-Duser.timezone=Asia/Shanghai', '-jar', str(jar), '--server.port=48067',
                 '--spring.profiles.active=dcc-local-development,local-maintenance',
                 '--yudao.dcc.legacy-registration-maintenance.enabled=true',
                 '--yudao.dcc.legacy-registration-maintenance.protected-root=' + str(PROTECTED),
                 '--yudao.dcc.legacy-registration-maintenance.request-file=' + str(path),
                 '--yudao.dcc.legacy-registration-maintenance.request-sha256=' + sha(path),
                 '--flowable.database-schema-update=false', '--flowable.async-executor-activate=false', '--spring.quartz.auto-startup=false',
                 '--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl',
                 "--spring.datasource.dynamic.datasource.master.druid.init-connection-sqls=SET time_zone='+08:00'"]
    flags = json.loads((ROOT / 'g21-runtime-plan.json').read_text(encoding='utf-8-sig'))['backendOverrides']
    arguments += [flag for flag in flags if not flag.startswith('--spring.quartz.')]
    for logger in ['cn.iocoder.yudao.module.system.service.oauth2', 'cn.iocoder.yudao.module.system.dal.mysql.oauth2',
                   'cn.iocoder.yudao.module.system.dal.redis.oauth2', 'org.springframework.data.redis', 'io.lettuce.core.protocol',
                   'org.mybatis.spring', 'org.apache.ibatis', 'org.springframework.jdbc.core']:
        arguments.append('--logging.level.' + logger + '=OFF')
    receipt.update(status='ACTUAL_AUTHENTICATED_DEVELOPMENT_MAINTENANCE_RUNNING', activationWriteAttempted=True)
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    # Startup/runner diagnostics stay private; token is never an argv/file/log value.
    with (OUTPUT / 'stdout.log').open('xb') as stdout, (OUTPUT / 'stderr.log').open('xb') as stderr:
        process = subprocess.Popen(arguments, cwd=REPO / 'IntRuoyiBackend', env=env, stdin=subprocess.PIPE, stdout=stdout, stderr=stderr, creationflags=subprocess.CREATE_NO_WINDOW)
        receipt.update(maintenancePid=process.pid, ownerJarSha256=sha(jar))
        receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
        try:
            process.communicate(input=token.encode('utf-8'), timeout=300)
        except subprocess.TimeoutExpired:
            receipt.update(status='MAINTENANCE_TIMEOUT_ACTUAL_STATE_REQUIRES_ROOT_REVIEW', automaticRetryAllowed=False)
            receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
            raise ValueError('Owned maintenance outcome requires explicit Root process and database review')
    del token, env
    text = (OUTPUT / 'stdout.log').read_text(encoding='utf-8', errors='replace')
    results = [json.loads(line) for line in text.splitlines() if line.startswith('{"status":"LEGACY_REGISTRATION_ACTUAL_RECEIPT"')]
    receipt.update(exitCode=process.returncode, status='ACTUAL_LEGACY_ACTIVATION_RECEIPT_RETURNED' if process.returncode == 0 and len(results) == 1 else 'STOPPED_REVIEW_ACTUAL_ACTIVATION_STATE_NO_RETRY',
                   activationReceipt=results[0] if len(results) == 1 else None, stdoutSha256=sha(OUTPUT / 'stdout.log'), stderrSha256=sha(OUTPUT / 'stderr.log'), automaticRetryAllowed=False)
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    after = history_snapshot()
    (OUTPUT / 'original-after.json').write_text(json.dumps(after, indent=2) + '\n', encoding='utf-8')
    history_unchanged = before == after
    receipt.update(originalAfter=desc(OUTPUT / 'original-after.json'), originalSevenTablesUnchanged=history_unchanged)
    if not history_unchanged:
        receipt['status'] = 'ACTUAL_REGISTRATION_RECEIPT_HISTORY_DIFF_REQUIRES_ROOT_REVIEW'
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: receipt[k] for k in ['status', 'exitCode', 'actualFrontendLogin', 'activationReceipt']}))
    if receipt['status'] != 'ACTUAL_LEGACY_ACTIVATION_RECEIPT_RETURNED':
        raise SystemExit(1)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'status': 'DEVELOPMENT_MAINTENANCE_STOPPED', 'errorType': type(error).__name__}))
        raise SystemExit(1) from None
