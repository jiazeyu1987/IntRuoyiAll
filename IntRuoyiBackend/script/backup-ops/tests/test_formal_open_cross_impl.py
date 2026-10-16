"""Java-issued signed bytes through actual native OpenSSL; task-only public key."""
import hashlib
import importlib
import json
from pathlib import Path
import sys
import tempfile
import time

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))


def test_actual_java_ed25519_approval_verifies_without_resigning():
    with tempfile.TemporaryDirectory(prefix='cross-',dir='/control') as directory:
        root=Path(directory);key=root/'public.der';approval=root/'approval.json'
        key.write_bytes(Path('/cross/java-public.der').read_bytes());key.chmod(0o644)
        approval.write_bytes(Path('/cross/java-approval.json').read_bytes());approval.chmod(0o600)
        binding=json.loads(Path('/cross/java-binding.json').read_bytes())
        module=importlib.import_module('formal_open_authorization')
        value=module.verify_original_open_approval(str(approval),str(key),hashlib.sha256(key.read_bytes()).hexdigest(),
            binding,now_seconds=int(time.time()),guard=lambda:None)
        assert value['actorId']==17 and value['tenantId']==122
        assert value['approvalSha256']==hashlib.sha256(Path('/cross/java-approval.json').read_bytes()).hexdigest()
        assert value['approvalConsumed'] is False and value['releaseReady'] is False
