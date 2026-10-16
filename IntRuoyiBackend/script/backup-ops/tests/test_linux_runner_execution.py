"""Actual backup source-of-truth boundaries; no deployment success fixtures."""
import importlib.util
import subprocess
from pathlib import Path
from unittest.mock import patch
import pytest

SOURCE = Path(__file__).resolve().parents[1] / 'linux/backup_ops_linux.py'
spec = importlib.util.spec_from_file_location('owned_backup_linux', SOURCE)
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)

def test_runner_never_logs_or_raises_secret_bearing_command_output(tmp_path):
    runner = backup.Runner({'console': {'logRoot': str(tmp_path)}}, 'backup-now')
    secret = 'private-credential-sentinel'
    with patch.object(backup.subprocess, 'run', return_value=subprocess.CompletedProcess([], 1, secret)):
        with pytest.raises(backup.BackupOpsError) as failure:
            runner.run('MYSQL_PWD='+secret+' mysql', 'INTBK-3001', 'Snapshot failed')
    assert secret not in runner.log_path.read_text(encoding='utf-8')
    assert secret not in str(failure.value)
    assert 'INTBK-3001' in runner.log_path.read_text(encoding='utf-8')

def test_runner_enables_pipefail_and_returns_raw_output_only_to_caller(tmp_path):
    runner = backup.Runner({'console': {'logRoot': str(tmp_path)}}, 'backup-now')
    with patch.object(backup.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, 'private-output')) as run:
        assert runner.run('false | true', 'INTBK-3001', 'Snapshot failed') == 'private-output'
        assert run.call_args.args[0].startswith('set -o pipefail\n')
    assert 'private-output' not in runner.log_path.read_text(encoding='utf-8')

def test_snapshot_compresses_outside_container_pipeline():
    source = SOURCE.read_text(encoding='utf-8')
    start = source.index('def backup_now(')
    body = source[start:source.index('\ndef restore_data(', start)]
    assert ' | gzip -c > {2}' in body
    assert ' {1} | gzip -c".format(' not in body

def test_legacy_backup_refuses_formal_before_any_runtime_or_export_access():
    config={'environment':'prod','servers':{'production':{'host':'172.30.30.57'}}}
    with patch.object(backup,'get_runtime_paths',side_effect=AssertionError('runtime access forbidden')) as runtime:
        with pytest.raises(backup.BackupOpsError) as failure:
            backup.backup_now(config,None,'FULL')
    assert failure.value.code=='INTBK-1003'
    assert 'test/172.30.30.58' in failure.value.message
    runtime.assert_not_called()
