from pathlib import Path
import base64
import subprocess


def _script_text() -> str:
    script = Path(__file__).resolve().parents[1] / "deploy" / "manage-int-ruoyi-remote-root-disk.ps1"
    return script.read_text(encoding="utf-8")


def _raw_script_text() -> str:
    script = Path(__file__).resolve().parents[1] / "deploy" / "manage-int-ruoyi-remote-root-disk.ps1"
    return script.read_bytes().decode("utf-8-sig")


def _function(source: str, name: str, next_name: str) -> str:
    return source[source.index(f"function {name} {{"):source.index(f"function {next_name} {{")]


def _powershell(command: str) -> subprocess.CompletedProcess[str]:
    encoded = base64.b64encode(command.encode("utf-16le")).decode("ascii")
    return subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded],
                          capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=20,
                          check=False)


def test_status_remote_script_uses_lf_stdin_and_one_bash_process() -> None:
    source = _raw_script_text()
    ssh_function = _function(source, "Invoke-SshCapture", "Convert-MetricLines")
    status_function = _function(source, "Get-RemoteStatus", "Invoke-RemoteCleanup")
    command = "\n".join((
        "$ErrorActionPreference='Stop'",
        "$ExpectedHosts=@{test='172.30.30.58'}; $TargetEnvironment='test'; $ServerHost='172.30.30.58'; $ServerUser='root'",
        "function Assert-TargetBoundary {}",
        "function Remove-SshNoise {param([string]$Text) return $Text}",
        "function Convert-MetricLines {param([string]$Text) return $Text}",
        "function Invoke-ProcessCapture {param([string]$FilePath,[string[]]$ArgumentList,[string]$StandardInputText) "
        "if($FilePath -ne 'ssh' -or $ArgumentList -contains '-n' -or "
        "$ArgumentList[-2] -ne 'bash' -or $ArgumentList[-1] -ne '-s') {throw 'SSH_ARGUMENT_CONTRACT_INVALID'}; "
        "if($null -eq $StandardInputText -or $StandardInputText.Contains([char]13)) {throw 'REMOTE_SCRIPT_CRLF'}; "
        "return @{ExitCode=0;StdOut='LF_OK';StdErr=''}}",
        ssh_function,
        status_function,
        "if ((Get-RemoteStatus) -ne 'LF_OK') {throw 'STATUS_CAPTURE_FAILED'}",
        "Write-Output 'STATUS_LF_STDIN_PASS'",
    ))
    result = _powershell(command)
    assert result.returncode == 0, result.stderr
    assert "STATUS_LF_STDIN_PASS" in result.stdout


def test_process_capture_preserves_lf_script_bytes_through_real_stdin() -> None:
    source = _raw_script_text()
    capture_function = _function(source, "Invoke-ProcessCapture", "Invoke-SshCapture")
    child = "$inputText=[Console]::In.ReadToEnd(); " \
            "if($inputText.Contains([char]13)) {exit 9}; " \
            "if($inputText -cne \"set -eu`necho ok`n\") {exit 10}; Write-Output 'LF_OK'"
    child_encoded = base64.b64encode(child.encode("utf-16le")).decode("ascii")
    command = "\n".join((
        "$ErrorActionPreference='Stop'",
        capture_function,
        "$result=Invoke-ProcessCapture -FilePath 'powershell.exe' "
        f"-ArgumentList @('-NoProfile','-NonInteractive','-EncodedCommand','{child_encoded}') "
        "-StandardInputText \"set -eu`necho ok`n\"",
        "if($result.ExitCode -ne 0 -or $result.StdOut.Trim() -cne 'LF_OK') {throw 'STDIN_BYTE_CONTRACT_FAILED'}",
        "Write-Output 'PROCESS_STDIN_PASS'",
    ))
    result = _powershell(command)
    assert result.returncode == 0, result.stderr
    assert "PROCESS_STDIN_PASS" in result.stdout


def test_remote_root_disk_script_has_fixed_environment_hosts() -> None:
    text = _script_text()

    assert "test = '172.30.30.58'" in text
    assert "prod = '172.30.30.57'" in text
    assert "backup = '172.30.30.59'" in text
    assert "targetEnvironment=test/prod/backup" in text
    assert "target proof failed" in text


def test_remote_root_disk_cleanup_is_limited_to_temp_paths() -> None:
    text = _script_text()

    assert "$BackupTmpPath = '/opt/intruoyi/ops/backup/tmp'" in text
    assert "$SystemTmpPath = '/tmp'" in text
    assert "find \"$BACKUP_TMP\" -xdev -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +" in text
    assert "find \"$SYSTEM_TMP\" -xdev -mindepth 1 -maxdepth 1" in text
    assert "! -name 'systemd-private-*'" in text
    assert "ReleasePackage" not in text
    assert "/mnt/nas" not in text
    assert "fstab" not in text


def test_remote_root_disk_protects_prod_and_backup_cleanup() -> None:
    text = _script_text()

    assert "$Mode -eq 'cleanup'" in text
    assert "$TargetEnvironment -eq 'prod' -or $TargetEnvironment -eq 'backup'" in text
    assert "$ProdConfirmText -ne 'PROD'" in text
    assert "protected remote root cleanup requires ProdConfirmText=PROD" in text


def test_remote_root_disk_status_requires_runtime_ip_proof() -> None:
    text = _script_text()

    assert "hostname -I" in text
    assert "grep -qw \"$EXPECTED_IP\"" in text
    assert "status output does not prove $TargetEnvironment server $expectedHost" in text
    assert "status output mountPoint must be /" in text
