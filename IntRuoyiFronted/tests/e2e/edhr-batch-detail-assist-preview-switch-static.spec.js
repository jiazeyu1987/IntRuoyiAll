const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8').replace(/\r\n/g, '\n')

const extractBlock = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.notEqual(start, -1, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end + endToken.length)
}

const rail = extractBlock(
  detail,
  '<aside class="edhr-batch-detail__review-rail"',
  '</aside>'
)
const assistSwitchBlock = extractBlock(
  rail,
  'edhr-batch-detail__preview-mode-switch',
  '</div>'
)
const assistPreviewBlock = extractBlock(
  detail,
  'edhr-batch-detail__assist-preview',
  '</section>'
)
const script = detail.slice(detail.indexOf('<script setup'), detail.indexOf('</script>'))

assert.ok(rail.includes('<el-switch'), '右侧栏顶部必须新增 Element Plus Switch。')
assert.ok(
  assistSwitchBlock.includes('v-model="detailPreviewAssistMode"'),
  'Switch 必须绑定详情页本地 detailPreviewAssistMode，不得复用填写载体状态。'
)
assert.ok(
  script.includes('const detailPreviewAssistMode = ref(false)'),
  '详情页默认必须保持原表模式。'
)
assert.ok(
  assistSwitchBlock.includes('原表模式') && assistSwitchBlock.includes('辅助模式'),
  'Switch 区域必须同时显示“原表模式/辅助模式”文案。'
)
assert.ok(
  assistSwitchBlock.includes('未配置辅助模式') &&
    assistSwitchBlock.includes(':disabled="!selectedPreviewAssistRowsConfigured"'),
  '无辅助配置时 Switch 必须保留但禁用，并提示“未配置辅助模式”。'
)
assert.ok(
  !assistSwitchBlock.includes('selectFillCarrier') &&
    !assistSwitchBlock.includes('openPendingTaskByFillCarrier') &&
    !assistSwitchBlock.includes('handleSelectedPendingTaskAction'),
  '辅助模式 Switch 只能控制中间预览，不得改变右侧卡片查看/打开动作。'
)
assert.ok(
  detail.includes('effectiveDetailPreviewAssistMode') &&
    detail.indexOf('effectiveDetailPreviewAssistMode') < detail.indexOf('<EdhrExecutionReadonlyForm'),
  '中间预览区必须先判断有效辅助模式，再回落到原表只读预览。'
)
assert.ok(
  assistPreviewBlock.includes('selectedPreviewAssistFields') &&
    assistPreviewBlock.includes('字段说明') &&
    assistPreviewBlock.includes('位置') &&
    assistPreviewBlock.includes('当前值') &&
    assistPreviewBlock.includes('必填') &&
    assistPreviewBlock.includes('完成'),
  '辅助模式必须渲染只读字段名、说明、位置、当前值、必填/完成状态。'
)

const forbiddenWriteEntrypoints = [
  'openFieldAuditSignatureDialog',
  'openSubmitDialog',
  'saveEdhrFieldChanges',
  'ProFeedbackApi.submitEdhrExecution',
  'prepareEdhrAttachmentUpload',
  '<el-upload',
  '@click='
]
for (const token of forbiddenWriteEntrypoints) {
  assert.ok(
    !assistPreviewBlock.includes(token),
    `详情页辅助模式只读预览不得包含写入或动作入口：${token}`
  )
}

console.log('PASS: eDHR batch detail assist preview switch static contract')
