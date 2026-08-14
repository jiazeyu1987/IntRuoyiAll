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
const signatureTraceSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-signature-trace-section"',
  'data-testid="dcc-detail-signature-section"'
)

assert(
  packageJson.scripts['e2e:dcc:detail-signature-view-mode:static'] ===
    'node tests/e2e/dcc-detail-signature-view-mode-static.spec.js',
  'package.json 必须提供 e2e:dcc:detail-signature-view-mode:static 脚本'
)

assert(!detailPage.includes('signatureEvidenceViewMode'), '签名留痕视图切换状态必须随黄框删除')
assert(
  !signatureSection.includes('data-testid="dcc-detail-signature-view-mode"'),
  '签名留痕黄框内常用/高级视图切换控件必须删除'
)
assert(!signatureSection.includes('常用视图'), '签名留痕黄框内“常用视图”按钮必须删除')
assert(!signatureSection.includes('高级视图'), '签名留痕黄框内“高级视图”按钮必须删除')
assert(
  !signatureSection.includes('常用视图默认显示签名业务字段'),
  '签名留痕黄框内常用视图说明必须删除'
)
assert(
  !signatureSection.includes('高级视图显示签名方式和 Hash 证据'),
  '签名留痕黄框内高级视图说明必须删除'
)
assert(!signatureSection.includes('<TableQuickFilter'), '签名留痕黄框内快速过滤必须删除')
assert(!signatureSection.includes('<UserTableColumnSettings'), '签名留痕黄框内显示字段必须删除')
assert(!detailPage.includes('dccSignatureEvidenceQuickFilter'), '签名留痕快速过滤脚本状态必须删除')
assert(
  !detailPage.includes('signatureEvidenceViewModeOptions'),
  '签名留痕常用/高级视图切换选项必须删除'
)

assert(
  signatureTraceSection.includes(':show-query-form="false"'),
  '签核追溯标准列表必须关闭顶部查询/工具栏，删除黄框内标题、导出/打印和重置列'
)
assert(!signatureTraceSection.includes('汇总上传人'), '签核追溯黄框内说明必须删除')
assert(!signatureTraceSection.includes('exportSignatureTrace'), '签核追溯黄框内导出入口必须删除')
assert(!signatureTraceSection.includes('printSignatureTrace'), '签核追溯黄框内打印入口必须删除')
assert(!signatureTraceSection.includes('show-column-reset'), '签核追溯黄框内重置列入口必须删除')
assert(!detailPage.includes('const exportSignatureTrace ='), '签核追溯导出脚本必须随入口删除')
assert(!detailPage.includes('const printSignatureTrace ='), '签核追溯打印脚本必须随入口删除')

for (const label of ['角色', '上传人 / 四级审批人', '审批意见', '签名时间', '签名方式', '证据状态', '文件哈希', '盖章文件 / 发布文件证据']) {
  assert(
    signatureTraceSection.includes(`label="${label}"`),
    `签核追溯表格必须保留正式列：${label}`
  )
}
assert(
  signatureTraceSection.includes(':data="pagedSignatureTraceRows"'),
  '签核追溯表格必须继续使用正式分页数据'
)

const commonColumns = ['版本', '签名人', '部门/岗位', '角色', '动作', '签名含义', '签名目的', '受控副本', '证据状态', '签名意见', '签名时间']
for (const label of commonColumns) {
  assert(signatureSection.includes(`label="${label}"`), `常用视图必须保留业务列：${label}`)
}

const advancedColumns = ['权限依据', '签名方式', '源文件 hash', '副本 hash', '证据 hash']
for (const label of advancedColumns) {
  const labelIndex = signatureSection.indexOf(`label="${label}"`)
  assert(labelIndex >= 0, `删除高级视图切换后必须直接保留证据列：${label}`)
  const columnStart = signatureSection.lastIndexOf('<el-table-column', labelIndex)
  const columnEnd = signatureSection.indexOf('</el-table-column>', labelIndex)
  const columnSource = signatureSection.slice(columnStart, columnEnd)
  assert(
    !/signatureEvidenceViewMode/.test(columnSource),
    `证据列不得继续依赖已删除的高级视图切换：${label}`
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
