const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const dialog = fs
  .readFileSync(
    path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const includes = (token, message) => assert.ok(dialog.includes(token), message)
includes("activeConfigMode === 'source' && !hasAssistFillConfiguration", '单表入口仅在无辅助分配时显示')
includes('assistRows.value.length > 0 || loadedFillAssignmentCount.value > 0', '正式辅助行和权限分配均应阻止单表入口')
includes('applyWholeFormFillRule(permission.fillRule)', '读取正式整表权限配置')
includes('v-model="wholeFormFillRule.candidateSourceIds"\n                  multiple', '整表填写人支持多选')
includes('@change="wholeFormFillRule.candidateSourceIds = []"', '来源切换清空旧来源ID，避免身份混用')
includes('wholeFormFillRule: wholeFormFillRuleBusinessFields(wholeFormFillRule.value)', '整表业务配置必须纳入未保存检查')
includes('savedWholeFormFillRuleSignature.value = buildWholeFormFillRuleSignature(wholeFormFillRule.value)', '加载和保存响应时重置整表权限基线')
includes('fillRule: wholeFormFillRuleForSave', '整表配置使用正式fillRule写入')
includes('signatureCellMarkers: buildSignatureMarkersForSave(rules)', '保存保留原单元格签名配置链路')

const normalizeBody = dialog.match(
  /const normalizedWholeFormFillRuleForSave = \(\): EdhrProcessFormCandidateRule \| null => \{([\s\S]*?)\n\}/
)
assert.ok(normalizeBody, '必须存在独立整表保存校验')
const normalizeIds = (ids) => [...new Set(ids.map(Number).filter((id) => Number.isFinite(id) && id > 0))]
const businessExpression = dialog.match(
  /const wholeFormFillRuleBusinessFields = \(rule: EdhrProcessFormCandidateRule \| null\) =>\n([\s\S]*?)\n\nconst buildWholeFormFillRuleSignature/
)
assert.ok(businessExpression, '必须提取业务字段作为整表权限基线')
const businessFields = new Function('rule', 'normalizeAssignmentIds', `return ${businessExpression[1]}`)
const signature = (rule) => JSON.stringify(businessFields(rule, normalizeIds))
const normalize = (rule, hasAssist = false, baseline = null) =>
  new Function('hasAssistFillConfiguration', 'wholeFormFillRule', 'normalizeAssignmentIds', 'buildWholeFormFillRuleSignature', 'savedWholeFormFillRuleSignature', normalizeBody[1])(
    { value: hasAssist },
    { value: rule },
    normalizeIds,
    signature,
    { value: signature(baseline) }
  )

assert.equal(normalize(null), null, '未配置且未操作时不创建默认整表权限')
const original = {
  candidateSourceType: 'ROLE',
  candidateSourceIds: [910415],
  completionPolicy: 'ALL',
  dueMinutes: 30,
  enabled: false,
  remark: '正式填写责任'
}
assert.deepEqual(normalize(original), original, '原角色、多完成条件、启用状态、时限与备注全部保留')
assert.equal(normalize(original, false, original), null, '已配置未修改时保存单元格不重写权限')
assert.equal(
  normalize({ ...original, candidateUsers: [{ userId: 101, displayName: '员工' }] }, false, original),
  null,
  '只读候选用户变化不触发权限写入'
)
for (const change of [
  { candidateSourceType: 'USERS', candidateSourceIds: [101, 102] },
  { candidateSourceIds: [910416] },
  { completionPolicy: 'ANY_ONE' },
  { enabled: true }
]) {
  const modified = { ...original, ...change }
  assert.deepEqual(normalize(modified, false, original), modified, '责任来源、候选、完成条件或启用状态变更应写权限')
}
assert.deepEqual(
  normalize({ ...original, candidateSourceType: 'USERS', candidateSourceIds: [101, 102, 101] }),
  { ...original, candidateSourceType: 'USERS', candidateSourceIds: [101, 102] },
  '指定员工多选保留正式ID并去重'
)
assert.equal(normalize(original, true), null, '辅助分配路径不写入整表权限')
assert.throws(() => normalize({ ...original, candidateSourceIds: [] }), /缺少填写人或角色/, '清空责任主体必须明确失败')

const saveStart = dialog.indexOf('const confirmAllRules = async () => {')
const save = dialog.slice(saveStart, dialog.indexOf('\nwatch(', saveStart))
assert.ok(
  save.indexOf('normalizedWholeFormFillRuleForSave()') < save.indexOf('BatchRecordReportApi.saveCellRules({'),
  '责任主体校验必须在单元格写入前完成'
)
assert.ok(
  save.indexOf('normalizedAssistAssignmentsForSave(assistRowsForSave)') < save.indexOf('BatchRecordReportApi.saveCellRules({'),
  '辅助填写人校验也必须在单元格写入前完成'
)
assert.match(save, /if \(hasAssistRowsForSave\) \{[\s\S]*?fillAssignments: fillAssignmentsForSave![\s\S]*?else if \(wholeFormFillRuleForSave\)/, '辅助分配和整表填写权限保持独立保存分支')
console.log('PASS batch-record-whole-form-fill-rule-static')
