"""Start the reviewed main int_qms runtime; configuration secrets remain in memory."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess

TASK = Path(__file__).resolve().parent
REPO = TASK.parents[2]
PROTECTED = Path('C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime')

def environment():
    spec = importlib.util.spec_from_file_location('previous_runtime_configuration', TASK / 'g43-local-runtime.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.REPO = REPO
    module.PROTECTED = PROTECTED
    result = module.environment()
    result['DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL'] = 'http://host.docker.internal:48061'
    result['INTRUOYI_BACKEND_LOG_FILE'] = str(PROTECTED / 'backend/application.log')
    result['JAVA_TOOL_OPTIONS'] = '-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai'
    configuration = json.loads(result['SPRING_APPLICATION_JSON'])
    configuration['spring']['quartz']['scheduler-name'] = 'dcc-main-qms-acceptance'
    configuration['spring']['quartz']['properties']['org']['quartz']['scheduler']['instanceName'] = 'dcc-main-qms-acceptance'
    result['SPRING_APPLICATION_JSON'] = json.dumps(configuration, ensure_ascii=False)
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['backend','frontend'])
    parser.add_argument('--attempt', type=int, default=1)
    parser.add_argument('--enable-activation', action='store_true')
    arguments = parser.parse_args()
    branch = subprocess.run(['git','branch','--show-current'], cwd=REPO, capture_output=True, check=True).stdout.decode().strip()
    if branch != 'int_qms':
        raise ValueError('Unexpected current branch')
    proof = json.loads((TASK / 'g53-runtime-upgrade-root-review.json').read_text(encoding='utf-8-sig'))
    if proof['status'] != 'CLONE_AND_SOURCE_THREE_MIGRATIONS_FIRST_REPEAT_VERIFIED':
        raise ValueError('Actual approved runtime database upgrade has not passed')
    guard = subprocess.run(['pwsh','-NoProfile','-File',str(REPO / 'scripts/preflight/branch-runtime-port-guard.ps1')], cwd=REPO, capture_output=True)
    if guard.returncode:
        raise ValueError('Branch runtime guard failed')
    port = 48061 if arguments.mode == 'backend' else 8061
    check = subprocess.run(['pwsh','-NoProfile','-Command', f'$p=@(Get-NetTCPConnection -LocalPort {port} -State Listen -ErrorAction SilentlyContinue);if($p.Count){{exit 1}};exit 0'], capture_output=True)
    if check.returncode:
        raise ValueError('Current task port already occupied')
    if arguments.attempt < 1:
        raise ValueError('Explicit positive runtime attempt required')
    directory = PROTECTED / (arguments.mode if arguments.attempt == 1 else arguments.mode + '-attempt-' + str(arguments.attempt))
    if directory.exists():
        raise ValueError('Existing runtime output requires exact ownership review')
    directory.mkdir(parents=True)
    if arguments.mode == 'backend':
        jar = REPO / 'IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar'
        reviewed_package = json.loads((TASK / 'g62-package-root-review.json').read_text(encoding='utf-8-sig'))
        if reviewed_package['exitCode'] != 0 or hashlib.sha256(jar.read_bytes()).hexdigest() != reviewed_package['jarSha256']:
            raise ValueError('Reviewed executable package differs')
        overrides = [value for value in json.loads((TASK / 'g21-runtime-plan.json').read_text(encoding='utf-8-sig'))['backendOverrides']
                     if not value.startswith('--spring.quartz.')]
        overrides += ['--spring.quartz.auto-startup=' + ('true' if arguments.enable_activation else 'false'),
                      '--dcc.local-activation-startup-sync.enabled=' + ('true' if arguments.enable_activation else 'false'),
                      '--flowable.async-executor-activate=false','--flowable.async-history-executor-activate=false',
                      "--spring.datasource.dynamic.datasource.master.druid.init-connection-sqls=SET time_zone='+08:00'",
                      '--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl']
        if arguments.enable_activation:
            # This is the original task-owned registration created through the real job UI.
            overrides.append('--dcc.local-activation-startup-sync.job-id=5625')
        for logger in ['cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor',
                       'cn.iocoder.yudao.module.system.service.oauth2','cn.iocoder.yudao.module.system.dal.mysql.oauth2',
                       'cn.iocoder.yudao.module.system.dal.redis.oauth2','org.springframework.data.redis','io.lettuce.core.protocol',
                       'org.mybatis.spring','org.apache.ibatis','org.springframework.jdbc.core']:
            overrides.append('--logging.level.' + logger + '=OFF')
        quote = lambda value: "'" + value.replace("'", "''") + "'"
        command = '& ' + quote(str(REPO / 'scripts/runtime/start-branch-backend.ps1')) + " -SpringProfile 'dcc-local-development' -ExtraArgs @(" + ','.join(map(quote,overrides)) + ')'
        executable = ['pwsh','-NoProfile','-Command',command]
        cwd, env = REPO, environment()
        env['INTRUOYI_BACKEND_LOG_FILE'] = str(directory / 'application.log')
    else:
        env = os.environ.copy()
        env.update(VITE_PORT='8061',VITE_BASE_URL='http://127.0.0.1:48061',VITE_PROXY_TARGET='http://127.0.0.1:48061',
                   VITE_API_URL='/admin-api',VITE_APP_CAPTCHA_ENABLE='false',INTRUOYI_RUNTIME_PROFILE='int_qms')
        node = shutil.which('node.exe')
        if not node:
            raise ValueError('Existing Node runtime is required')
        cwd = REPO / 'IntRuoyiFronted'
        executable = [node,str(cwd / 'node_modules/vite/bin/vite.js'),'--mode','branch-qms','--host','127.0.0.1','--port','8061','--strictPort']
    with (directory / 'stdout.log').open('xb') as out,(directory / 'stderr.log').open('xb') as err:
        process = subprocess.Popen(executable,cwd=cwd,env=env,stdout=out,stderr=err,creationflags=subprocess.CREATE_NO_WINDOW)
    receipt = {'status':'OWNED_MAIN_QMS_RUNTIME_STARTING','mode':arguments.mode,'launcherPid':process.pid,
               'repository':REPO.as_posix(),'branch':branch,'slot':0,'port':port,'output':directory.as_posix(),
               'database':'ruoyi-vue-pro','credentialsPersisted':False,'backgroundJobsEnabled':arguments.enable_activation,
               'jobStartupSyncEnabled':False, 'ownedActivationStartupSyncEnabled':arguments.enable_activation,
               'automaticHandlerScope':'only separately registered dccControlledFileActivationJob',
               'realBusinessAcceptance':False,'attempt':arguments.attempt}
    receipt_name = 'g56-' + arguments.mode + '-start-receipt' + ('' if arguments.attempt == 1 else '-r' + str(arguments.attempt)) + '.json'
    (TASK / receipt_name).write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(receipt))

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'status':'START_PRECONDITION_FAILED','errorType':type(error).__name__}))
        raise SystemExit(1) from None
