"""Run only in an isolated Linux container; real shell failure, no server access."""
import importlib.util
import tempfile
from pathlib import Path

source=Path('/workspace/linux/backup_ops_linux.py')
spec=importlib.util.spec_from_file_location('real_backup_runner',source)
backup=importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)
with tempfile.TemporaryDirectory(prefix='owned-runner-') as directory:
    runner=backup.Runner({'console':{'logRoot':directory}},'backup-now')
    for command in ['false | true', 'true | false', 'printf badgzip | gunzip -c | cat']:
        try:
            runner.run(command,'INTBK-3001','Actual isolated pipeline failed')
        except backup.BackupOpsError as error:
            assert error.code=='INTBK-3001' and error.status=='fail'
        else:
            raise AssertionError('PIPELINE_FAILURE_WAS_HIDDEN')
    secret='isolated-private-sentinel'
    assert runner.run("printf '"+secret+"'",'INTBK-3001','Output failed')==secret
    assert secret not in runner.log_path.read_text()
    assert 'commandSha256=' in runner.log_path.read_text()
print('REAL_LINUX_PIPELINE_AND_LOG_BOUNDARIES_PASS')
