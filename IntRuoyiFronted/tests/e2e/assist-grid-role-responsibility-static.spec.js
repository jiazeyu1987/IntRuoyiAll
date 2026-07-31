const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const listPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const permissionApi = read('src/api/mes/pro/edhr/processFormPermissionRule.ts')

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

includes(permissionApi, 'candidateSourceNames?: string[]', '权限规则响应类型必须携带角色/个人责任主体名称。')
includes(dialog, 'getSimpleRoleList', '辅助映射配置必须读取角色候选项。')
includes(dialog, 'pendingAssistSubjectType', '辅助映射配置必须允许选择个人或角色责任主体类型。')
includes(dialog, 'assistResponsibilitySubjects', '辅助映射配置必须维护责任主体集合，而不是只维护填写人用户集合。')
includes(dialog, 'ASSIST_GRID_(USERS|ROLE)', '辅助映射 rowKey 必须能表达个人或角色责任主体。')
includes(dialog, 'candidateSourceType: selectedAssistSubject.value.candidateSourceType', '保存辅助格时必须使用当前选择的责任主体类型。')
includes(dialog, 'candidateSourceIds: [...selectedAssistSubject.value.candidateSourceIds]', '保存辅助格时必须使用当前选择的责任主体 ID。')
notIncludes(dialog, "candidateSourceType: 'USERS' as const,\n      candidateSourceIds: [userId]", '保存 payload 不能从 rowKey 反推为固定个人。')
notIncludes(dialog, "candidateSourceType: parsed\n        ? 'USERS'", '读取已有辅助分配时不能因 rowKey 形态覆盖后端持久化的责任主体类型。')
notIncludes(dialog, 'const candidateSourceIds = parsed\n      ? [parsed.userId]', '读取已有辅助分配时不能优先使用 rowKey 里的用户 ID。')
includes(listPage, 'fillAssignments', '批记录表单列表必须读取辅助模式 fillAssignments。')
includes(listPage, 'buildFillAssignmentSummaryText', '批记录表单列表必须使用辅助分配汇总文案。')
includes(listPage, 'rule.fillAssignments?.length', '旧单一填写人入口必须识别辅助分配，避免覆盖辅助模式。')
includes(listPage, 'openCellRulesDialog(row)', '辅助模式下点击填写入口必须进入辅助映射配置。')

console.log('PASS: assist grid role responsibility static contract')
