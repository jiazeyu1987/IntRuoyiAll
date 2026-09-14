$ErrorActionPreference = 'Stop'

$base = 'E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1244-pqc-active-order-latest-hotfix.jar'
$stamp = Get-Date -Format 'yyyyMMdd-HHmm'
$new = "E:\IntRuoyi\output\runtime\int_main\backend-latest-$stamp-pqc-snapshot-process-hotfix.jar"
$module = 'E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes'
$work = Join-Path $module "target-pqc-route-snapshot\hotfix-$stamp"
$mainOut = Join-Path $module 'target-pqc-route-snapshot\classes17'

if (-not (Test-Path -LiteralPath $base)) {
    throw "Base runtime jar not found: $base"
}
if (Test-Path -LiteralPath $work) {
    throw "Hotfix work directory already exists: $work"
}

New-Item -ItemType Directory -Force -Path $work | Out-Null
Copy-Item -LiteralPath $base -Destination $new -Force

Push-Location $work
try {
    jar xf $new BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar
    $nested = Join-Path $work 'BOOT-INF\lib\yudao-module-mes-2026.04-SNAPSHOT.jar'

    $relClasses = @(
        'cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRouteProcessCandidate.class',
        'cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcTaskOption.class',
        'cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.class',
        'cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderProcessSnapshotMapper.class',
        'cn/iocoder/yudao/module/mes/dal/mysql/qa/regulation/MesQaInspectionRegulationVersionMapper.class',
        'cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.class'
    )

    $serviceDir = Join-Path $mainOut 'cn\iocoder\yudao\module\mes\service\pro\frontline'
    $serviceClasses = Get-ChildItem -LiteralPath $serviceDir -Filter 'MesFrontlinePqcContextServiceImpl*.class' |
        ForEach-Object { $_.FullName.Substring($mainOut.Length + 1).Replace('\', '/') }

    $respDir = Join-Path $mainOut 'cn\iocoder\yudao\module\mes\controller\admin\pro\feedback\vo\frontline'
    $respClasses = Get-ChildItem -LiteralPath $respDir -Filter 'MesFrontlineRouteProcessRespVO*.class' |
        ForEach-Object { $_.FullName.Substring($mainOut.Length + 1).Replace('\', '/') }

    $allClasses = @($relClasses + $serviceClasses + $respClasses) | Sort-Object -Unique
    foreach ($rel in $allClasses) {
        $full = Join-Path $mainOut ($rel.Replace('/', '\'))
        if (-not (Test-Path -LiteralPath $full)) {
            throw "Missing compiled class: $rel"
        }
        jar uf $nested -C $mainOut $rel
    }

    jar uf0 $new -C $work BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar
} finally {
    Pop-Location
}

$sha = (Get-FileHash -Algorithm SHA256 -LiteralPath $new).Hash
[PSCustomObject]@{
    Jar = $new
    Sha256 = $sha
    UpdatedClassCount = $allClasses.Count
    Classes = ($allClasses -join ';')
} | Format-List
