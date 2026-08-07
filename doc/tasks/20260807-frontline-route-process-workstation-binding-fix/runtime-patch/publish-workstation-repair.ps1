$ErrorActionPreference = 'Stop'

$frontendEnvPath = 'E:\IntRuoyi\IntRuoyiFronted\.env'
$apiBase = 'http://127.0.0.1:48081/admin-api'
$routeId = 922119
$sourceVersionId = 490
$candidateId = 626
$expectedBindings = @{
    922985 = 980010
    922986 = 980008
    922987 = 980009
    922988 = 980011
    922989 = 980012
    922990 = 980013
    922991 = 980014
    922992 = 980015
    922993 = 980016
    922994 = 980017
    922995 = 980018
    922996 = 980019
    922997 = 980020
    922998 = 980021
}

function Resolve-ExpectedWorkstationId([long] $processId) {
    $key = [int] $processId
    if (-not $expectedBindings.ContainsKey($key)) {
        throw "Missing approved workstation mapping: processId=$processId"
    }
    $workstationId = [long] $expectedBindings[$key]
    if ($workstationId -le 0) {
        throw "Approved workstation mapping is invalid: processId=$processId"
    }
    return $workstationId
}

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
    $response = Invoke-RestMethod -Method Get -Uri ($apiBase + $path) -Headers $headers
    return Require-Success $response "GET $path"
}

function Invoke-ApiPost([string] $path, $body, $headers) {
    $response = Invoke-RestMethod -Method Post -Uri ($apiBase + $path) -Headers $headers `
        -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Depth 20 -Compress)
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
if (-not $loginData.accessToken) {
    throw 'Login response is missing access token'
}
$headers = @{
    Authorization = 'Bearer ' + $loginData.accessToken
    'tenant-id' = [string] $tenantId
}

$versionsBefore = @(Invoke-ApiGet "/mes/pro/route-version/list-by-route?routeId=$routeId" $headers)
$candidate = $versionsBefore | Where-Object { [long] $_.id -eq $candidateId } | Select-Object -First 1
if ($null -eq $candidate -or $candidate.lifecycleStatus -ne 'DRAFT' -or $candidate.active) {
    throw "Candidate $candidateId is not the expected DRAFT version"
}
if ([long] $candidate.sourceRouteVersionId -ne $sourceVersionId) {
    throw "Candidate $candidateId source version changed"
}
$openCandidates = @($versionsBefore | Where-Object {
    -not $_.active -and $_.lifecycleStatus -in @('DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH')
})
if ($openCandidates.Count -ne 1 -or [long] $openCandidates[0].id -ne $candidateId) {
    throw 'Route open-candidate precondition changed'
}

$graph = Invoke-ApiGet "/mes/pro/route-process-flow/get?routeId=$routeId&routeVersionId=$candidateId" $headers
$nodes = @($graph.nodes)
if ($nodes.Count -ne $expectedBindings.Count) {
    throw "Candidate node count mismatch: expected=$($expectedBindings.Count), actual=$($nodes.Count)"
}
$actualProcessIds = @($nodes | ForEach-Object { [long] $_.processId } | Sort-Object)
$expectedProcessIds = @($expectedBindings.Keys | ForEach-Object { [long] $_ } | Sort-Object)
if (($actualProcessIds -join ',') -ne ($expectedProcessIds -join ',')) {
    throw 'Candidate process identity set does not match the approved repair map'
}
$nullBindingCount = @($nodes | Where-Object { $null -eq $_.routeProcessWorkstationId }).Count
if ($nullBindingCount -ne $nodes.Count) {
    throw 'Candidate already contains an unexpected formal workstation binding'
}
$candidateInitialBindingState = 'EMPTY'

$updates = foreach ($node in $nodes) {
    $processId = [long] $node.processId
    [ordered]@{
        id = [long] $node.routeProcessId
        routeId = $routeId
        processId = $processId
        workstationId = Resolve-ExpectedWorkstationId $processId
        sort = [int] $node.sort
        keyFlag = [bool] $node.keyFlag
        checkFlag = [bool] $node.checkFlag
    }
}
$saveBody = [ordered]@{
    routeId = $routeId
    routeVersionId = $candidateId
    graphVersion = [long] $graph.graphVersion
    edges = @($graph.edges)
    boundaryEdges = @($graph.boundaryEdges)
    layouts = @($graph.layouts)
    routeProcessCreates = @()
    routeProcessUpdates = @($updates)
    routeProcessDeletes = @()
}
$validation = Invoke-ApiPost '/mes/pro/route-process-flow/validate' $saveBody $headers
if (-not $validation.valid -or $validation.validationStatus -ne 'VALID') {
    throw "Candidate graph validation failed: status=$($validation.validationStatus)"
}
$saved = Invoke-ApiPost '/mes/pro/route-process-flow/save' $saveBody $headers
if (-not $saved.valid -or $saved.validationStatus -ne 'VALID') {
    throw "Candidate graph save failed: status=$($saved.validationStatus)"
}

$savedGraph = Invoke-ApiGet "/mes/pro/route-process-flow/get?routeId=$routeId&routeVersionId=$candidateId" $headers
$savedNodes = @($savedGraph.nodes)
foreach ($node in $savedNodes) {
    $processId = [long] $node.processId
    $expectedWorkstationId = Resolve-ExpectedWorkstationId $processId
    if ([long] $node.routeProcessWorkstationId -ne $expectedWorkstationId) {
        throw "Saved formal binding mismatch: processId=$processId"
    }
}
$blockers = Invoke-ApiGet "/mes/pro/route-version/blockers?id=$candidateId" $headers
if (-not $blockers.publishable -or @($blockers.blockers).Count -ne 0) {
    throw "Candidate is not publishable: $(@($blockers.blockers) -join '; ')"
}

$published = Invoke-ApiPost "/mes/pro/route-version/submit-publish?id=$candidateId" @{} $headers
if (-not $published.active -or $published.lifecycleStatus -ne 'ACTIVE') {
    throw "Candidate did not publish directly: status=$($published.lifecycleStatus)"
}
$versionsAfter = @(Invoke-ApiGet "/mes/pro/route-version/list-by-route?routeId=$routeId" $headers)
$activeVersions = @($versionsAfter | Where-Object { $_.active })
if ($activeVersions.Count -ne 1 -or [long] $activeVersions[0].id -ne $candidateId) {
    throw 'Published route does not have the repair candidate as its unique active version'
}
$liveGraph = Invoke-ApiGet "/mes/pro/route-process-flow/get?routeId=$routeId" $headers
$liveNodes = @($liveGraph.nodes)
if ($liveNodes.Count -ne $expectedBindings.Count) {
    throw 'Published live graph node count mismatch'
}
foreach ($node in $liveNodes) {
    $processId = [long] $node.processId
    $expectedWorkstationId = Resolve-ExpectedWorkstationId $processId
    if ([long] $node.routeProcessWorkstationId -ne $expectedWorkstationId) {
        throw "Published formal binding mismatch: processId=$processId"
    }
}

[pscustomobject]@{
    TenantId = $tenantId
    RouteId = $routeId
    PreviousActiveVersionId = $sourceVersionId
    ActiveVersionId = $candidateId
    ActiveVersionNo = $published.versionNo
    CandidateInitialBindingState = $candidateInitialBindingState
    CandidateGraphVersionBefore = $graph.graphVersion
    CandidateGraphVersionAfter = $saved.graphVersion
    PublishedNodeCount = $liveNodes.Count
    PublishedFormalBindingCount = @($liveNodes | Where-Object { $null -ne $_.routeProcessWorkstationId }).Count
    BlockerCount = @($blockers.blockers).Count
} | Format-List
