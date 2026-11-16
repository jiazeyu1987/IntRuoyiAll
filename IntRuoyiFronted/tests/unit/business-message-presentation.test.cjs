const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/businessMessagePresentation.ts'), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const api = {}
Function('exports', code)(api)
const suffix = '\n【工单：WO-001；工序：整单；发起人：张三（SYSTEM_USER）；状态：PQC已提交，待组长复核】'

test('formal handoff metadata is separated without losing identifiers or original state', () => {
  const result = api.describeHandoffMessage('PQC_REVIEW', '本人PQC检验提交已完成，请组长签名复核' + suffix)
  assert.equal(result.title, 'PQC组长复核')
  assert.equal(result.body, 'PQC检验已提交，请组长签名复核。')
  assert.deepEqual(result.fields, [
    { label: '工单', value: 'WO-001' }, { label: '工序', value: '整单' },
    { label: '发起人', value: '张三' }, { label: '交接时状态', value: 'PQC已提交，待组长复核' }
  ])
})

test('freeze notice keeps restriction, NCR number and full multiline business reason', () => {
  const result = api.describeHandoffMessage('NCR_FREEZE_NOTICE', 'NCR NCR-123 已冻结工单/批次，暂停受影响生产、检验和放行，等待QA正式处置：尺寸异常\n请核查第二批' + suffix)
  assert.equal(result.title, '工单/批次冻结通知')
  assert.equal(result.body, '已暂停相关生产、检验和放行，等待QA正式处置。\n冻结原因：尺寸异常\n请核查第二批')
  assert.deepEqual(result.fields[0], { label: '不合格记录', value: 'NCR-123' })
})

test('arbitrary comments and incomplete metadata remain intact', () => {
  for (const reason of ['首件偏差：2mm\n【请保留所有附件】', '说明【工单：WO-001；工序：整单】', '说明' + suffix + '\n补充意见']) {
    assert.deepEqual(api.splitHandoffReason(reason), { body: reason, fields: [] })
  }
})

test('historical station messages use frozen params, without inferring current task completion', () => {
  const result = api.describeNotifyMessage({ templateCode: 'MES_ACTIVE_ORDER_HANDOFF', readStatus: true,
    templateContent: '原始模板中的内部编号', templateParams: { handoffType: 'NCR_FREEZE_NOTICE', reason: '保持冻结' + suffix } })
  assert.equal(result.title, '工单/批次冻结通知')
  assert.equal(result.body, '保持冻结')
  assert.doesNotMatch(JSON.stringify(result), /已解冻|可以生产|SYSTEM_USER/)
})

test('other message types preserve their original full text', () => {
  const content = '请确认文件A\n版本：B/1\n意见：保留原文'
  assert.deepEqual(api.describeNotifyMessage({ templateCode: 'OTHER', templateContent: content }), { title: '', body: content, fields: [] })
})

test('release task descriptions keep business identifiers outside the action and exclude raw navigation URLs', () => {
  for (const [templateCode, title] of [
    ['MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED', '请完成PQC生产放行'],
    ['MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED', '请完成上市放行审批']
  ]) {
    const result = api.describeNotifyMessage({ templateCode, templateContent: '旧模板：入口 /internal?id=23',
      templateParams: { workOrderCode: 'WO-001', batchCode: 'B-002', processName: '放行', reason: '正式放行任务候选交接', actionUrl: '/internal?id=23' } })
    assert.equal(result.title, title)
    assert.equal(result.body, '')
    assert.deepEqual(result.fields, [{ label: '工单', value: 'WO-001' }, { label: '批次', value: 'B-002' }, { label: '工序', value: '放行' }])
    assert.doesNotMatch(JSON.stringify(result), /internal|候选交接/)
  }
})

test('work task notices preserve rejection reasons and show incomplete input explicitly', () => {
  const templateCode = 'MES_EDHR_REWORK_TASK_ASSIGNED'
  const result = api.describeNotifyMessage({ templateCode, templateParams: { workOrderCode: 'WO', batchCode: 'B', reason: '温度填写错误\n请补充说明' } })
  assert.equal(result.body, '温度填写错误\n请补充说明')
  assert.match(api.describeNotifyMessage({ templateCode, templateContent: '原始正文', templateParams: { workOrderCode: 'WO' } }).title, /缺少工单或批次/)
})

test('missing handoff inputs and unknown types are visible rather than invented actions', () => {
  assert.match(api.describeNotifyMessage({ templateCode: 'MES_ACTIVE_ORDER_HANDOFF', templateContent: '原始正文' }).title, /信息不完整/)
  const result = api.describeHandoffMessage('NEW_KIND', '原始意见')
  assert.match(result.title, /未识别.*NEW_KIND/)
  assert.equal(result.body, '原始意见')
})
