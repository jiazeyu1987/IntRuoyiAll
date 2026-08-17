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
        throw 'Failed to start Docker MySQL client'
    }
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        Stdout = $stdout.Trim()
        Stderr = $stderr.Trim()
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

function Assert-SqlFails {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql,
        [string]$Schema,
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [string]$ExpectedMessage
    )
    $result = Invoke-ContainerMySql -Sql $Sql -Schema $Schema
    if ($result.ExitCode -eq 0) {
        throw "$Label unexpectedly succeeded"
    }
    if ($ExpectedMessage -and -not $result.Stderr.Contains($ExpectedMessage)) {
        throw "$Label failed for an unexpected reason: $($result.Stderr)"
    }
    Write-Output "PASS: $Label rejected"
}

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]$Expected,
        [Parameter(Mandatory = $true)]$Actual,
        [Parameter(Mandatory = $true)][string]$Label
    )
    if ($Expected -ne $Actual) {
        throw "$Label mismatch: expected '$Expected', actual '$Actual'"
    }
    Write-Output "PASS: $Label"
}

function New-OwnedSchema {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Schema,
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[string]]$Registry
    )

    [void](Invoke-SqlSuccess -Label "create isolated schema $Schema" `
        -Sql "CREATE DATABASE ``$Schema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    $Registry.Add($Schema)
}

function Remove-OwnedSchemas {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[string]]$Schemas
    )

    $cleanupErrors = [System.Collections.Generic.List[string]]::new()
    foreach ($ownedSchema in $Schemas) {
        if ($ownedSchema -notmatch '^codex_regcert_t04a_[a-f0-9]{16}(_partial|_incompatible)?$') {
            $cleanupErrors.Add("Refusing to drop non-task schema '$ownedSchema'")
            continue
        }
        try {
            [void](Invoke-SqlSuccess -Label "drop isolated schema $ownedSchema" `
                -Sql "DROP DATABASE IF EXISTS ``$ownedSchema``;")
        }
        catch {
            $cleanupErrors.Add($_.Exception.Message)
        }
    }
    if ($cleanupErrors.Count -gt 0) {
        throw "Cleanup failed after all owned schemas were attempted: $($cleanupErrors -join ' | ')"
    }
}

$migrationPath = (Resolve-Path -LiteralPath $Migration).Path
$migrationSql = [System.IO.File]::ReadAllText($migrationPath, [System.Text.Encoding]::UTF8)
$suffix = [Guid]::NewGuid().ToString('N').Substring(0, 16)
$schema = "codex_regcert_t04a_$suffix"
$partialSchema = "${schema}_partial"
$incompatibleSchema = "${schema}_incompatible"
$createdSchemas = [System.Collections.Generic.List[string]]::new()

try {
    $running = (& docker inspect --format '{{.State.Running}}' $Container 2>$null).Trim()
    if ($LASTEXITCODE -ne 0 -or $running -ne 'true') {
        throw "Approved MySQL container '$Container' is not running"
    }

    New-OwnedSchema -Schema $schema -Registry $createdSchemas
    New-OwnedSchema -Schema $partialSchema -Registry $createdSchemas
    New-OwnedSchema -Schema $incompatibleSchema -Registry $createdSchemas

    [void](Invoke-SqlSuccess -Schema $schema -Sql $migrationSql -Label 'first migration apply')
    Write-Output 'PASS: first migration apply'
    [void](Invoke-SqlSuccess -Schema $schema -Sql $migrationSql -Label 'repeat migration apply')
    Write-Output 'PASS: repeat migration apply'

    [void](Invoke-SqlSuccess -Schema $partialSchema -Label 'create half migration fixture' -Sql @'
CREATE TABLE `dcc_registration_certificate` (`id` bigint NOT NULL PRIMARY KEY);
'@)
    Assert-SqlFails -Schema $partialSchema -Sql $migrationSql -Label 'half migration' `
        -ExpectedMessage 'DCC registration certificate core partial schema detected'
    $partialTableCount = Invoke-SqlSuccess -Schema $partialSchema -Label 'half migration residual table count' -Sql @"
SELECT COUNT(*) FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = '$partialSchema'
   AND TABLE_NAME LIKE 'dcc_registration_certificate%';
"@
    Assert-Equal -Expected '1' -Actual $partialTableCount -Label 'half migration made no additional tables'

    $tableCount = Invoke-SqlSuccess -Schema $schema -Label 'core table count' -Sql @"
SELECT COUNT(*) FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = '$schema'
   AND TABLE_NAME LIKE 'dcc_registration_certificate%';
"@
    Assert-Equal -Expected '6' -Actual $tableCount -Label 'exact six core tables'

    $generatedColumns = Invoke-SqlSuccess -Schema $schema -Label 'generated column metadata' -Sql @"
SELECT CONCAT(TABLE_NAME, '|', COLUMN_NAME, '|', EXTRA, '|', LOWER(GENERATION_EXPRESSION))
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = '$schema'
   AND COLUMN_NAME IN ('current_unique_flag', 'pending_unique_flag',
                       'entrusted_enterprise_count', 'bound_file_unique_flag')
 ORDER BY TABLE_NAME, COLUMN_NAME;
"@
    foreach ($name in @('current_unique_flag', 'pending_unique_flag', 'entrusted_enterprise_count', 'bound_file_unique_flag')) {
        if ($generatedColumns -notmatch [regex]::Escape("|$name|STORED GENERATED|")) {
            throw "generated column contract missing for $name"
        }
    }
    if ($generatedColumns -notmatch 'entrusted_enterprise_count\|STORED GENERATED\|.*json_length') {
        throw 'entrusted enterprise count is not generated with JSON_LENGTH'
    }
    Write-Output 'PASS: generated columns and JSON_LENGTH metadata'

    $uniqueIndexes = Invoke-SqlSuccess -Schema $schema -Label 'unique index metadata' -Sql @"
SELECT CONCAT(TABLE_NAME, '|', INDEX_NAME, '|', MAX(NON_UNIQUE), '|',
              GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','), '|',
              SUM(SUB_PART IS NOT NULL))
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = '$schema'
   AND INDEX_NAME IN ('uk_dcc_reg_cert_version_no', 'uk_dcc_reg_cert_current',
                      'uk_dcc_reg_cert_pending', 'uk_dcc_reg_cert_snapshot_revision',
                      'uk_dcc_reg_cert_entrusted', 'uk_dcc_reg_cert_bound_file',
                      'uk_dcc_reg_cert_audit_event')
 GROUP BY TABLE_NAME, INDEX_NAME
 ORDER BY TABLE_NAME, INDEX_NAME;
"@
    $expectedIndexes = @(
        'dcc_registration_certificate_audit|uk_dcc_reg_cert_audit_event|0|tenant_id,event_key|0',
        'dcc_registration_certificate_file|uk_dcc_reg_cert_bound_file|0|tenant_id,bound_file_unique_flag|0',
        'dcc_registration_certificate_snapshot|uk_dcc_reg_cert_snapshot_revision|0|tenant_id,version_id,revision_no|0',
        'dcc_registration_certificate_snapshot_entrusted|uk_dcc_reg_cert_entrusted|0|tenant_id,snapshot_id,enterprise_id|0',
        'dcc_registration_certificate_version|uk_dcc_reg_cert_current|0|tenant_id,certificate_id,current_unique_flag|0',
        'dcc_registration_certificate_version|uk_dcc_reg_cert_pending|0|tenant_id,certificate_id,pending_unique_flag|0',
        'dcc_registration_certificate_version|uk_dcc_reg_cert_version_no|0|tenant_id,certificate_id,version_no|0'
    )
    Assert-Equal -Expected ($expectedIndexes -join "`n") -Actual $uniqueIndexes -Label 'exact unique index order and full columns'

    $checkContracts = Invoke-SqlSuccess -Schema $schema -Label 'CHECK metadata' -Sql @"
SELECT CONCAT(tc.TABLE_NAME, '|', tc.CONSTRAINT_NAME, '|', LOWER(cc.CHECK_CLAUSE))
  FROM information_schema.TABLE_CONSTRAINTS tc
  JOIN information_schema.CHECK_CONSTRAINTS cc
    ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
   AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
 WHERE tc.CONSTRAINT_SCHEMA = '$schema'
   AND tc.CONSTRAINT_NAME IN ('chk_dcc_reg_cert_master_status', 'chk_dcc_reg_cert_version_type',
                              'chk_dcc_reg_cert_version_status', 'chk_dcc_reg_cert_snapshot_json_array',
                              'chk_dcc_reg_cert_production_relation', 'chk_dcc_reg_cert_file_owner_type',
                              'chk_dcc_reg_cert_file_kind', 'chk_dcc_reg_cert_audit_event_key')
 ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;
"@
    foreach ($name in @('chk_dcc_reg_cert_master_status', 'chk_dcc_reg_cert_version_type',
            'chk_dcc_reg_cert_version_status', 'chk_dcc_reg_cert_snapshot_json_array',
            'chk_dcc_reg_cert_production_relation', 'chk_dcc_reg_cert_file_owner_type',
            'chk_dcc_reg_cert_file_kind', 'chk_dcc_reg_cert_audit_event_key')) {
        if ($checkContracts -notmatch [regex]::Escape("|$name|")) {
            throw "CHECK contract missing for $name"
        }
    }
    Write-Output 'PASS: named CHECK constraints'

    [void](Invoke-SqlSuccess -Schema $schema -Label 'seed uniqueness fixtures' -Sql @'
INSERT INTO dcc_registration_certificate
  (id, owner_company_id, product_master_id, status, tenant_id)
VALUES (1, 10, 20, 'ACTIVE', 1), (2, 10, 21, 'DRAFT', 1), (3, 10, 22, 'VOIDED', 1);
INSERT INTO dcc_registration_certificate_version
  (id, certificate_id, version_no, version_type, status, tenant_id)
VALUES
  (11, 1, 1, 'INITIAL_CERTIFICATE', 'CURRENT', 1),
  (12, 1, 2, 'RENEWAL_CERTIFICATE', 'PENDING_EFFECTIVE', 1),
  (13, 1, 3, 'RENEWAL_CERTIFICATE', 'OLD', 1),
  (14, 1, 4, 'RENEWAL_CERTIFICATE', 'OLD', 1),
  (21, 2, 1, 'INITIAL_CERTIFICATE', 'DRAFT', 1);
'@)
    Assert-SqlFails -Schema $schema -Label 'duplicate current version' -Sql @'
INSERT INTO dcc_registration_certificate_version
  (certificate_id, version_no, version_type, status, tenant_id)
VALUES (1, 5, 'RENEWAL_CERTIFICATE', 'CURRENT', 1);
'@
    Assert-SqlFails -Schema $schema -Label 'duplicate pending version' -Sql @'
INSERT INTO dcc_registration_certificate_version
  (certificate_id, version_no, version_type, status, tenant_id)
VALUES (1, 6, 'RENEWAL_CERTIFICATE', 'PENDING_EFFECTIVE', 1);
'@
    Assert-SqlFails -Schema $schema -Label 'formal version content overwrite' -Sql @'
UPDATE dcc_registration_certificate_version
   SET certificate_no = 'OVERWRITE'
 WHERE id = 11;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed formal version lifecycle update' -Sql @'
UPDATE dcc_registration_certificate_version
   SET status = 'OLD', voided_at = NOW(), voided_by = 99, void_reason = 'controlled lifecycle evidence'
 WHERE id = 11;
'@)
    Write-Output 'PASS: allowed formal version lifecycle update'
    Assert-SqlFails -Schema $schema -Label 'formal version DRAFT rollback overwrite bypass' -Sql @'
UPDATE dcc_registration_certificate_version SET status = 'DRAFT' WHERE id = 13;
UPDATE dcc_registration_certificate_version SET certificate_no = 'OVERWRITE' WHERE id = 13;
'@
    Assert-SqlFails -Schema $schema -Label 'OLD version return to CURRENT' -Sql @'
UPDATE dcc_registration_certificate_version SET status = 'CURRENT' WHERE id = 13;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed OLD version void transition' -Sql @'
UPDATE dcc_registration_certificate_version
   SET status = 'VOIDED', voided_at = NOW(), voided_by = 99, void_reason = 'void transition fixture'
 WHERE id = 14;
'@)
    Write-Output 'PASS: allowed OLD version void transition'
    Assert-SqlFails -Schema $schema -Label 'VOIDED version return to CURRENT' -Sql @'
UPDATE dcc_registration_certificate_version SET status = 'CURRENT' WHERE id = 14;
'@
    Assert-SqlFails -Schema $schema -Label 'formal master DRAFT rollback overwrite bypass' -Sql @'
UPDATE dcc_registration_certificate SET status = 'DRAFT' WHERE id = 1;
UPDATE dcc_registration_certificate SET owner_company_id = 999 WHERE id = 1;
'@
    Assert-SqlFails -Schema $schema -Label 'VOIDED master leaves terminal status' -Sql @'
UPDATE dcc_registration_certificate SET status = 'ACTIVE' WHERE id = 3;
'@
    Assert-SqlFails -Schema $schema -Label 'formal master fact overwrite' -Sql @'
UPDATE dcc_registration_certificate
   SET owner_company_id = 999
 WHERE id = 1;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed formal master pointer update' -Sql @'
UPDATE dcc_registration_certificate
   SET current_version_id = 11, current_snapshot_id = 101, row_version = row_version + 1
 WHERE id = 1;
'@)
    Write-Output 'PASS: allowed formal master pointer update'
    $draftFlags = Invoke-SqlSuccess -Schema $schema -Label 'draft uniqueness flags' -Sql @'
SELECT CONCAT(IFNULL(current_unique_flag, 'NULL'), '|', IFNULL(pending_unique_flag, 'NULL'))
  FROM dcc_registration_certificate_version WHERE id = 21;
'@
    Assert-Equal -Expected 'NULL|NULL' -Actual $draftFlags -Label 'DRAFT has no formal uniqueness flags'

    [void](Invoke-SqlSuccess -Schema $schema -Label 'valid production relation rows' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (id, version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES
  (101, 11, 1, 'Product A', 'Registrant A', b'0', b'1', JSON_ARRAY(), NOW(), 1),
  (102, 12, 1, 'Product A', 'Registrant A', b'1', b'0',
   JSON_ARRAY(JSON_OBJECT('enterpriseId', 30, 'enterpriseName', 'Factory A')), NOW(), 1);
'@)
    Assert-SqlFails -Schema $schema -Label 'both production modes false' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (13, 1, 'P', 'R', b'0', b'0', JSON_ARRAY(), NOW(), 1);
'@
    Assert-SqlFails -Schema $schema -Label 'entrusted production without authority' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (13, 2, 'P', 'R', b'1', b'0', JSON_ARRAY(), NOW(), 1);
'@
    Assert-SqlFails -Schema $schema -Label 'self-only production with residual authority' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (13, 3, 'P', 'R', b'0', b'1', JSON_ARRAY(JSON_OBJECT('enterpriseId', 30)), NOW(), 1);
'@
    Assert-SqlFails -Schema $schema -Label 'formal snapshot overwrite' -Sql @'
UPDATE dcc_registration_certificate_snapshot
   SET product_name = 'OVERWRITE'
 WHERE id = 101;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'entrusted projection fixture' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot_entrusted
  (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id)
VALUES (102, 30, 'Factory A', 1, 1);
'@)
    Assert-SqlFails -Schema $schema -Label 'entrusted projection overwrite' -Sql @'
UPDATE dcc_registration_certificate_snapshot_entrusted
   SET enterprise_name_snapshot = 'OVERWRITE'
 WHERE snapshot_id = 102 AND enterprise_id = 30;
'@

    [void](Invoke-SqlSuccess -Schema $schema -Label 'draft snapshot and projection fixture' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (id, version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (201, 21, 1, 'Draft Product', 'Draft Registrant', b'1', b'0',
        JSON_ARRAY(JSON_OBJECT('enterpriseId', 31, 'enterpriseName', 'Draft Factory')), NOW(), 1);
INSERT INTO dcc_registration_certificate_snapshot_entrusted
  (id, snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id)
VALUES (2001, 201, 31, 'Draft Factory', 1, 1);
'@)
    [void](Invoke-SqlSuccess -Schema $schema -Label 'draft snapshot update' -Sql @'
UPDATE dcc_registration_certificate_snapshot
   SET product_name = 'Edited Draft Product'
 WHERE id = 201;
'@)
    Write-Output 'PASS: draft snapshot update'
    [void](Invoke-SqlSuccess -Schema $schema -Label 'draft entrusted projection update' -Sql @'
UPDATE dcc_registration_certificate_snapshot_entrusted
   SET enterprise_name_snapshot = 'Edited Draft Factory'
 WHERE id = 2001;
'@)
    Write-Output 'PASS: draft entrusted projection update'
    Assert-SqlFails -Schema $schema -Label 'cross-version snapshot reattachment' -Sql @'
UPDATE dcc_registration_certificate_snapshot
   SET version_id = 11
 WHERE id = 201;
'@
    Assert-SqlFails -Schema $schema -Label 'cross-snapshot projection reattachment' -Sql @'
UPDATE dcc_registration_certificate_snapshot_entrusted
   SET snapshot_id = 101
 WHERE id = 2001;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'draft projection delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot_entrusted WHERE id = 2001;
'@)
    Write-Output 'PASS: draft entrusted projection delete'
    [void](Invoke-SqlSuccess -Schema $schema -Label 'draft snapshot delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot WHERE id = 201;
'@)
    Write-Output 'PASS: draft snapshot delete'
    [void](Invoke-SqlSuccess -Schema $schema -Label 'recreate draft snapshot and projection' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (id, version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (201, 21, 1, 'Draft Product', 'Draft Registrant', b'1', b'0',
        JSON_ARRAY(JSON_OBJECT('enterpriseId', 31, 'enterpriseName', 'Draft Factory')), NOW(), 1);
INSERT INTO dcc_registration_certificate_snapshot_entrusted
  (id, snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id)
VALUES (2001, 201, 31, 'Draft Factory', 1, 1);
UPDATE dcc_registration_certificate_version
   SET status = 'CURRENT', formalized_at = NOW(), formalized_by = 99
 WHERE id = 21;
'@)
    Assert-SqlFails -Schema $schema -Label 'formalized draft snapshot overwrite' -Sql @'
UPDATE dcc_registration_certificate_snapshot SET product_name = 'OVERWRITE' WHERE id = 201;
'@
    Assert-SqlFails -Schema $schema -Label 'formalized draft snapshot delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot WHERE id = 201;
'@
    Assert-SqlFails -Schema $schema -Label 'formalized draft projection overwrite' -Sql @'
UPDATE dcc_registration_certificate_snapshot_entrusted
   SET enterprise_name_snapshot = 'OVERWRITE' WHERE id = 2001;
'@
    Assert-SqlFails -Schema $schema -Label 'formalized draft projection delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot_entrusted WHERE id = 2001;
'@

    [void](Invoke-SqlSuccess -Schema $schema -Label 'orphan snapshot fail-closed fixture' -Sql @'
INSERT INTO dcc_registration_certificate_snapshot
  (id, version_id, revision_no, product_name, registrant_name,
   entrusted_production, self_production, entrusted_enterprises_json, effective_at, tenant_id)
VALUES (301, 999, 1, 'Orphan Product', 'Orphan Registrant', b'1', b'0',
        JSON_ARRAY(JSON_OBJECT('enterpriseId', 32, 'enterpriseName', 'Orphan Factory')), NOW(), 1);
INSERT INTO dcc_registration_certificate_snapshot_entrusted
  (id, snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id)
VALUES (3001, 301, 32, 'Orphan Factory', 1, 1);
'@)
    Assert-SqlFails -Schema $schema -Label 'orphan snapshot update' -Sql @'
UPDATE dcc_registration_certificate_snapshot SET product_name = 'OVERWRITE' WHERE id = 301;
'@
    Assert-SqlFails -Schema $schema -Label 'orphan snapshot delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot WHERE id = 301;
'@
    Assert-SqlFails -Schema $schema -Label 'orphan projection update' -Sql @'
UPDATE dcc_registration_certificate_snapshot_entrusted
   SET enterprise_name_snapshot = 'OVERWRITE' WHERE id = 3001;
'@
    Assert-SqlFails -Schema $schema -Label 'orphan projection delete' -Sql @'
DELETE FROM dcc_registration_certificate_snapshot_entrusted WHERE id = 3001;
'@

    $auditColumns = Invoke-SqlSuccess -Schema $schema -Label 'audit field metadata' -Sql @"
SELECT CONCAT(COLUMN_NAME, '|', DATA_TYPE, '|', IS_NULLABLE)
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = '$schema'
   AND TABLE_NAME = 'dcc_registration_certificate_audit'
   AND COLUMN_NAME IN ('owner_company_id', 'business_file_id', 'result', 'result_code', 'request_trace_id')
 ORDER BY COLUMN_NAME;
"@
    Assert-Equal -Expected (@(
        'business_file_id|bigint|YES',
        'owner_company_id|bigint|NO',
        'request_trace_id|varchar|NO',
        'result|varchar|NO',
        'result_code|varchar|YES'
    ) -join "`n") -Actual $auditColumns -Label 'complete audit field metadata'

    $newChecks = Invoke-SqlSuccess -Schema $schema -Label 'file and audit CHECK metadata' -Sql @"
SELECT tc.CONSTRAINT_NAME
  FROM information_schema.TABLE_CONSTRAINTS tc
 WHERE tc.CONSTRAINT_SCHEMA = '$schema'
   AND tc.CONSTRAINT_NAME IN ('chk_dcc_reg_cert_file_status', 'chk_dcc_reg_cert_audit_result',
                              'chk_dcc_reg_cert_audit_trace')
 ORDER BY tc.CONSTRAINT_NAME;
"@
    Assert-Equal -Expected (@(
        'chk_dcc_reg_cert_audit_result',
        'chk_dcc_reg_cert_audit_trace',
        'chk_dcc_reg_cert_file_status'
    ) -join "`n") -Actual $newChecks -Label 'file status and audit CHECK contracts'

    Assert-SqlFails -Schema $schema -Label 'unknown file status' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, tenant_id)
VALUES ('VERSION', 11, 'REGISTRATION_CERTIFICATE', 499, 'unknown.pdf', 'application/pdf',
        10, REPEAT('f', 64), 'UNKNOWN', 1);
'@
    Assert-SqlFails -Schema $schema -Label 'direct BOUND file missing binding evidence' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, tenant_id)
VALUES ('VERSION', 11, 'REGISTRATION_CERTIFICATE', 498, 'missing-binding.pdf', 'application/pdf',
        10, REPEAT('e', 64), 'BOUND', 1);
'@
    Assert-SqlFails -Schema $schema -Label 'direct VOIDED file missing binding actor' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, bound_at, tenant_id)
VALUES ('VERSION', 11, 'REGISTRATION_CERTIFICATE', 497, 'missing-actor.pdf', 'application/pdf',
        10, REPEAT('d', 64), 'VOIDED', NOW(), 1);
'@
    Assert-SqlFails -Schema $schema -Label 'STAGED file carries binding evidence' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, bound_at, bound_by, tenant_id)
VALUES ('VERSION', 11, 'REGISTRATION_CERTIFICATE', 496, 'staged-bound.pdf', 'application/pdf',
        10, REPEAT('c', 64), 'STAGED', NOW(), 99, 1);
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'file lifecycle fixtures' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, bound_at, bound_by, tenant_id)
VALUES
  (4001, 'VERSION', 11, 'REGISTRATION_CERTIFICATE', 501, 'staged-bind.pdf', 'application/pdf',
   10, REPEAT('1', 64), 'STAGED', NULL, NULL, 1),
  (4002, 'VERSION', 11, 'REGISTRATION_CERTIFICATE', 502, 'staged-cleanup.pdf', 'application/pdf',
   10, REPEAT('2', 64), 'STAGED', NULL, NULL, 1),
  (4003, 'VERSION', 11, 'REGISTRATION_CERTIFICATE', 503, 'terminal-cleanup.pdf', 'application/pdf',
   10, REPEAT('3', 64), 'CLEANUP_REQUIRED', NULL, NULL, 1),
  (4004, 'VERSION', 11, 'REGISTRATION_CERTIFICATE', 504, 'terminal-void.pdf', 'application/pdf',
   10, REPEAT('4', 64), 'VOIDED', NOW(), 99, 1),
  (4005, 'VERSION', 11, 'REGISTRATION_CERTIFICATE', 505, 'illegal-staged-void.pdf', 'application/pdf',
   10, REPEAT('5', 64), 'STAGED', NULL, NULL, 1);
'@)
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed staged file transitions' -Sql @'
UPDATE dcc_registration_certificate_file
   SET status = 'BOUND', bound_at = NOW(), bound_by = 99
 WHERE id = 4001;
UPDATE dcc_registration_certificate_file
   SET status = 'CLEANUP_REQUIRED'
 WHERE id = 4002;
'@)
    Write-Output 'PASS: allowed STAGED file transitions'
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed file status idempotency' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'BOUND' WHERE id = 4001;
UPDATE dcc_registration_certificate_file SET status = 'CLEANUP_REQUIRED' WHERE id = 4002;
'@)
    Write-Output 'PASS: allowed file status idempotency'
    Assert-SqlFails -Schema $schema -Label 'BOUND file returns to STAGED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'STAGED' WHERE id = 4001;
'@
    Assert-SqlFails -Schema $schema -Label 'BOUND file changes to CLEANUP_REQUIRED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'CLEANUP_REQUIRED' WHERE id = 4001;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'allowed bound to void transition' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'VOIDED' WHERE id = 4001;
UPDATE dcc_registration_certificate_file SET status = 'VOIDED' WHERE id = 4001;
'@)
    Write-Output 'PASS: allowed BOUND to VOIDED and terminal idempotency'
    Assert-SqlFails -Schema $schema -Label 'CLEANUP_REQUIRED file returns to STAGED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'STAGED' WHERE id = 4003;
'@
    Assert-SqlFails -Schema $schema -Label 'CLEANUP_REQUIRED file changes to BOUND' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'BOUND' WHERE id = 4003;
'@
    Assert-SqlFails -Schema $schema -Label 'CLEANUP_REQUIRED file changes to VOIDED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'VOIDED' WHERE id = 4003;
'@
    Assert-SqlFails -Schema $schema -Label 'VOIDED file returns to BOUND' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'BOUND' WHERE id = 4004;
'@
    Assert-SqlFails -Schema $schema -Label 'VOIDED file returns to STAGED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'STAGED' WHERE id = 4004;
'@
    Assert-SqlFails -Schema $schema -Label 'VOIDED file changes to CLEANUP_REQUIRED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'CLEANUP_REQUIRED' WHERE id = 4004;
'@
    Assert-SqlFails -Schema $schema -Label 'STAGED file skips directly to VOIDED' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'VOIDED' WHERE id = 4005;
'@
    Assert-SqlFails -Schema $schema -Label 'STAGED file becomes BOUND without evidence' -Sql @'
UPDATE dcc_registration_certificate_file SET status = 'BOUND' WHERE id = 4005;
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'bound file fixture' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, bound_at, bound_by, tenant_id)
VALUES ('VERSION', 11, 'REGISTRATION_CERTIFICATE', 500, 'a.pdf', 'application/pdf',
        10, REPEAT('a', 64), 'BOUND', NOW(), 99, 1);
'@)
    Assert-SqlFails -Schema $schema -Label 'duplicate tenant bound infra file' -Sql @'
INSERT INTO dcc_registration_certificate_file
  (owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
   file_size, sha256, status, bound_at, bound_by, tenant_id)
VALUES ('VERSION', 12, 'REGISTRATION_CERTIFICATE', 500, 'b.pdf', 'application/pdf',
        10, REPEAT('b', 64), 'BOUND', NOW(), 99, 1);
'@
    Assert-SqlFails -Schema $schema -Label 'bound file metadata overwrite' -Sql @'
UPDATE dcc_registration_certificate_file
   SET original_name = 'overwrite.pdf'
 WHERE infra_file_id = 500 AND tenant_id = 1;
'@

    Assert-SqlFails -Schema $schema -Label 'blank audit event key' -Sql @'
INSERT INTO dcc_registration_certificate_audit
  (tenant_id, owner_company_id, certificate_id, event_key, event_type, result,
   request_trace_id, detail_json, occurred_at)
VALUES (1, 10, 1, '   ', 'FORMALIZED', 'SUCCESS', 'trace-blank-event', JSON_OBJECT(), NOW());
'@
    Assert-SqlFails -Schema $schema -Label 'unknown audit result' -Sql @'
INSERT INTO dcc_registration_certificate_audit
  (tenant_id, owner_company_id, certificate_id, event_key, event_type, result,
   request_trace_id, detail_json, occurred_at)
VALUES (1, 10, 1, 'cert:1:bad-result', 'FORMALIZED', 'UNKNOWN', 'trace-bad-result',
        JSON_OBJECT(), NOW());
'@
    Assert-SqlFails -Schema $schema -Label 'blank audit request trace' -Sql @'
INSERT INTO dcc_registration_certificate_audit
  (tenant_id, owner_company_id, certificate_id, event_key, event_type, result,
   request_trace_id, detail_json, occurred_at)
VALUES (1, 10, 1, 'cert:1:blank-trace', 'FORMALIZED', 'FAILURE', '   ', JSON_OBJECT(), NOW());
'@
    [void](Invoke-SqlSuccess -Schema $schema -Label 'audit event fixture' -Sql @'
INSERT INTO dcc_registration_certificate_audit
  (tenant_id, owner_company_id, certificate_id, business_file_id, event_key, event_type,
   result, result_code, request_trace_id, detail_json, occurred_at)
VALUES (1, 10, 1, 500, 'cert:1:formalized', 'FORMALIZED', 'SUCCESS', 'OK',
        'trace-formalized-1', JSON_OBJECT(), NOW());
'@)
    Assert-SqlFails -Schema $schema -Label 'duplicate tenant audit event key' -Sql @'
INSERT INTO dcc_registration_certificate_audit
  (tenant_id, owner_company_id, certificate_id, event_key, event_type, result,
   request_trace_id, detail_json, occurred_at)
VALUES (1, 10, 1, 'cert:1:formalized', 'FORMALIZED', 'SUCCESS', 'trace-duplicate',
        JSON_OBJECT(), NOW());
'@
    Assert-SqlFails -Schema $schema -Label 'audit overwrite' -Sql @'
UPDATE dcc_registration_certificate_audit
   SET event_type = 'OVERWRITE'
 WHERE tenant_id = 1 AND event_key = 'cert:1:formalized';
'@
    Assert-SqlFails -Schema $schema -Label 'audit delete' -Sql @'
DELETE FROM dcc_registration_certificate_audit
 WHERE tenant_id = 1 AND event_key = 'cert:1:formalized';
'@

    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Sql $migrationSql `
        -Label 'incompatible six-table baseline migration')
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'break ordinary column contract' -Sql @'
ALTER TABLE dcc_registration_certificate_snapshot
  MODIFY COLUMN registrant_name bigint NOT NULL;
'@)
    Assert-SqlFails -Schema $incompatibleSchema -Sql $migrationSql `
        -Label 'incompatible six-table ordinary column type' `
        -ExpectedMessage 'DCC registration certificate core column contract mismatch'
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'restore ordinary column contract' -Sql @'
ALTER TABLE dcc_registration_certificate_snapshot
  MODIFY COLUMN registrant_name varchar(255) NOT NULL COMMENT 'Registrant name snapshot';
'@)
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'break ordinary column nullability' -Sql @'
ALTER TABLE dcc_registration_certificate_snapshot
  MODIFY COLUMN registrant_name varchar(255) NULL;
'@)
    Assert-SqlFails -Schema $incompatibleSchema -Sql $migrationSql `
        -Label 'incompatible six-table ordinary column nullability' `
        -ExpectedMessage 'DCC registration certificate core column contract mismatch'
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'restore ordinary column nullability' -Sql @'
ALTER TABLE dcc_registration_certificate_snapshot
  MODIFY COLUMN registrant_name varchar(255) NOT NULL COMMENT 'Registrant name snapshot';
'@)
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'weaken generated expression with tautology' -Sql @'
ALTER TABLE dcc_registration_certificate_version
  MODIFY COLUMN current_unique_flag tinyint GENERATED ALWAYS AS
    (CASE WHEN ((deleted = b'0' AND status = 'CURRENT') OR 1 = 1) THEN 1 ELSE NULL END) STORED;
'@)
    Assert-SqlFails -Schema $incompatibleSchema -Sql $migrationSql `
        -Label 'incompatible six-table weakened generated expression' `
        -ExpectedMessage 'DCC registration certificate core exact generated expression mismatch'
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'restore generated expression' -Sql @'
ALTER TABLE dcc_registration_certificate_version
  MODIFY COLUMN current_unique_flag tinyint GENERATED ALWAYS AS
    (CASE WHEN (deleted = b'0' AND status = 'CURRENT') THEN 1 ELSE NULL END) STORED;
'@)
    [void](Invoke-SqlSuccess -Schema $incompatibleSchema -Label 'break named CHECK expression' -Sql @'
ALTER TABLE dcc_registration_certificate_file
  DROP CHECK chk_dcc_reg_cert_file_status,
  ADD CONSTRAINT chk_dcc_reg_cert_file_status CHECK
    (status IN ('STAGED', 'BOUND', 'CLEANUP_REQUIRED', 'VOIDED') OR 1 = 1);
'@)
    Assert-SqlFails -Schema $incompatibleSchema -Sql $migrationSql `
        -Label 'incompatible six-table named CHECK expression' `
        -ExpectedMessage 'DCC registration certificate core exact CHECK expression mismatch'

    Write-Output 'PASS: runtime MySQL generated columns, CHECKs, and conditional uniqueness'
}
finally {
    Remove-OwnedSchemas -Schemas $createdSchemas
    Write-Output 'PASS: isolated MySQL schemas removed'
}
