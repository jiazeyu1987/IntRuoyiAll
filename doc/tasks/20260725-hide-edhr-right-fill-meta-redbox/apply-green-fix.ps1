$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Read-Utf8 {
  param([string] $Path)
  [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Write-Utf8 {
  param([string] $Path, [string] $Source)
  [System.IO.File]::WriteAllText($Path, $Source, $utf8NoBom)
}

function Get-Newline {
  param([string] $Source)
  if ($Source.Contains("`r`n")) { "`r`n" } else { "`n" }
}

function Use-Newline {
  param([string] $Text, [string] $Newline)
  $Text.Replace("`n", $Newline)
}

function Replace-Required {
  param(
    [ref] $Source,
    [string] $Old,
    [string] $New
  )
  $newline = Get-Newline $Source.Value
  $candidateOld = Use-Newline $Old $newline
  $candidateNew = Use-Newline $New $newline
  if (-not $Source.Value.Contains($candidateOld)) {
    throw "Missing expected token: $($Old.Substring(0, [Math]::Min(100, $Old.Length)))"
  }
  $Source.Value = $Source.Value.Replace($candidateOld, $candidateNew)
}

function Remove-Between {
  param(
    [ref] $Source,
    [string] $Start,
    [string] $End
  )
  $newline = Get-Newline $Source.Value
  $candidateStart = Use-Newline $Start $newline
  $candidateEnd = Use-Newline $End $newline
  $startIndex = $Source.Value.IndexOf($candidateStart)
  if ($startIndex -lt 0) {
    throw "Missing start anchor: $($Start.Substring(0, [Math]::Min(100, $Start.Length)))"
  }
  $endIndex = $Source.Value.IndexOf($candidateEnd, $startIndex)
  if ($endIndex -lt 0) {
    throw "Missing end anchor: $($End.Substring(0, [Math]::Min(100, $End.Length)))"
  }
  $Source.Value = $Source.Value.Substring(0, $startIndex) + $Source.Value.Substring($endIndex)
}

$root = 'E:\IntRuoyi'
$detailPath = Join-Path $root 'IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue'
$detail = Read-Utf8 $detailPath

Remove-Between ([ref] $detail) @'
              <div
                v-if="showPrimaryFormFillMeta"
                class="edhr-batch-detail__primary-fill-meta"
                aria-label="表单填写元信息"
              >
'@ @'
              <div
                v-if="selectedTaskForEvidence && isSpecialNode(selectedTaskForEvidence)"
'@

Remove-Between ([ref] $detail) @'
type PrimaryFormFillMetaItem = {
'@ @'
type TraceRecordFieldResponsibilityEntry = {
'@

Remove-Between ([ref] $detail) @'
const resolvePrimaryFormFillersText = () => {
'@ @'
const selectedSpecialNodeForEvidence = computed(() => {
'@

Remove-Between ([ref] $detail) @'
.edhr-batch-detail__primary-fill-meta {
'@ @'
.edhr-batch-detail__preview-context {
'@

Write-Utf8 $detailPath $detail

$reviewPath = Join-Path $root 'IntRuoyiFronted\tests\e2e\edhr-review-summary-right-rail-static.spec.js'
$review = Read-Utf8 $reviewPath
Replace-Required ([ref] $review) @'
assertIncludes(
  'class="edhr-batch-detail__primary-fill-meta"',
  '填写人和提交时间必须保留一级界面可见的元信息块'
)
'@ @'
assertExcludes(
  'class="edhr-batch-detail__primary-fill-meta"',
  '右侧红框不得继续展示填写人和提交时间元信息块'
)
'@
Replace-Required ([ref] $review) @'
assert.ok(
  rail.includes('class="edhr-batch-detail__primary-fill-meta"') &&
    rail.includes('primaryFormFillMetaItems'),
  '填写人和提交时间必须显示在右侧黄框区域'
)
'@ @'
assert.ok(
  !rail.includes('class="edhr-batch-detail__primary-fill-meta"') &&
    !rail.includes('primaryFormFillMetaItems'),
  '填写人和提交时间不得显示在右侧红框区域'
)
'@
Replace-Required ([ref] $review) @'
assert.ok(
  rail.includes('v-if="showPrimaryFormFillMeta"') &&
    !rail.includes('primaryFormFillMetaItems.length') &&
    detail.includes('const showPrimaryFormFillMeta = computed'),
  '右侧黄框填写元信息必须用脚本层布尔 computed 控制显示，避免模板读取 undefined.length'
)
'@ @'
assert.ok(
  !rail.includes('v-if="showPrimaryFormFillMeta"') &&
    !detail.includes('const showPrimaryFormFillMeta = computed'),
  '右侧红框填写元信息控制逻辑不得保留'
)
'@
Replace-Required ([ref] $review) @'
assertIncludes(
  '.edhr-batch-detail__primary-fill-value',
  '一级填写元信息必须有紧凑值样式，避免长姓名或时间撑破右侧操作栏'
)
'@ @'
assertExcludes(
  '.edhr-batch-detail__primary-fill-value',
  '右侧红框填写元信息样式不得保留'
)
'@
Write-Utf8 $reviewPath $review

$signoffPath = Join-Path $root 'IntRuoyiFronted\tests\e2e\mes-edhr-batch-review-signoff-summary-static.spec.js'
$signoff = Read-Utf8 $signoffPath
Replace-Required ([ref] $signoff) @'
assert(pageSource.includes('primaryFormFillMetaItems'), '页面必须聚合填写元信息')
assert(railSource.includes('primaryFormFillMetaItems'), '填写元信息必须放在右侧黄框一级区域')
assert(
  !topPreviewSource.includes('class="edhr-batch-detail__primary-fill-meta"'),
  '顶部红框位置不得继续展示填写元信息'
)
'@ @'
assert(!pageSource.includes('primaryFormFillMetaItems'), '页面不得保留右侧红框填写元信息聚合')
assert(!railSource.includes('primaryFormFillMetaItems'), '右侧栏不得保留红框填写元信息')
assert(
  !topPreviewSource.includes('class="edhr-batch-detail__primary-fill-meta"'),
  '顶部红框位置不得继续展示填写元信息'
)
'@
Replace-Required ([ref] $signoff) @'
assert(
  pageSource.includes('resolvePrimaryFormFillersText') && pageSource.includes('resolvePrimaryFormSubmitTimesText'),
  '右侧一级区域必须分别展示所有填写人和表单提交时间'
)
'@ @'
assert(
  !pageSource.includes('resolvePrimaryFormFillersText') && !pageSource.includes('resolvePrimaryFormSubmitTimesText'),
  '右侧独立填写人和提交时间计算逻辑不得保留'
)
'@
Write-Utf8 $signoffPath $signoff

$adminPath = Join-Path $root 'IntRuoyiFronted\tests\e2e\edhr-batch-admin-filler-visibility-static.spec.js'
$admin = Read-Utf8 $adminPath
Replace-Required ([ref] $admin) @'
assert.match(
  detailPage,
  /label:\s*'填写人'/,
  '批记录详情一级界面必须展示填写人。'
)
assert.match(
  rail,
  /primaryFormFillMetaItems/,
  '批记录详情必须在右侧黄框一级区域展示填写人和提交时间。'
)
'@ @'
assert.match(
  rail,
  /class="edhr-batch-detail__rail-process-form-filler"[\s\S]*resolveTaskCardFillersText\(task\)/,
  '批记录详情必须在右侧单据卡片内展示填写人。'
)
assert.doesNotMatch(
  rail,
  /primaryFormFillMetaItems/,
  '批记录详情不得保留右侧独立填写人和提交时间红框。'
)
'@
Replace-Required ([ref] $admin) @'
assert.match(
  detailPage,
  /const resolvePrimaryFormFillersText = \(\) =>[\s\S]*resolvePendingTaskFillableUsersText\(selectedTask\)/,
  '一级填写元信息必须优先展示任务填写人，并兜底展示当前工序应填写人员。'
)
'@ @'
assert.doesNotMatch(
  detailPage,
  /const resolvePrimaryFormFillersText = \(\)/,
  '批记录详情不得保留右侧独立填写元信息计算。'
)
'@
Write-Utf8 $adminPath $admin

$directPath = Join-Path $root 'IntRuoyiFronted\tests\e2e\edhr-batch-fill-direct-navigation-static.spec.js'
$direct = Read-Utf8 $directPath
Replace-Required ([ref] $direct) @'
assert.ok(
  rail.includes('primaryFormFillMetaItems') &&
    detailPage.includes("label: '填写人'") &&
    detailPage.includes("label: '提交时间'") &&
    detailPage.includes('resolveTaskGateText'),
  '批次详情右侧一级区域必须使用填写语义，并统一格式化任务阻断原因。'
)
'@ @'
assert.ok(
  rail.includes('edhr-batch-detail__rail-process-form-filler') &&
    rail.includes('resolveTaskCardFillersText(task)') &&
    !rail.includes('primaryFormFillMetaItems') &&
    detailPage.includes('resolveTaskGateText'),
  '批次详情右侧栏必须保留单据卡片填写人和任务阻断原因，并删除独立填写元信息红框。'
)
'@
Write-Utf8 $directPath $direct

$ordinaryPath = Join-Path $root 'IntRuoyiFronted\tests\e2e\edhr-ordinary-process-fill-only-static.spec.js'
$ordinary = Read-Utf8 $ordinaryPath
Replace-Required ([ref] $ordinary) @'
assert.ok(
  pendingDetailPanel.includes('primaryFormFillMetaItems') &&
    pendingDetailPanel.includes('{{ item.label }}'),
  'ordinary pending detail must keep filler information in the right-side first-level area'
)
assert.ok(
  !pendingMainPreview.includes('class="edhr-batch-detail__primary-fill-meta"'),
  'ordinary pending detail must not keep filler information in the top red-box preview area'
)
'@ @'
assert.ok(
  pendingDetailPanel.includes('edhr-batch-detail__rail-process-form-filler') &&
    pendingDetailPanel.includes('resolveTaskCardFillersText(task)'),
  'ordinary pending detail must keep filler information on each right-side form card'
)
assert.ok(
  !pendingDetailPanel.includes('class="edhr-batch-detail__primary-fill-meta"') &&
    !pendingDetailPanel.includes('primaryFormFillMetaItems') &&
    !pendingMainPreview.includes('class="edhr-batch-detail__primary-fill-meta"'),
  'ordinary pending detail must not keep independent filler metadata red-box content'
)
'@
Write-Utf8 $ordinaryPath $ordinary
