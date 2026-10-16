import hashlib,json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_replica_producer as producer

@pytest.mark.parametrize('change',['pass','observed-digest','physical-digest','extra-secret-field'])
def test_cli_without_current_owner_rejects_even_matching_input_digests(tmp_path,change,capsys):
    physical=tmp_path/'physical.json';physical.write_bytes(b'{"physicalTreeDigest":"test"}')
    observed=tmp_path/'observed.json';observed.write_bytes(b'{"state":"test"}')
    secret=tmp_path/'secret.env';secret.write_text('MINIO_ROOT_USER=private-access\nMINIO_ROOT_PASSWORD=private-secret\n');secret.chmod(0o600)
    if change=='extra-secret-field':secret.write_text(secret.read_text()+'OTHER=value\n')
    args=['--operation-id','operation-1','--capture-root',str(tmp_path/'capture'),
        '--physical-receipt',str(physical),'--expected-physical-sha256',hashlib.sha256(physical.read_bytes()).hexdigest(),
        '--observations',str(observed),'--expected-observations-sha256',hashlib.sha256(observed.read_bytes()).hexdigest(),
        '--credential-file',str(secret),'--evidence-root',str(tmp_path/'evidence'),'--image-id','sha256:'+'1'*64]
    if change=='observed-digest':observed.write_text('{}')
    if change=='physical-digest':physical.write_text('{}')
    with patch.object(producer,'_require_linux'),patch.object(producer,'produce_isolated_replica_export',return_value={'state':'REPLICA_EXPORTED_PENDING_RESTORE_AND_FORMAL_BARRIERS','productionEvidence':False}) as dispatch:
        exit_code=producer.main(args)
        output=capsys.readouterr().out
        assert 'private-secret' not in output and 'private-access' not in output
        assert exit_code==1
        assert dispatch.call_count==0
        assert json.loads(output)['code']=='FORMAL_REPLICA_CURRENT_OWNER_REQUIRED'
        assert json.loads(output)['productionEvidence'] is False
