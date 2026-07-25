$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$path = 'E:\IntRuoyi\IntRuoyiFronted\tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js'
$source = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$newline = if ($source.Contains("`r`n")) { "`r`n" } else { "`n" }

$source = $source.Replace(
  "test('eDHR batch detail moves red-box metadata to right-side first-level rail', () => {",
  "test('eDHR batch detail hides right-side red-box fill metadata', () => {"
)

$anchor = "  assert.match(rail, /detail\?\.batchExecutionCode/)$newline"
$insert = $anchor +
  '  assert.match(rail, /class="edhr-batch-detail__rail-process-form-filler"/)' + $newline +
  '  assert.match(rail, /resolveTaskCardFillersText\(task\)/)' + $newline
if (-not $source.Contains($anchor)) {
  throw 'Missing batchExecutionCode assertion anchor.'
}
if (-not $source.Contains('resolveTaskCardFillersText\(task\)')) {
  $source = $source.Replace($anchor, $insert)
}

$oldRail = @(
  '  assert.match(rail, /class="edhr-batch-detail__primary-fill-meta"/)',
  '  assert.match(rail, /primaryFormFillMetaItems/)',
  '  assert.match(rail, /v-if="showPrimaryFormFillMeta"/)'
) -join $newline
$newRail = @(
  '  assert.doesNotMatch(rail, /class="edhr-batch-detail__primary-fill-meta"/)',
  '  assert.doesNotMatch(rail, /primaryFormFillMetaItems/)',
  '  assert.doesNotMatch(rail, /v-if="showPrimaryFormFillMeta"/)'
) -join $newline
if (-not $source.Contains($oldRail)) {
  throw 'Missing rail primary-fill-meta assertion block.'
}
$source = $source.Replace($oldRail, $newRail)

$tailAnchor = '  assert.doesNotMatch(mainPreview, /class="edhr-batch-detail__primary-fill-meta"/)'
$tailIndex = $source.IndexOf($tailAnchor)
if ($tailIndex -lt 0) {
  throw 'Missing mainPreview tail anchor.'
}
$closeIndex = $source.IndexOf("$newline})", $tailIndex)
if ($closeIndex -lt 0) {
  throw 'Missing test close anchor.'
}
$newTail = @(
  $tailAnchor,
  '  assert.doesNotMatch(detail, /primaryFormFillMetaItems/)',
  '  assert.doesNotMatch(detail, /const showPrimaryFormFillMeta = computed/)',
  '  assert.doesNotMatch(detail, /type PrimaryFormFillMetaItem/)',
  '  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-label"/)',
  '  assert.doesNotMatch(detail, /class="edhr-batch-detail__primary-fill-value"/)'
) -join $newline
$source = $source.Substring(0, $tailIndex) + $newTail + $source.Substring($closeIndex)

[System.IO.File]::WriteAllText($path, $source, $utf8NoBom)
