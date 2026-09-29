"""Exercise the real launcher functions without running services, Maven, or SQL."""

import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

import pytest


SCRIPT = Path(__file__).resolve().parents[1] / "deploy/restart-int-ruoyi-local.ps1"
POWERSHELL = shutil.which("powershell.exe") or shutil.which("pwsh")
MAIN_CLASS = "org.springframework.boot.loader.launch.JarLauncher"
START_CLASS = "cn.iocoder.yudao.server.YudaoServerApplication"
CLASS_BYTES = bytes.fromhex("cafebabe0000003d")  # Structural fixture, never executed.


def write_jar(path, *, defect=None):
    manifest = (
        "Manifest-Version: 1.0\r\n"
        f"Main-Class: {MAIN_CLASS}\r\n"
        f"Start-Class: {START_CLASS}\r\n"
        "Spring-Boot-Classes: BOOT-INF/classes/\r\n"
        "Spring-Boot-Lib: BOOT-INF/lib/\r\n\r\n"
    )
    if defect == "no_start_manifest":
        manifest = manifest.replace(f"Start-Class: {START_CLASS}\r\n", "")
    if defect == "folded_manifest":
        manifest = manifest.replace("YudaoServerApplication", "YudaoServer\r\n Application")
    nested = io.BytesIO()
    with zipfile.ZipFile(nested, "w") as archive:
        archive.writestr("fixture/Dependency.class", CLASS_BYTES)
    with zipfile.ZipFile(path, "w") as archive:
        if defect != "no_manifest":
            archive.writestr("META-INF/MANIFEST.MF", manifest)
        if defect != "no_loader":
            archive.writestr(MAIN_CLASS.replace(".", "/") + ".class", CLASS_BYTES)
        if defect != "no_application":
            archive.writestr(
                "BOOT-INF/classes/" + START_CLASS.replace(".", "/") + ".class",
                b"not a class" if defect == "bad_class" else CLASS_BYTES,
            )
        if defect != "no_libs":
            archive.writestr(
                "BOOT-INF/lib/dependency.jar",
                b"not a jar" if defect == "bad_lib" else nested.getvalue(),
                compress_type=zipfile.ZIP_DEFLATED if defect == "compressed_lib" else zipfile.ZIP_STORED,
            )
    return hashlib.sha256(path.read_bytes()).hexdigest()


# Load only reviewed function definitions by AST; never dot-source the launcher.
# All process, schema, container and Maven actions are replaced before invocation.
# The generated secret-bearing child command is inspected in memory, never printed.
HARNESS = r"""
$ErrorActionPreference = 'Stop'
$inputData = $env:LAUNCHER_TEST_INPUT | ConvertFrom-Json
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($inputData.script, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -ne 0) { throw 'Launcher parser failed' }
$functions = @('Start-Backend', 'Get-BackendJarSha256', 'Assert-BackendExecutableJar', 'Assert-BackendLaunchConfiguration')
foreach ($statement in $ast.EndBlock.Statements) {
    if ($statement -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $statement.Name -in $functions) {
        . ([scriptblock]::Create($statement.Extent.Text))
    }
}
$script:events = [System.Collections.Generic.List[string]]::new()
$script:launch = $null
$script:mavenArgs = @()
function Fail([string]$Message) { throw $Message }
function Require-Command([string]$Name) { $script:events.Add("require:$Name") }
function Ensure-RequiredLocalMySqlSchema { $script:events.Add('schema-stub') }
function Assert-LocalShowroomFileConfigProtected { $script:events.Add('showroom-stub') }
function Assert-LocalShowroomMediaBucketConsistency { $script:events.Add('minio-stub') }
function Assert-LocalDockerRuntimePortRoute { param($Name, $Port) }
function Stop-MatchingProcesses { param($Label, $CommandFragment) $script:events.Add('stop-matching') }
function Stop-Port { param($Port) $script:events.Add('stop-port') }
function mvn {
    $script:events.Add('build')
    $script:mavenArgs = @($args)
    $global:LASTEXITCODE = [int]$inputData.buildExit
    if ($global:LASTEXITCODE -eq 0 -and -not $inputData.omitBuiltJar) {
        Microsoft.PowerShell.Management\Copy-Item -LiteralPath $inputData.fixture -Destination (Join-Path $BackendDir 'target/yudao-server-exec.jar')
    }
}
function Copy-Item {
    param($LiteralPath, $Destination, [switch]$Force)
    $script:events.Add('copy')
    if ($inputData.copyFailure) { throw 'Simulated fixture copy failure' }
    Microsoft.PowerShell.Management\Copy-Item -LiteralPath $LiteralPath -Destination $Destination -Force:$Force
    if ($inputData.corruptCopy) { [IO.File]::AppendAllText($Destination, 'fixture corruption') }
}
function Start-Process {
    param($FilePath, $ArgumentList, $WorkingDirectory, $RedirectStandardOutput, $RedirectStandardError, $WindowStyle)
    $script:events.Add('start')
    $encodedIndex = [array]::IndexOf($ArgumentList, '-EncodedCommand')
    $child = [Text.Encoding]::Unicode.GetString([Convert]::FromBase64String($ArgumentList[$encodedIndex + 1]))
    $childTokens = $null
    $childErrors = $null
    $childAst = [System.Management.Automation.Language.Parser]::ParseInput($child, [ref]$childTokens, [ref]$childErrors)
    $jarArgument = $childAst.FindAll({param($node) $node -is [System.Management.Automation.Language.StringConstantExpressionAst] -and $node.Value.EndsWith('.jar')}, $true)
    $script:launch = @{
        file = $FilePath
        windowStyle = [string]$WindowStyle
        parserErrors = $childErrors.Count
        jar = @($jarArgument | ForEach-Object { $_.Value })
        javaArray = $child.Contains('& java @backendArgs')
        port = $child.Contains('--server.port=48081')
        configPresent = $child.Contains("`$env:DCC_ONLYOFFICE_BASE_URL = 'http://fixture-onlyoffice'") -and
            $child.Contains("`$env:DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL = 'http://fixture-files'") -and
            $child.Contains("`$env:DCC_SIGNATURE_EVIDENCE_HMAC_SECRET = 'fixture-secret'") -and
            $child.Contains("`$env:DCC_SIGNATURE_EVIDENCE_KEY_VERSION = 'fixture-v1'")
    }
}
$RepoRoot = $inputData.workspace
$BackendDir = Join-Path $RepoRoot 'yudao-server'
$RuntimeDir = Join-Path $RepoRoot 'runtime'
$RuntimeControlStateDir = Join-Path $RepoRoot 'control'
$BackendPort = 48081
$LocalDockerRuntimeHost = '127.0.0.2'
$OnlyOfficeBaseUrl = 'http://fixture-onlyoffice'
$OnlyOfficePublicFileBaseUrl = 'http://fixture-files'
$DccSignatureEvidenceHmacSecret = 'fixture-secret'
$DccSignatureEvidenceKeyVersion = 'fixture-v1'
if ($inputData.missingConfig) { Set-Variable -Name $inputData.missingConfig -Value ' ' }
$PrebuiltBackendJar = [string]$inputData.jar
$PrebuiltBackendSha256 = [string]$inputData.sha
$errorMessage = $null
try { Start-Backend } catch { $errorMessage = $_.Exception.Message }
@{
    error = $errorMessage
    events = @($script:events)
    launch = $script:launch
    mavenArgs = $script:mavenArgs
    parameters = @($ast.ParamBlock.Parameters | ForEach-Object { $_.Name.VariablePath.UserPath })
} | ConvertTo-Json -Depth 5 -Compress
"""


def invoke_launcher(tmp_path, *, jar="valid", sha="valid", defect=None, **options):
    assert POWERSHELL, "PowerShell is required for launcher behavior tests"
    fixture = tmp_path / "isolated build [verified].jar"
    digest = write_jar(fixture, defect=defect)
    workspace = tmp_path / "selected runtime workspace"
    (workspace / "yudao-server/target").mkdir(parents=True)
    (workspace / "yudao-server/pom.xml").write_text("<project/>", encoding="utf-8")
    if jar == "valid":
        jar = str(fixture)
    if sha == "valid":
        sha = digest
    payload = dict(script=str(SCRIPT), workspace=str(workspace), fixture=str(fixture), jar=jar, sha=sha, buildExit=0)
    payload.update(options)
    result = subprocess.run(
        [POWERSHELL, "-NoProfile", "-NonInteractive", "-Command", HARNESS],
        env={**os.environ, "LAUNCHER_TEST_INPUT": json.dumps(payload)},
        capture_output=True, text=True, timeout=45, encoding="utf-8",
    )
    assert result.returncode == 0, "PowerShell harness failed (raw child output withheld)"
    return json.loads(result.stdout.strip().splitlines()[-1]), fixture, workspace


def assert_not_stopped(result):
    assert "stop-matching" not in result["events"], result["events"]
    assert "stop-port" not in result["events"], result["events"]
    assert "start" not in result["events"], result["events"]


@pytest.mark.parametrize("jar,sha,reason", [
    ("missing.jar", "valid", "Missing executable backend jar"),
    ("valid", "", "together"),
    ("", "valid", "together"),
    ("valid", "not-a-sha256", "SHA256"),
    ("valid", "0" * 64, "SHA256 mismatch"),
    (" ", " ", "Missing executable backend jar"),
])
def test_invalid_prebuilt_rejected_before_stop_build_or_schema(tmp_path, jar, sha, reason):
    result, _, _ = invoke_launcher(tmp_path, jar=jar, sha=sha)
    assert result["error"] and reason in result["error"]
    assert_not_stopped(result)
    assert "build" not in result["events"]
    assert "schema-stub" not in result["events"]


@pytest.mark.parametrize("defect", [
    "no_manifest", "no_start_manifest", "no_loader", "no_application",
    "bad_class", "no_libs", "bad_lib", "compressed_lib",
])
def test_non_executable_artifact_rejected_before_stop(tmp_path, defect):
    result, _, _ = invoke_launcher(tmp_path, defect=defect)
    assert result["error"] and "executable backend jar" in result["error"].lower(), result["error"]
    assert_not_stopped(result)
    assert "build" not in result["events"]
    assert "schema-stub" not in result["events"]


@pytest.mark.parametrize("defect", [None, "folded_manifest"])
def test_prebuilt_is_copied_verified_and_launched_without_maven(tmp_path, defect):
    result, fixture, workspace = invoke_launcher(tmp_path, defect=defect)
    assert result["error"] is None, result["error"]
    assert {"PrebuiltBackendJar", "PrebuiltBackendSha256"} <= set(result["parameters"])
    assert "build" not in result["events"]
    assert "require:mvn" not in result["events"]
    runtime_jars = list((workspace / "runtime").glob("*.jar"))
    assert len(runtime_jars) == 1
    assert runtime_jars[0].read_bytes() == fixture.read_bytes()
    assert Path(result["launch"]["jar"][0]) == runtime_jars[0]
    assert result["events"].index("copy") < result["events"].index("stop-matching")
    assert result["events"].index("stop-port") < result["events"].index("start")
    assert result["launch"]["configPresent"]
    assert result["launch"]["javaArray"] and result["launch"]["port"]
    assert result["launch"]["parserErrors"] == 0
    assert result["launch"]["windowStyle"] == "Hidden"


def test_normal_build_finishes_and_copies_before_stop(tmp_path):
    result, fixture, workspace = invoke_launcher(tmp_path, jar="", sha="")
    assert result["error"] is None, result["error"]
    assert result["mavenArgs"] == ["-pl", "yudao-server", "-am", "-DskipTests", "package"]
    assert result["events"].index("build") < result["events"].index("copy") < result["events"].index("stop-matching")
    assert next((workspace / "runtime").glob("*.jar")).read_bytes() == fixture.read_bytes()


@pytest.mark.parametrize("options", [{"buildExit": 1}, {"omitBuiltJar": True}, {"defect": "no_manifest"}])
def test_normal_build_failure_or_invalid_output_keeps_old_runtime(tmp_path, options):
    result, _, _ = invoke_launcher(tmp_path, jar="", sha="", **options)
    assert result["error"]
    assert "build" in result["events"]
    assert_not_stopped(result)


@pytest.mark.parametrize("options", [{"copyFailure": True}, {"corruptCopy": True}])
def test_failed_or_corrupted_runtime_copy_keeps_old_runtime(tmp_path, options):
    result, _, _ = invoke_launcher(tmp_path, **options)
    assert result["error"]
    assert "copy" in result["events"]
    assert_not_stopped(result)


@pytest.mark.parametrize("name", [
    "OnlyOfficeBaseUrl", "OnlyOfficePublicFileBaseUrl",
    "DccSignatureEvidenceHmacSecret", "DccSignatureEvidenceKeyVersion",
])
def test_missing_required_launch_configuration_keeps_old_runtime(tmp_path, name):
    result, _, _ = invoke_launcher(tmp_path, missingConfig=name)
    assert result["error"] and name in result["error"]
    assert "fixture-secret" not in result["error"]
    assert_not_stopped(result)
