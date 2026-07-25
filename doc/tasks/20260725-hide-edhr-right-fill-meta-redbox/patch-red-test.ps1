$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$path = 'E:\IntRuoyi\IntRuoyiFronted\tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js'
$source = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)

function Replace-Required {
  param(
    [string] $Old,
    [string] $New
  )
  if (-not $script:source.Contains($Old)) {
    throw "Missing expected token: $($Old.Substring(0, [Math]::Min(80, $Old.Length)))"
  }
  $script:source = $script:source.Replace($Old, $New)
}

Replace-Required @'
test('eDHR batch detail moves red-box metadata to right-side first-level rail', () => {
'@ @'
test('eDHR batch detail hides right-side red-box fill metadata', () => {
'@

Replace-Required @'
  assert.match(rail, /detail\?\.batchExecutionCode/)
  assert.match(rail, /class="edhr-batch-detail__rail-process-form-action"/)
'@ @'
  assert.match(rail, /detail\?\.batchExecutionCode/)
  assert.match(rail, /class="edhr-batch-detail__rail-process-form-filler"/)
  assert.match(rail, /resolveTaskCardFillersText\(task\)/)
  assert.match(rail, /class="edhr-batch-detail__rail-process-form-action"/)
'@

Replace-Required @'
  assert.match(rail, /class="edhr-batch-detail__primary-fill-meta"/)
  assert.match(rail, /primaryFormFillMetaItems/)
  assert.match(rail, /v-if="showPrimaryFormFillMeta"/)
'@ @'
  assert.doesNotMatch(rail, /class="edhr-batch-detail__primary-fill-meta"/)
  assert.doesNotMatch(rail, /primaryFormFillMetaItems/)
  assert.doesNotMatch(rail, /v-if="showPrimaryFormFillMeta"/)
'@

Replace-Required @'
  assert.match(detail, /primaryFormFillMetaItems/)
  assert.match(detail, /const showPrimaryFormFillMeta = computed/)
  assert.match(detail, /label:\s*'填写人'/)
  assert.match(detail, /label:\s*'提交时间'/)
'@ @'
  assert.doesNotMatch(detail, /primaryFormFillMetaItems/)
  assert.doesNotMatch(detail, /const showPrimaryFormFillMeta = computed/)
  assert.doesNotMatch(detail, /type PrimaryFormFillMetaItem/)
  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-label"/)
  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-value"/)
'@

[System.IO.File]::WriteAllText($path, $source, $utf8NoBom)
