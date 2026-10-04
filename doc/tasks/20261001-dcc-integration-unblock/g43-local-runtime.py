"""Root-owned registered slot6 runtime launcher. Secrets stay in process environment."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')


def environment():
    result = os.environ.copy()
    docker = shutil.which('docker.exe')
    found = subprocess.run([docker, 'inspect', '--format={{json .Config.Env}}', 'int-ruoyi-mysql'], capture_output=True, check=True)
    values = json.loads(found.stdout)
    passwords = [v.split('=', 1)[1] for v in values if v.startswith('MYSQL_ROOT_PASSWORD=')]
    if len(passwords) != 1 or not passwords[0]:
        raise ValueError('Current local MySQL credential unavailable')
    source = (Path('C:/IntRuoyiAll-int_main') / 'IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1').read_text(encoding='utf-8-sig')
    for key in ['DCC_SIGNATURE_EVIDENCE_HMAC_SECRET', 'DCC_SIGNATURE_EVIDENCE_KEY_VERSION']:
        existing = re.findall(r'`?\$env:' + key + r"\s*=\s*'([^']+)'", source)
        if len(existing) != 1 or not existing[0]:
            raise ValueError('Existing formal local signature configuration missing')
        result[key] = existing[0]
    result.update(SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL="jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&sessionVariables=time_zone='%2B08:00'",
                  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME='root',
                  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=passwords[0],
                  SPRING_DATA_REDIS_HOST='127.0.0.1', SPRING_DATA_REDIS_PORT='26379',
                  DCC_ONLYOFFICE_BASE_URL='http://127.0.0.1:8080', DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL='http://host.docker.internal:48067',
                  INTRUOYI_BACKEND_LOG_FILE=str(PROTECTED / 'g44-local-backend/application.log'),
                  INTRUOYI_RUNTIME_PROFILE='int_qms')
    result['PATH'] = 'C:/IntRuoyiAll-int_main/.runtime/tools/jdk-17/bin;' + result['PATH']
    # Reuse the existing local configuration in memory, with a complete RAM Quartz map.
    # Loading the local profile itself would merge JDBC-only Quartz setters into RAMJobStore.
    node = shutil.which('node.exe')
    loader = REPO / 'IntRuoyiFronted/node_modules/.pnpm/js-yaml@4.1.0/node_modules/js-yaml/index.js'
    source_yaml = REPO / 'IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml'
    script = "const fs=require('fs'),y=require(process.argv[1]);function merge(a,b){for(const[k,v]of Object.entries(b||{})){if(v&&typeof v==='object'&&!Array.isArray(v))a[k]=merge(a[k]&&typeof a[k]==='object'?a[k]:{},v);else a[k]=v}return a}let d={};y.loadAll(fs.readFileSync(process.argv[2],'utf8'),v=>merge(d,v));process.stdout.write(JSON.stringify(d));"
    parsed = subprocess.run([node, '-e', script, str(loader), str(source_yaml)], capture_output=True)
    if parsed.returncode or parsed.stderr:
        raise ValueError('Existing local YAML configuration could not be read safely')
    configuration = json.loads(parsed.stdout)
    master = configuration['spring']['datasource']['dynamic']['datasource']['master']
    master.update(url=result['SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL'], username='root', password=passwords[0])
    configuration['spring']['data']['redis'].update(host='127.0.0.1', port=26379)
    configuration['spring']['data']['redis'].pop('password', None)
    configuration['spring']['quartz'].update(autoStartup=False, **{'auto-startup': False, 'job-store-type': 'memory', 'scheduler-name': 'dcc-integration-slot6'})
    configuration['spring']['quartz']['properties'] = {'org': {'quartz': {'scheduler': {'instanceName': 'dcc-integration-slot6', 'instanceId': 'AUTO'},
        'jobStore': {'class': 'org.quartz.simpl.RAMJobStore', 'misfireThreshold': 60000},
        'threadPool': {'class': 'org.quartz.simpl.SimpleThreadPool', 'threadCount': 3, 'threadPriority': 5}}}}
    # The unrelated MES receipt service defines empty defaults and rejects issue/verify when unconfigured.
    # Match that existing disabled behavior; do not invent an issuer or signing credential for DCC.
    configuration['mes']['pro']['edhr']['independent-receipt'] = {'issuer-system': '', 'signing-secret': ''}
    result['SPRING_APPLICATION_JSON'] = json.dumps(configuration, ensure_ascii=False)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['start'])
    args = parser.parse_args()
    guard = subprocess.run(['pwsh', '-NoProfile', '-File', str(REPO / 'scripts/preflight/branch-runtime-port-guard.ps1')], cwd=REPO, capture_output=True)
    if guard.returncode:
        raise ValueError('Registered task branch runtime port guard failed')
    checks = subprocess.run(['pwsh', '-NoProfile', '-Command', "$p=@(Get-NetTCPConnection -LocalPort 48067 -State Listen -ErrorAction SilentlyContinue);if($p.Count){exit 1};exit 0"], capture_output=True)
    if checks.returncode:
        raise ValueError('Task ports already occupied')
    # The authoritative branch launcher revalidates registered slot and existing Jar.
    directory = PROTECTED / 'g44-local-backend'
    if directory.exists():
        raise ValueError('Existing local runtime receipts require ownership review')
    directory.mkdir()
    overrides = [s for s in json.loads((ROOT / 'g21-runtime-plan.json').read_text(encoding='utf-8-sig'))['backendOverrides'] if not s.startswith('--spring.quartz.')]
    overrides += ['--spring.quartz.auto-startup=false', '--flowable.async-executor-activate=false',
                  '--flowable.async-history-executor-activate=false',
                  "--spring.datasource.dynamic.datasource.master.druid.init-connection-sqls=SET time_zone='+08:00'",
                  '--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl']
    for logger in ['cn.iocoder.yudao.module.system.service.oauth2', 'cn.iocoder.yudao.module.system.dal.mysql.oauth2',
                   'cn.iocoder.yudao.module.system.dal.redis.oauth2', 'org.springframework.data.redis', 'io.lettuce.core.protocol',
                   'org.mybatis.spring', 'org.apache.ibatis', 'org.springframework.jdbc.core']:
        overrides.append('--logging.level.' + logger + '=OFF')
    java = Path('C:/IntRuoyiAll-int_main/.runtime/tools/jdk-17/bin/java.exe')
    jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
    arguments = [str(java), '-Dfile.encoding=UTF-8', '-Duser.timezone=Asia/Shanghai', '-jar', str(jar), '--server.port=48067', '--spring.profiles.active=dcc-local-development', *overrides]
    with (directory / 'stdout.log').open('xb') as stdout, (directory / 'stderr.log').open('xb') as stderr:
        process = subprocess.Popen(arguments, cwd=REPO / 'IntRuoyiBackend', env=environment(), stdout=stdout, stderr=stderr,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
    jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
    receipt = {'status': 'OWNED_SLOT6_RUNTIME_STARTING', 'launcherPid': process.pid, 'profile': 'dcc-local-development', 'slot': 6,
               'frontendPort': 8067, 'backendPort': 48067, 'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
               'runtimeDirectory': str(directory), 'credentialValuesPersisted': False, 'businessAcceptanceComplete': False,
               'autoJobsEnabled': False, 'secretEnvironmentNamesOnly': ['mysqlPassword', 'existingHmacSecret', 'existingHmacVersion']}
    (ROOT / 'g43-runtime-start-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': receipt['status'], 'launcherPid': process.pid, 'backendPort': 48067}))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'status': 'LOCAL_RUNTIME_START_FAILED', 'errorType': type(error).__name__}))
        raise SystemExit(1) from None
