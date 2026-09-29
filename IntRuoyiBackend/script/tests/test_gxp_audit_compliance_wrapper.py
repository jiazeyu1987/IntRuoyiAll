"""Execute the real wrapper in isolated fixtures; Maven/Python are explicit doubles.

No Maven process, real Java test, database, or service is started by these tests.
"""
from pathlib import Path
import shutil
import subprocess

import pytest


WRAPPER = Path(__file__).resolve().parents[1] / 'release/run-gxp-audit-compliance-gate.ps1'
SHELL = 'pwsh'


@pytest.fixture(params=['pwsh', 'powershell'], autouse=True)
def powershell_version(request, monkeypatch):
    monkeypatch.setitem(globals(), 'SHELL', request.param)


NAMES = [
    'GxpAuditPolicyBundleLoaderTest', 'GxpAuditPolicyActivationTest',
    'GxpAuditPolicyActivationReplayBehaviorTest', 'GxpAuditPolicyActivationServiceContractTest',
    'GxpAuditV2ContractTest', 'GxpAuditServiceImplTest', 'GxpAuditQueryServiceImplTest',
    'GxpAuditQueryContractTest', 'GxpAuditAttemptServiceImplTest', 'GxpAuditPersistenceModelTest',
]


@pytest.fixture
def tmp_path(tmp_path_factory):
    """Keep pytest's caller-selected base, without long parameterized node names."""
    return tmp_path_factory.mktemp('w')


def run_gate(tmp_path, mode='ok', switches=()):
    # This is a synthetic layout, not the production checkout. Reserve headroom
    # below legacy Windows path limits; do not retry elsewhere on failure.
    backend = tmp_path / 'b'
    wrapper = backend / 'script/release' / WRAPPER.name
    sources = backend / 'yudao-module-system/src/test/java/example'
    reports = backend / 'yudao-module-system/target/surefire-reports'
    expected_paths = [wrapper, tmp_path / 'harness.ps1']
    for name in NAMES + ['GxpAuditNewBehaviorTest']:
        expected_paths.extend([
            sources / f'{name}.java',
            reports / f'TEST-example.{name}-gxp-{"0" * 32}.xml',
        ])
    longest = max(expected_paths, key=lambda path: len(str(path.resolve())))
    length = len(str(longest.resolve()))
    assert length <= 240, (
        f'Wrapper fixture path budget exceeded: {length} > 240: {longest}. '
        'Provide an explicitly shorter --basetemp; no fixture files were written.'
    )
    wrapper.parent.mkdir(parents=True)
    shutil.copyfile(WRAPPER, wrapper)
    sources.mkdir(parents=True)
    for name in NAMES + ['GxpAuditNewBehaviorTest']:
        if mode == 'missing-source' and name == 'GxpAuditServiceImplTest':
            continue
        (sources / f'{name}.java').write_text(f'package example; class {name} {{}}', encoding='utf-8')
    reports.mkdir(parents=True)
    for name in NAMES:
        (reports / f'TEST-example.{name}.xml').write_text(
            f'<testsuite name="example.{name}" tests="99" skipped="0" failures="0" errors="0"/>', encoding='utf-8')
    for relative in ['script/gxp_audit_coverage_gate.py', 'script/tests/test_gxp_audit_policy_static.py']:
        path = backend / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('# explicit fixture, intercepted by Python double', encoding='utf-8')
    harness = tmp_path / 'harness.ps1'
    harness.write_text(r'''
param([string]$Mode, [string]$Wrapper, [string[]]$Switches)
$ErrorActionPreference = 'Stop'
function global:mvn {
    Write-Output ('DOUBLE_MAVEN ' + ($args -join ' '))
    $global:LASTEXITCODE = 0
    if ($Mode -eq 'maven-fail') { $global:LASTEXITCODE = 17; return }
    if ($Mode -in @('no-reports', 'stale')) { return }
    $selector = @($args | Where-Object { $_ -like '-Dtest=*' })[0].Substring(7)
    $suffixArgs = @($args | Where-Object { $_ -like '-Dsurefire.reportNameSuffix=*' })
    $suffix = if ($suffixArgs.Count) { '-' + $suffixArgs[0].Split('=')[1] } else { '' }
    $reportRoot = Join-Path (Get-Location) 'yudao-module-system/target/surefire-reports'
    foreach ($target in $selector.Split(',')) {
        $name = $target.Split('.')[-1]
        if ($Mode -eq 'missing-report' -and $name -eq 'GxpAuditServiceImplTest') { continue }
        $tests = if ($Mode -eq 'zero') { 0 } else { 2 }
        $skipped = if ($Mode -eq 'all-skipped') { 2 } elseif ($Mode -eq 'partial-skipped') { 1 } else { 0 }
        $failures = if ($Mode -eq 'failed-report') { 1 } else { 0 }
        # Surefire 3.5.3 uses '-suffix' in filenames, but '(suffix)' in suite identity.
        $suiteSuffix = if ($suffix) { '(' + $suffix.Substring(1) + ')' } else { '' }
        $body = "<testsuite name='example.$name$suiteSuffix' tests='$tests' skipped='$skipped' failures='$failures' errors='0'/>"
        if ($Mode -eq 'malformed') { $body = '<bad' }
        [IO.File]::WriteAllText((Join-Path $reportRoot "TEST-example.$name$suffix.xml"), $body)
    }
}
function global:python {
    Write-Output ('DOUBLE_PYTHON ' + ($args -join ' '))
    $global:LASTEXITCODE = 0
    if (($args -join ' ') -match 'gxp_audit_coverage_gate.py' -and $Mode -eq 'coverage-fail') { $global:LASTEXITCODE = 23 }
    if (($args -join ' ') -match 'pytest' -and $Mode -eq 'static-fail') { $global:LASTEXITCODE = 19 }
}
$params = @{}
foreach ($switch in $Switches) { $params[$switch] = $true }
& $Wrapper @params
exit $LASTEXITCODE
''', encoding='utf-8')
    command = [SHELL, '-NoProfile', '-File', str(harness), '-Mode', mode, '-Wrapper', str(wrapper)]
    # Invoke switches from a command expression because -File array parsing differs across PS versions.
    if switches:
        quote = lambda s: "'" + str(s).replace("'", "''") + "'"
        command = [SHELL, '-NoProfile', '-Command',
                   f'& {quote(harness)} -Mode {quote(mode)} -Wrapper {quote(wrapper)} -Switches @(' +
                   ','.join(quote(s) for s in switches) + '); exit $LASTEXITCODE']
    return subprocess.run(command, text=True, capture_output=True, encoding='utf-8', timeout=30)


@pytest.mark.parametrize('mode', ['missing-source', 'no-reports', 'stale', 'missing-report', 'zero',
                                  'all-skipped', 'partial-skipped', 'failed-report', 'malformed'])
def test_rejects_unproven_execution(tmp_path, mode):
    result = run_gate(tmp_path, mode)
    assert result.returncode != 0, result.stdout + result.stderr
    assert 'gate passed' not in result.stdout.lower()
    if mode == 'missing-source':
        assert 'DOUBLE_MAVEN' not in result.stdout
        assert 'Missing or ambiguous target test file' in result.stderr
    elif mode in ('no-reports', 'stale', 'missing-report'):
        assert 'Missing current-run target Surefire report' in result.stderr
    elif mode in ('zero', 'all-skipped'):
        assert 'Target has zero executed tests' in result.stderr
    elif mode == 'partial-skipped':
        assert result.returncode == 2
        assert 'INCOMPLETE' in result.stdout
    elif mode == 'failed-report':
        assert 'Target test failures/errors' in result.stderr


@pytest.mark.parametrize('mode,code', [('maven-fail', 17), ('static-fail', 19), ('coverage-fail', 23)])
def test_propagates_child_exit(tmp_path, mode, code):
    result = run_gate(tmp_path, mode)
    assert result.returncode == code, result.stdout + result.stderr


@pytest.mark.parametrize('switches', [('SkipMaven',), ('SkipPolicyStatic',), ('SkipMaven', 'SkipPolicyStatic')])
def test_skipped_stages_are_incomplete(tmp_path, switches):
    result = run_gate(tmp_path, switches=switches)
    assert result.returncode == 2, result.stdout + result.stderr
    assert 'INCOMPLETE' in result.stdout
    assert 'compliance gate passed' not in result.stdout


def test_executes_current_targets_and_scanner_without_full_compliance_claim(tmp_path):
    result = run_gate(tmp_path)
    assert result.returncode == 0, result.stdout + result.stderr
    assert '-pl yudao-module-system -am' in result.stdout
    assert 'GxpAuditNewBehaviorTest' in result.stdout
    assert 'GxpAuditTrail' not in result.stdout
    assert 'gxp_audit_coverage_gate.py' in result.stdout
    assert 'pytest' in result.stdout
    assert 'software gate passed' in result.stdout
    assert 'not a full compliance certification' in result.stdout


def test_fixture_rejects_excessive_base_before_writing(tmp_path):
    excessive = tmp_path / ('x' * 120)
    with pytest.raises(AssertionError, match='Wrapper fixture path budget exceeded'):
        run_gate(excessive)
    assert not excessive.exists()
