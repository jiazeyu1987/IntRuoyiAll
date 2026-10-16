"""Pure validation in the actual supported host Python; no native owner emulation."""
import hashlib
from pathlib import Path
import sys

import pytest
sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_snapshot_linux as snapshot


def test_deep_json_has_fixed_journal_refusal():
    plan=b'{}\n'
    # Extra newline is invalid canonical journal on every runtime, including
    # native builds that can handle this depth without RecursionError.
    raw=b'{"details":{"nested":'+b'['*7000+b'0'+b']'*7000+b'},"previousDigest":"'+hashlib.sha256(plan).hexdigest().encode()+b'","sequence":1,"step":"maintenance-originals-bound"}\n\n'
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_MAINTENANCE_JOURNAL_INVALID'):
        snapshot._parse_maintenance_events(plan,[('event-000001.json',raw)],snapshot.FormalMaintenanceClaim._steps)
