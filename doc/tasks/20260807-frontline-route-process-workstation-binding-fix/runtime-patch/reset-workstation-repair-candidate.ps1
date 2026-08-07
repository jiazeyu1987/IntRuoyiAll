$ErrorActionPreference = 'Stop'

$frontendEnvPath = 'E:\IntRuoyi\IntRuoyiFronted\.env'
$apiBase = 'http://127.0.0.1:48081/admin-api'
$routeId = 922119
$activeVersionId = 490
$invalidCandidateId = 626

function Read-FrontendEnv([string] $name) {
    $line = Get-Content -Encoding utf8 -LiteralPath $frontendEnvPath |
        Where-Object { $_ -match ('^\s*' + [regex]::Escape($name) + '\s*=') } |
        Select-Object -Last 1
    if (-not $line) {
        throw "Missing $name in frontend .env"
    }
    return (($line -split '=', 2)[1]).Trim().Trim('"').Trim("'")
}

function Require-Success($response, [string] $operation) {
    if ($null -eq $response -or $response.code -notin 0, 200) {
        throw "$operation failed: code=$($response.code), msg=$($response.msg)"
    }
    return $response.data
}

function Invoke-ApiGet([string] $path, $headers) {
    return Require-Success (Invoke-RestMethod -Method Get -Uri ($apiBase + $path) -Headers $headers) "GET $path"
}

function Invoke-ApiPost([string] $path, $body, $headers) {
    $response = Invoke-RestMethod -Method Post -Uri ($apiBase + $path) -Headers $headers `
        -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Depth 10 -Compress)
    return Require-Success $response "POST $path"
}

$tenantName = Read-FrontendEnv 'VITE_APP_DEFAULT_LOGIN_TENANT'
$username = Read-FrontendEnv 'VITE_APP_DEFAULT_LOGIN_USERNAME'
$password = Read-FrontendEnv 'VITE_APP_DEFAULT_LOGIN_PASSWORD'
$tenantResponse = Invoke-RestMethod -Method Get `
    -Uri ($apiBase + '/system/tenant/get-id-by-name?name=' + [uri]::EscapeDataString($tenantName))
$tenantId = Require-Success $tenantResponse 'tenant lookup'
$loginResponse = Invoke-RestMethod -Method Post -Uri ($apiBase + '/system/auth/login') `
    -Headers @{ 'tenant-id' = [string] $tenantId } -ContentType 'application/json; charset=utf-8' `
    -Body (@{ username = $username; password = $password } | ConvertTo-Json -Compress)
$loginData = Require-Success $loginResponse 'login'
$headers = @{
    Authorization = 'Bearer ' + $loginData.accessToken
    'tenant-id' = [string] $tenantId
}

$versionsBefore = @(Invoke-ApiGet "/mes/pro/route-version/list-by-route?routeId=$routeId" $headers)
$activeBefore = @($versionsBefore | Where-Object { $_.active })
$invalidCandidate = $versionsBefore | Where-Object { [long] $_.id -eq $invalidCandidateId } | Select-Object -First 1
if ($activeBefore.Count -ne 1 -or [long] $activeBefore[0].id -ne $activeVersionId) {
    throw 'Active route version changed before candidate reset'
}
if ($null -eq $invalidCandidate -or $invalidCandidate.lifecycleStatus -ne 'DRAFT' -or $invalidCandidate.active) {
    throw 'Task-owned invalid candidate is no longer the expected DRAFT version'
}

$cancelled = Invoke-ApiPost "/mes/pro/route-version/cancel?id=$invalidCandidateId" @{} $headers
if ($cancelled.lifecycleStatus -ne 'CANCELLED' -or $cancelled.active) {
    throw 'Task-owned invalid candidate did not enter CANCELLED state'
}

$created = Invoke-ApiPost '/mes/pro/route-version/create-candidate' ([ordered]@{
    routeId = $routeId
    sourceRouteVersionId = $activeVersionId
    changeReason = 'Repair formal route-process workstation bindings for frontline production'
}) $headers
if ($created.lifecycleStatus -ne 'DRAFT' -or $created.active -or
    [long] $created.sourceRouteVersionId -ne $activeVersionId) {
    throw 'Replacement candidate was not created from the expected active version'
}

$replacementId = [long] $created.id
$graph = Invoke-ApiGet "/mes/pro/route-process-flow/get?routeId=$routeId&routeVersionId=$replacementId" $headers
$nodes = @($graph.nodes)
if ($nodes.Count -ne 14 -or @($nodes | Where-Object { $null -ne $_.routeProcessWorkstationId }).Count -ne 0) {
    throw 'Replacement candidate does not have the expected empty formal binding baseline'
}
$versionsAfter = @(Invoke-ApiGet "/mes/pro/route-version/list-by-route?routeId=$routeId" $headers)
$openCandidates = @($versionsAfter | Where-Object {
    -not $_.active -and $_.lifecycleStatus -in @('DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH')
})
if ($openCandidates.Count -ne 1 -or [long] $openCandidates[0].id -ne $replacementId) {
    throw 'Replacement candidate is not the unique open candidate'
}

[pscustomobject]@{
    TenantId = $tenantId
    RouteId = $routeId
    ActiveVersionId = $activeVersionId
    CancelledCandidateId = $invalidCandidateId
    ReplacementCandidateId = $replacementId
    ReplacementVersionNo = $created.versionNo
    ReplacementGraphVersion = $graph.graphVersion
    ReplacementNodeCount = $nodes.Count
    ReplacementFormalBindingCount = @($nodes | Where-Object { $null -ne $_.routeProcessWorkstationId }).Count
} | Format-List
