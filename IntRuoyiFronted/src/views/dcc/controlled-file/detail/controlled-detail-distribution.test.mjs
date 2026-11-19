import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import { parse, compileScript, compileTemplate, compileStyle } from 'vue/compiler-sfc'

const loadHelper = async filename => {
  const source = readFileSync(new URL(filename, import.meta.url), 'utf8')
  const compiled = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } })
  return import(`data:text/javascript;base64,${Buffer.from(compiled.outputText).toString('base64')}`)
}
const { buildControlledDetailDistributionRows: build, canReadControlledDistributionReceipts: canReadReceipts,
  getControlledDetailTrainingAttachments: trainingAttachments } = await loadHelper('./controlled-detail-distribution.ts')
assert.equal(canReadReceipts({ canEditFile: true, processInstanceId: 'round-1' }, true), true)
for (const file of [undefined, {}, { canEditFile: false, processInstanceId: 'round-1' }, { canEditFile: true }]) {
  assert.equal(canReadReceipts(file, true), false, '菜单授权不能代替当前版本的下发读取资格')
}
assert.equal(canReadReceipts({ canEditFile: true, processInstanceId: 'round-1' }, false), false)
const { getControlledDetailFormalVersions: formalVersions, getControlledDetailPredecessorFact: predecessorFact,
  getControlledDetailTimeEvents: timeEvents } = await loadHelper('./controlled-detail-version.ts')
const { getControlledDetailPersonText: personText } = await loadHelper('./controlled-detail-people.ts')
const file = { id: 1, versionNo: 'C/1', distributionStatuses: [{
  id: 10, departmentId: 100, distributionMedium: 'PUBLIC_FOLDER', status: 'PENDING', recipientUserIds: [7],
  recipients: [{ id: 20, userId: 7, readAt: 1, acknowledgedAt: null }]
}] }
const receipt = { controlledFileId: 1, versionNo: 'C/1', distributionId: 10, recipientId: 20,
  departmentId: 100, userId: 7, messageStatus: 'SENT', readAt: 2, acknowledgedAt: 3,
  signatureId: 30, signedAt: 3, signerNameSnapshot: '正式签收人' }
assert.equal(build(file, [receipt]).length, 1)
assert.equal(build(file, [receipt])[0].acknowledgedAt, 3)
assert.equal(build(file, [receipt])[0].readAt, 2)
assert.equal(build(file, [receipt])[0].status, 'ACKNOWLEDGED')
assert.equal(file.distributionStatuses[0].status, 'PENDING', '原分发摘要必须独立保留')
for (const changed of [{ recipientId: 21 }, { distributionId: 11 }, { userId: 8 }, { departmentId: 101 }]) {
  const rows = build(file, [{ ...receipt, ...changed }])
  assert.equal(rows.length, 2, '无法精确关联的回执应独立保留')
  assert.equal(rows[0].acknowledgedAt, null)
  assert.equal(rows[1].acknowledgedAt, 3)
}
const actual = structuredClone(file)
actual.distributionStatuses[0].recipients[0].acknowledgedAt = 5
assert.equal(build(actual)[0].acknowledgedAt, 5, '详情接口的逐人实际回执可直接展示')
assert.equal(build({ distributionStatuses: [] }, [receipt]).length, 1)
const unassigned = structuredClone(file)
unassigned.distributionStatuses[0].recipients = []
assert.equal(build(unassigned, [receipt]).length, 2, '仅有相同人员不足以归并签收')

const history = [
  { id: 1, versionNo: 'A/1', status: 'OBSOLETE', controlledTime: '2026-11-01 10:00:00' },
  { id: 2, versionNo: 'A/2', status: 'SUPERSEDED', publishedTime: 1 },
  { id: 3, versionNo: 'A/3', status: 'REJECTED', revisionAttemptNo: 1 },
  { id: 4, versionNo: 'A/3', status: 'WITHDRAWN', revisionAttemptNo: 2 },
  { id: 5, versionNo: 'A/3', status: 'FINALIZATION_FAILED', revisionAttemptNo: 3 },
  { id: 6, versionNo: 'A/3-1', status: 'WORKING' },
  { id: 7, versionNo: 'A/3-2', status: 'SUPERSEDED', supersededByFileId: 8 },
  { id: 8, versionNo: 'A/3', status: 'ACTIVE', controlledTime: '2026-11-19 10:00:00' },
  { id: 9, versionNo: 'A/4', status: 'CONTROLLED_PENDING_EFFECTIVE', controlledTime: '2026-11-19 11:00:00' },
  { id: 10, versionNo: 'A/4', status: 'PENDING_APPLICANT_REWORK' },
  { id: 11, versionNo: 'A/5', status: 'IN_APPROVAL' }
]
assert.deepEqual(formalVersions(history).map(row => row.id), [1, 2, 8, 9], '只列受控/发布过的正式版本；SUPERSEDED 草稿与多次失败 A/3 不冒充正式版本')
assert.equal(formalVersions(history).filter(row => row.versionNo === 'A/3').length, 1)
assert.equal(history.length, 11, '筛选不得修改原始申请历史')
assert.deepEqual(formalVersions(), [])
assert.deepEqual(formalVersions([{ id: 12, versionNo: 'A/3', status: 'SUPERSEDED' }]), [], '仅状态与版本文本不足以证明已正式受控')
assert.deepEqual(formalVersions([{ id: 13, versionNo: 'A/3', status: 'REJECTED', publishedTime: 1 }]), [], '失败状态不能因时间字段而冒充正式版本')

const current = { id: 30, status: 'ACTIVE', versionNo: 'A/7', revisionChangeType: 'PARTIAL', revisionSourceControlledFileId: 29 }
const obsoletePredecessor = { id: 29, versionNo: 'A/6', status: 'OBSOLETE', controlledTime: '2026-11-01 10:00:00',
  obsoletedTime: 1, supersededByFileId: 30 }
assert.equal(predecessorFact(current).kind, 'pending', '明确有前版但作废记录缺失须待核验')
assert.equal(predecessorFact({ ...current, versionHistory: [obsoletePredecessor] }).kind, 'obsolete')
assert.equal(predecessorFact({ ...current, versionHistory: [{ ...obsoletePredecessor, obsoletedTime: undefined }] }).kind, 'pending', '仅作废状态不能证明已记录作废')
assert.equal(predecessorFact({ ...current, versionHistory: [{ ...obsoletePredecessor, supersededByFileId: 31 }] }).kind, 'pending', '其他版本产生的作废不能冒充本次作废')
assert.equal(predecessorFact({ ...current, versionHistory: [{ ...obsoletePredecessor, status: 'SUPERSEDED' }] }).kind, 'pending')
assert.equal(predecessorFact({ id: 30, status: 'ACTIVE', versionNo: 'A/1' }).kind, 'unknown', '不凭状态、版本号或空历史推断首次生效')
assert.equal(predecessorFact({ id: 30, revisionChangeType: 'INITIAL', effectiveTime: '2026-11-01' }).kind, 'initial')
assert.equal(predecessorFact({ id: 30, revisionChangeType: 'INITIAL', predecessorControlledFileId: 28,
  versionHistory: [{ id: 28, status: 'SUPERSEDED', versionNo: 'A/1-1' }] }).kind, 'initial', '初始申请选用的草稿不是前版受控文件')
assert.equal(predecessorFact({ id: 30, predecessorControlledFileId: 29,
  versionHistory: [{ ...obsoletePredecessor, supersededByFileId: undefined }] }).kind, 'pending', '前驱需匹配真实正式版本，才能作为前版证据')
assert.equal(predecessorFact({ id: 30, revisionChangeType: 'INITIAL', revisionSourceControlledFileId: 29 }).kind, 'pending', '有明确前版不能覆盖为初始版本')
assert.deepEqual(timeEvents({ controlledTime: 'time-1', activatedTime: 'time-1', publishedTime: 2, distributedTime: 'time-3' }, String), [
  { time: 'time-1', labels: ['受控', '启用'] }, { time: '2', labels: ['发布'] }, { time: 'time-3', labels: ['下发'] }
], '仅同时间事件合并，分别保留不同时间与对应事项')
assert.deepEqual(timeEvents({}, String), [])
assert.equal(personText('DCC241 (DCC241)'), 'DCC241')
assert.equal(personText(' 张三 (zhangsan) '), '张三 (zhangsan)')
assert.equal(personText('zhangsan'), 'zhangsan')
assert.equal(personText(undefined), '')
const attachmentFile = { id: 30, attachments: [
  { attachmentId: 'exact-training', controlledFileId: '30', attachmentPurpose: 'TRAINING_RECORD' },
  { attachmentId: 'other-version', controlledFileId: 29, attachmentPurpose: 'TRAINING_RECORD' },
  { attachmentId: 'ordinary', controlledFileId: 30, attachmentPurpose: 'ORDINARY' }
] }
assert.deepEqual(trainingAttachments(attachmentFile).map(row => row.attachmentId), ['exact-training'], '培训链接只能定位本版本真实培训附件')
assert.deepEqual(trainingAttachments({ id: 30 }), [])

let fixedTableCount = 0
for (const filename of ['ControlledDetailVersionPanel.vue', 'ControlledDetailDistributionPanel.vue']) {
  const { descriptor, errors } = parse(readFileSync(new URL(filename, import.meta.url), 'utf8'), { filename })
  assert.equal(errors.length, 0)
  const script = compileScript(descriptor, { id: filename })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: filename,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.equal(template.errors.length, 0, template.errors.join('\n'))
  const visit = node => {
    if (node.tag === 'el-table') {
      fixedTableCount++
      assert.ok(node.props.some(prop => prop.type === 6 && prop.name === 'data-user-table-column-explicit'), `${filename} 的固定表格必须声明正式字段标记`)
    }
    for (const child of node.children || []) visit(child)
  }
  visit(descriptor.template.ast)
}
assert.equal(fixedTableCount, 4, '版本历史、分发接收、原摘要、培训四张表均需验证')
const styles = compileStyle({ source: readFileSync(new URL('./controlled-detail-panels.css', import.meta.url), 'utf8'), id: 'direction-two', scoped: true })
assert.equal(styles.errors.length, 0)
console.log('PASS: 正式版本、实际旧版证据、初始/前版缺失区分、时间合并、姓名账号去重、真实培训附件、签收关联、四表标记、SFC/CSS 编译')
