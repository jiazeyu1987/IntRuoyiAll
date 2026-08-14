const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPagePath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/detail/index.vue'
)
const detailPage = fs.readFileSync(detailPagePath, 'utf8').replace(/\r\n/g, '\n')

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const viewerModeTemplate = extractBetween(
  detailPage,
  '<ContentWrap v-if="viewerMode">',
  '<template v-else>',
  'controlled browser viewer template'
)

assert.match(
  viewerModeTemplate,
  /data-testid="dcc-detail-controlled-browser-linkage"/,
  'viewer mode must render the controlled browser linkage evidence section'
)

for (const label of ['受控浏览入口', '最终目录路径', 'publishedFileId', 'stampedFileId', 'master 当前生效版本']) {
  assert.match(
    viewerModeTemplate,
    new RegExp(label),
    `viewer mode controlled browser linkage must show ${label}`
  )
}

for (const token of [
  'openControlledBrowserLocation',
  'controlledBrowserDirectoryPath',
  'fileDetail?.publishedFileId',
  'fileDetail?.stampedFileId',
  'fileDetail?.currentActiveVersionNo'
]) {
  assert.match(
    viewerModeTemplate,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `viewer mode controlled browser linkage must use ${token}`
  )
}

assert.doesNotMatch(
  viewerModeTemplate,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'viewer linkage must not rely on mock, fallback, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC controlled browser viewer linkage static contract')
