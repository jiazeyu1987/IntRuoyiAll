"""Linux release entrypoints must remain executable after a Windows Git checkout."""
from pathlib import Path
import subprocess


def test_formal_linux_shell_checkout_uses_lf():
    root = Path(__file__).resolve().parents[4]
    scripts = sorted((root / 'IntRuoyiBackend/script/backup-ops').rglob('*.sh'))
    assert scripts
    for script in scripts:
        relative = script.relative_to(root).as_posix()
        result = subprocess.run(['git', '-C', str(root), 'check-attr', 'eol', '--', relative],
                                capture_output=True, text=True, check=True)
        assert result.stdout.strip() == relative + ': eol: lf', relative
        assert b'\r' not in script.read_bytes(), relative
