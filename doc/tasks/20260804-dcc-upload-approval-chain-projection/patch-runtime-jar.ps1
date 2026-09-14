$ErrorActionPreference = 'Stop'

$repoRoot = 'E:\IntRuoyi'
$oldJar = Join-Path $repoRoot 'output\runtime\int_main\backend-runtime-control-20260804-dcc-nas-uncontrolled-import.jar'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$newJar = Join-Path $repoRoot "output\runtime\int_main\backend-runtime-control-20260804-dcc-upload-approval-chain-projection-$stamp.jar"
$tempRoot = Join-Path $repoRoot "output\runtime\int_main\patch-20260804-dcc-upload-approval-chain-projection-$stamp"
$nestedEntry = 'BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar'
$classRoot = Join-Path $repoRoot 'IntRuoyiBackend\yudao-module-dcc\target\classes'
$relativeClasses = @(
  'cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileCategoryController.class',
  'cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalMatrixAdminService.class',
  'cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalMatrixAdminService$MatrixPositionIds.class',
  'cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalMatrixAdminServiceImpl.class'
)

if (-not (Test-Path $oldJar)) {
  throw "Missing source runtime jar: $oldJar"
}
if (Test-Path $newJar) {
  throw "Refusing to overwrite existing runtime jar: $newJar"
}
if (Test-Path $tempRoot) {
  throw "Refusing to reuse existing patch directory: $tempRoot"
}

New-Item -ItemType Directory -Path $tempRoot | Out-Null
Copy-Item -LiteralPath $oldJar -Destination $newJar

Push-Location $tempRoot
& jar xf $newJar $nestedEntry
if ($LASTEXITCODE -ne 0) {
  throw "jar extract nested module failed: $LASTEXITCODE"
}
Pop-Location

$moduleJar = Join-Path $tempRoot $nestedEntry
$classTemp = Join-Path $tempRoot 'classes'
foreach ($relativeClass in $relativeClasses) {
  $sourceClass = Join-Path $classRoot ($relativeClass -replace '/', '\')
  if (-not (Test-Path $sourceClass)) {
    throw "Missing compiled class: $sourceClass"
  }
  $targetClass = Join-Path $classTemp ($relativeClass -replace '/', '\')
  New-Item -ItemType Directory -Path (Split-Path $targetClass -Parent) -Force | Out-Null
  Copy-Item -LiteralPath $sourceClass -Destination $targetClass
}

Push-Location $classTemp
& jar uf $moduleJar @relativeClasses
if ($LASTEXITCODE -ne 0) {
  throw "jar update module failed: $LASTEXITCODE"
}
Pop-Location

Push-Location $tempRoot
& jar uf0 $newJar $nestedEntry
if ($LASTEXITCODE -ne 0) {
  throw "jar update outer failed: $LASTEXITCODE"
}
Pop-Location

$javapText = (& javap -classpath $moduleJar cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminService) -join "`n"
if ($javapText -notmatch 'getActiveMatrixPositionIdsByCategoryIds') {
  throw 'Patched module jar does not expose getActiveMatrixPositionIdsByCategoryIds'
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($newJar)
try {
  $entry = $zip.GetEntry($nestedEntry)
  if ($null -eq $entry) {
    throw "Missing nested module entry after patch: $nestedEntry"
  }
  $nestedStored = $entry.CompressedLength -eq $entry.Length
  if (-not $nestedStored) {
    throw "Nested module entry is compressed: compressed=$($entry.CompressedLength), size=$($entry.Length)"
  }
} finally {
  $zip.Dispose()
}

$hash = Get-FileHash -Algorithm SHA256 $newJar
[PSCustomObject]@{
  newJar = $newJar
  sha256 = $hash.Hash
  nestedEntry = $nestedEntry
  nestedEntryStored = $nestedStored
  tempRoot = $tempRoot
  patchedClasses = $relativeClasses
} | ConvertTo-Json -Depth 4
