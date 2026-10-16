"""Explicit MySQL process doubles; real MySQL rejection verified separately."""
import importlib.util,subprocess
from pathlib import Path
from unittest.mock import patch
import pytest
ROOT=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'

@pytest.mark.parametrize('late',[False,True])
def test_write_rejection_probe_keeps_outer_deadline(late):
 spec=importlib.util.spec_from_file_location('probe',ROOT);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
 mysql=m.PrivateDockerMySql('owned-mysql','owned-private');clock=[10.]
 def describe(sql):clock[0]=11.;return 'id\tint\n'
 mysql.query=describe
 def execute(command,**kwargs):
  assert 0<kwargs['timeout']<=1.
  assert 'timeout --signal=TERM' in command[6] and '1s mysql' in command[6]
  if late:clock[0]=13.
  return subprocess.CompletedProcess([],1,b'',b'ERROR 1290 (HY000) at line 1: The MySQL server is running with the --super-read-only option so it cannot execute this statement\n')
 with patch.object(m.time,'monotonic',side_effect=lambda:clock[0]),patch.object(m.subprocess,'run',side_effect=execute):
  token=m.observation_deadline.set(12.)
  try:
   if late:
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):mysql.probe_rejected_zero_row_update('owned_probe','probe','id')
   else:assert mysql.probe_rejected_zero_row_update('owned_probe','probe','id')['result']=='REJECTED'
  finally:m.observation_deadline.reset(token)

@pytest.mark.parametrize('case',['rejected','accepted','permission','connection','unknown-column'])
def test_exact_zero_row_write_rejection(case):
 spec=importlib.util.spec_from_file_location('probe',ROOT);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
 mysql=m.PrivateDockerMySql('owned-mysql','owned-secret')
 mysql.query=lambda sql:'id\tint\tNO\tPRI\tNULL\t\n' if case!='unknown-column' else 'other\tint\n'
 stderr=b'ERROR 1290 (HY000) at line 1: The MySQL server is running with the --super-read-only option so it cannot execute this statement\n'
 if case=='permission':stderr=b'ERROR 1142 (42000) at line 1: UPDATE command denied\n'
 if case=='connection':stderr=b'ERROR 2002 (HY000): connection failed\n'
 result=subprocess.CompletedProcess([],0 if case=='accepted' else 1,b'',stderr)
 with patch.object(m.subprocess,'run',return_value=result) as run:
  if case=='rejected':
   actual=mysql.probe_rejected_zero_row_update('owned_probe','probe','id')
   assert actual['result']=='REJECTED' and actual['mysqlErrorNumber']==1290
  else:
   with pytest.raises(m.FormalSnapshotError):mysql.probe_rejected_zero_row_update('owned_probe','probe','id')
  if case=='unknown-column':assert not run.called
  else:
   assert run.call_args.kwargs['input']==b'owned-secret\nUPDATE `owned_probe`.`probe` SET `id`=`id` WHERE 1=0;'
   assert 'owned-secret' not in repr(run.call_args.args)
   command=run.call_args.args[0]
   assert '--no-login-paths' not in command and '--no-defaults' in command
   assert 'MYSQL_TEST_LOGIN_FILE' in command[6] and '.mylogin.cnf' in command[6]
