const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue')
const packageJsonPath = path.join(repoRoot, 'package.json')

const detailPage = fs.readFileSync(detailPagePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const extractBetween = (source, startToken, endToken) => {
  const startIndex = source.indexOf(startToken)
  const endIndex = source.indexOf(endToken, startIndex + startToken.length)
  assert(startIndex >= 0 && endIndex > startIndex, `无法提取 ${startToken} 到 ${endToken} 内容`)
  return source.slice(startIndex, endIndex)
}

const signatureSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-signature-section"',
  '</ContentWrap>'
)

assert(
  packageJson.scripts['e2e:dcc:detail-signature-view-mode:static'] ===
    'node tests/e2e/dcc-detail-signature-view-mode-static.spec.js',
  'package.json 必须提供 e2e:dcc:detail-signature-view-mode:static 脚本'
)

assert(
  detailPage.includes("const signatureEvidenceViewMode = ref<'common' | 'advanced'>('common')"),
  '详情页签名留痕必须默认使用 common 常用视图'
)
assert(
  signatureSection.includes('data-testid="dcc-detail-signature-view-mode"'),
  '签名留痕必须提供常用/高级视图切换控件'
)
assert(signatureSection.includes('常用视图'), '签名留痕视图切换必须包含“常用视图”')
assert(signatureSection.includes('高级视图'), '签名留痕视图切换必须包含“高级视图”')

const commonColumns = ['版本', '签名人', '部门/岗位', '角色', '动作', '签名含义', '签名目的', '受控副本', '证据状态', '签名意见', '签名时间']
for (const label of commonColumns) {
  assert(signatureSection.includes(`label="${label}"`), `常用视图必须保留业务列：${label}`)
}

const advancedColumns = ['权限依据', '签名方式', '源文件 hash', '副本 hash', '证据 hash']
for (const label of advancedColumns) {
  const labelIndex = signatureSection.indexOf(`label="${label}"`)
  assert(labelIndex >= 0, `高级视图必须保留证据列：${label}`)
  const columnStart = signatureSection.lastIndexOf('<el-table-column', labelIndex)
  const columnEnd = signatureSection.indexOf('</el-table-column>', labelIndex)
  const columnSource = signatureSection.slice(columnStart, columnEnd)
  assert(
    /v-if="[^"]*signatureEvidenceViewMode === 'advanced'[^"]*"/.test(columnSource),
    `证据列必须仅在高级视图显示：${label}`
  )
}

const fdaSnapshotFields = [
  'actorUsernameSnapshot',
  'actorDeptNameSnapshot',
  'actorPostNamesSnapshot',
  'actorRoleNamesSnapshot',
  'signaturePurpose',
  'authorizationBasis',
  'formatSignatureSnapshotValue',
  '旧版证据未记录',
  'signature-snapshot-muted'
]

for (const field of fdaSnapshotFields) {
  assert(signatureSection.includes(field) || detailPage.includes(field), `签名留痕必须展示 FDA 快照字段或旧证据提示：${field}`)
}

assert(
  detailPage.includes('fileDetail?.signatureSummaries') ||
    (detailPage.includes('dccSignatureEvidenceList') &&
      detailPage.includes('getDccElectronicSignaturePage')),
  '详情页签名留痕必须保留签名摘要或正式签名留痕分页数据源'
)

const behaviorHooks = [
  'getSignatureActorSummary',
  'getSignatureActionLabel',
  'getSignatureMeaningLabel',
  'getControlledCopyHashStatusLabel',
  'getSignatureEvidenceStatusLabel',
  'formatSignatureHashShort'
]

for (const hook of behaviorHooks) {
  assert(detailPage.includes(hook), `详情页签名留痕必须保留展示函数：${hook}`)
}

const forbiddenTerms = ['mock', 'placeholder data', 'fallback', '降级', '吞异常']
for (const term of forbiddenTerms) {
  assert(!detailPage.toLowerCase().includes(term.toLowerCase()), `详情页签名视图优化不得引入 ${term}`)
}

console.log('DCC detail signature view mode static contract passed.')
