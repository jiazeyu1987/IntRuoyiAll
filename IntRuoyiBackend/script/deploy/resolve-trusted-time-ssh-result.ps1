Set-StrictMode -Version Latest

function Protect-TrustedTimeDiagnostic {
    param([AllowEmptyString()][string]$Text = '')

    $assignmentPattern = '(?i)\b(MYSQL_ROOT_PASSWORD|MYSQL_PWD|PASSWORD|PASSWD|TOKEN|SECRET|ACCESS_KEY|SECRET_KEY|API_KEY)\b\s*[:=]\s*(?:"[^"\r\n]*"|''[^''\r\n]*''|[^\s;]+)'
    $redacted = [regex]::Replace($Text, $assignmentPattern, '$1=<REDACTED>')
    $mysqlPasswordPattern = '(?i)(^|\s)-p(?:"[^"\r\n]*"|''[^''\r\n]*''|[^\s;]+)'
    return [regex]::Replace($redacted, $mysqlPasswordPattern, '$1-p<REDACTED>')
}

function Resolve-TrustedTimeSshResult {
    param(
        [int]$ExitCode,
        [AllowEmptyString()][string]$StdOut = '',
        [AllowEmptyString()][string]$StdErr = ''
    )

    if ($ExitCode -ne 0) {
        $diagnostic = Protect-TrustedTimeDiagnostic -Text $StdErr.Trim()
        throw "SSH command failed with exit code ${ExitCode}: $diagnostic"
    }

    $stderrLines = @(($StdErr -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $knownClosedSocketPattern = '^close - IO is still pending on closed socket\. read:\d+, write:\d+, io:(?:0x)?[0-9A-Fa-f]+$'
    $unexpected = @($stderrLines | Where-Object { $_ -notmatch $knownClosedSocketPattern })
    if ($unexpected.Count -gt 0) {
        $diagnostic = Protect-TrustedTimeDiagnostic -Text ($unexpected -join ' | ')
        throw "SSH command emitted unexpected stderr with exit code 0: $diagnostic"
    }

    if ([string]::IsNullOrWhiteSpace($StdOut)) {
        throw 'SSH command returned empty stdout'
    }
    return $StdOut.Trim()
}
