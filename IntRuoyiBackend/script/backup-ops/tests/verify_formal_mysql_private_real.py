"""Real Linux stdin/argv boundary with local fake executables, no DB proof."""
import importlib.util
import os
from pathlib import Path
import tempfile
import gzip

spec=importlib.util.spec_from_file_location('owned_formal_mysql',Path('/workspace/linux/formal_snapshot_linux.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
with tempfile.TemporaryDirectory(prefix='owned-formal-mysql-') as directory:
    root=Path(directory)
    docker=root/'docker'
    docker.write_text('#!/bin/sh\nset -eu\ntest "$1" = exec; test "$2" = -i; shift 3\nexec "$@"\n')
    for name in ['mysql','mysqldump']:
        file=root/name
        file.write_text('#!/bin/sh\nset -eu\ntest "$MYSQL_PWD" = "private-stdin-sentinel"\ncase "$*" in *private-stdin-sentinel*) exit 9;; esac\n'
            + ('cat\n' if name=='mysql' else 'printf "ACTUAL_EXECUTABLE_DUMP_BYTES\\n"\n'))
        file.chmod(0o700)
    docker.chmod(0o700);os.environ['PATH']=directory+':'+os.environ['PATH']
    mysql=module.PrivateDockerMySql('owned-container','private-stdin-sentinel',timeout=10)
    assert mysql.query('SELECT observed;')=='SELECT observed;'
    runtime_pins={file:file.read_bytes() for file in (docker,root/'mysql',root/'mysqldump')}
    def private_fixture_guard():
        # Explicit local executable identity; never a database/source fence.
        assert all(file.is_file() and file.read_bytes()==raw for file,raw in runtime_pins.items())
    receipt=mysql.dump('owned_database',root/'database.sql.gz',guard=private_fixture_guard)
    assert gzip.decompress((root/'database.sql.gz').read_bytes())==b'ACTUAL_EXECUTABLE_DUMP_BYTES\n'
    assert receipt['bytes']>0 and len(receipt['sha256'])==64
print('REAL_LINUX_STDIN_ARGV_AND_GZIP_BOUNDARIES_PASS_NOT_DATABASE_SNAPSHOT')
