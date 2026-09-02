param(
    [Parameter(Mandatory = $true)]
    [string]$Container,

    [Parameter(Mandatory = $true)]
    [string]$Migration
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-ContainerMySql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql,
        [string]$Schema
    )

    $shellCommand = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names'
    if ($Schema) {
        $shellCommand += ' "$1"'
    }

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = 'docker'
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardInputEncoding = [System.Text.UTF8Encoding]::new($false)
    $startInfo.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $startInfo.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)
    @('exec', '-i', $Container, 'sh', '-c', $shellCommand, 'mysql-client') | ForEach-Object {
        [void]$startInfo.ArgumentList.Add($_)
    }
    if ($Schema) {
        [void]$startInfo.ArgumentList.Add($Schema)
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw 'Failed to start local Docker MySQL client.'
    }
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $process.WaitForExit()

    [pscustomobject]@{
        ExitCode = $process.ExitCode
        Stdout = $stdoutTask.GetAwaiter().GetResult().Trim()
        Stderr = $stderrTask.GetAwaiter().GetResult().Trim()
    }
}

function Invoke-SqlSuccess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql,
        [string]$Schema,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $result = Invoke-ContainerMySql -Sql $Sql -Schema $Schema
    if ($result.ExitCode -ne 0) {
        throw "$Label failed with exit $($result.ExitCode): $($result.Stderr)"
    }
    return $result.Stdout
}

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Actual,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    if ($Expected -ne $Actual) {
        throw "$Label mismatch: expected '$Expected', actual '$Actual'"
    }
    Write-Output "PASS: $Label"
}

$migrationPath = (Resolve-Path -LiteralPath $Migration).Path
$migrationSql = [System.IO.File]::ReadAllText($migrationPath, [System.Text.Encoding]::UTF8)
$schema = 'codex_regcert_notify_t04b_' + [Guid]::NewGuid().ToString('N').Substring(0, 16)

try {
    $running = [string](& docker inspect --format '{{.State.Running}}' $Container 2>$null)
    if ($LASTEXITCODE -ne 0 -or $running.Trim() -ne 'true') {
        throw "Approved MySQL container '$Container' is not running."
    }

    [void](Invoke-SqlSuccess -Label 'create isolated schema' -Sql "CREATE DATABASE ``$schema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    [void](Invoke-SqlSuccess -Schema $schema -Label 'create notify template fixture' -Sql @'
CREATE TABLE system_notify_template (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  code varchar(255) NOT NULL,
  type int NOT NULL,
  nickname varchar(255) NOT NULL,
  content text NOT NULL,
  params varchar(1000) NOT NULL,
  status tinyint NOT NULL,
  remark varchar(255) NULL,
  creator varchar(64) NULL,
  create_time datetime NULL,
  updater varchar(64) NULL,
  update_time datetime NULL,
  deleted bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
'@)

    [void](Invoke-SqlSuccess -Schema $schema -Sql $migrationSql -Label 'first migration apply')
    $firstState = Invoke-SqlSuccess -Schema $schema -Label 'first migration template state' -Sql @'
SELECT CONCAT(COUNT(*), '|', MAX(`params`), '|', MAX(`status`), '|', SUM(CHAR_LENGTH(`content`) > 0))
  FROM system_notify_template
 WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
   AND deleted = b'0';
'@
    Assert-Equal -Expected '1|["eventTitle","productName","certificateNo","effectiveDate","expiryDate"]|0|1' `
        -Actual $firstState -Label 'first migration inserts readable template exactly once'

    [void](Invoke-SqlSuccess -Schema $schema -Sql $migrationSql -Label 'repeat migration apply')
    $repeatCount = Invoke-SqlSuccess -Schema $schema -Label 'repeat migration active template count' -Sql @'
SELECT COUNT(*)
  FROM system_notify_template
 WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
   AND deleted = b'0';
'@
    Assert-Equal -Expected '1' -Actual $repeatCount -Label 'repeat migration remains idempotent'

    $procedureCount = Invoke-SqlSuccess -Schema $schema -Label 'migration procedure cleanup' -Sql @'
SELECT COUNT(*)
  FROM information_schema.ROUTINES
 WHERE ROUTINE_SCHEMA = DATABASE()
   AND ROUTINE_TYPE = 'PROCEDURE';
'@
    Assert-Equal -Expected '0' -Actual $procedureCount -Label 'migration procedure is removed'
}
finally {
    if ($schema -notmatch '^codex_regcert_notify_t04b_[a-f0-9]{16}$') {
        throw "Refusing to drop non-task schema '$schema'."
    }
    [void](Invoke-SqlSuccess -Label 'drop isolated schema' -Sql "DROP DATABASE IF EXISTS ``$schema``;")
    Write-Output 'PASS: isolated schema removed'
}
