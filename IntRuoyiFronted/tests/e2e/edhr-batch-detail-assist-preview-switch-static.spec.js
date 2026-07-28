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
const style = detail.slice(detail.indexOf('<style'), detail.indexOf('</style>'))

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
  /edhr-batch-detail__preview-mode-switch\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*auto\s+auto\s+auto;/.test(
    style
  ),
  'Switch 区域必须使用三列栅格承载“原表模式 / Switch / 辅助模式”，避免禁用提示挤压文案。'
)
assert.ok(
  /edhr-batch-detail__preview-mode-label\s*\{[\s\S]*white-space:\s*nowrap;/.test(style),
  '“原表模式/辅助模式”标签必须禁止换行，避免窄右侧栏中文字被截断。'
)
assert.ok(
  /edhr-batch-detail__preview-mode-disabled\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;[\s\S]*white-space:\s*nowrap;/.test(
    style
  ),
  '“未配置辅助模式”必须占满 Switch 行宽并禁止换行，确保蓝框区域文字完整可见。'
)
assert.ok(
  /edhr-batch-detail__preview-mode-disabled\s*\{[\s\S]*justify-self:\s*stretch;[\s\S]*background:\s*#f8fafc;[\s\S]*color:\s*#475467;[\s\S]*font-weight:\s*600;/.test(
    style
  ),
  '“未配置辅助模式”不能使用过浅禁用灰色，必须以清晰状态条样式完整可读。'
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
