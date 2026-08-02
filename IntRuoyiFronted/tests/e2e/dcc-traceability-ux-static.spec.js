const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required frontend file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')
const logsPage = readSource('src/views/dcc/controlled-file/logs/index.vue')

const signatureTraceSection = detailPage.slice(
  detailPage.indexOf('data-testid="dcc-detail-signature-trace-section"'),
  detailPage.indexOf('<ContentWrap data-testid="dcc-detail-signature-section">')
)
assert.ok(signatureTraceSection.length > 0, '签核追溯区必须存在')

assert.match(
  detailPage,
  /当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。/,
  '签名留痕权限提示必须业务化，说明摘要仍可查看且高级留痕需要额外权限'
)
assert.doesNotMatch(
  detailPage,
  /签名留痕无法加载；审批任务加载不受影响/,
  '签名留痕权限提示不得让用户误判主追溯证据缺失'
)

assert.match(
  logsPage,
  /controlledFileLogEmptyText/,
  '文控操作日志空态必须按目标文件上下文动态说明'
)
assert.match(
  logsPage,
  /暂无操作日志，签核证据请见签核追溯\/生命周期/,
  '目标文件无操作日志时必须指引签核追溯/生命周期'
)
assert.match(
  logsPage,
  /:empty-text="controlledFileLogEmptyText"/,
  '操作日志表格必须绑定上下文空态文案'
)

assert.match(signatureTraceSection, /label="审批意见"/, '签核追溯表必须合并展示审批意见')
assert.match(signatureTraceSection, /prop="approvalCommentText"/, '签核追溯表必须使用审批意见字段')
assert.match(detailPage, /approvalCommentText:\s*formatSignatureTraceComment/, '签核追溯行必须从签名意见生成审批意见展示值')
assert.match(detailPage, /'审批意见'/, '签核追溯导出/打印必须包含审批意见字段')

assert.match(signatureTraceSection, /label="文件证据"/, '签核追溯表必须展示文件证据操作列')
assert.match(signatureTraceSection, /data-testid="dcc-signature-trace-file-evidence"/, '文件证据入口必须有稳定测试标识')
assert.match(detailPage, /openTraceFileEvidence/, '发布/盖章文件证据必须提供可点击查看动作')
assert.match(detailPage, /查看盖章\/发布文件/, '发布/盖章文件证据按钮必须用业务化文案')
assert.match(detailPage, /publishedFileId/, '签核追溯文件证据必须保留 publishedFileId 可见性')
assert.match(detailPage, /stampedFileId/, '签核追溯文件证据必须保留 stampedFileId 可见性')

for (const expectedText of [
  '处理建议：请重新输入当前账号密码；如仍失败，请联系文控或系统管理员确认账号状态。',
  '处理建议：请联系文控负责人开通当前节点电子签名授权，或由流程管理员确认审批候选人配置。',
  '责任入口：DCC 电子签名管理 / 文控负责人'
]) {
  assert.ok(
    approvalActions.includes(expectedText) || detailPage.includes(expectedText),
    `签名失败诊断必须包含业务处理建议：${expectedText}`
  )
}

assert.doesNotMatch(
  [signatureTraceSection, logsPage].join('\n'),
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  '本次追溯 UX 修复不得引入 mock、placeholder、fallback、降级、吞异常或默认成功'
)

console.log('PASS: DCC traceability UX static contract')
