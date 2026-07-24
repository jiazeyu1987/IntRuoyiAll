const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) throw new Error(message)
}
const assertNotIncludes = (content, expected, message) => {
  if (content.includes(expected)) throw new Error(message)
}

const api = read('src/api/mes/pro/batchrecordreport/index.ts')
const edhrApi = read('src/api/mes/pro/edhr/batchExecution.ts')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const historyPage = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const formListPage = read('src/views/mes/pro/batchrecordformlist/index.vue')

assertIncludes(api, 'BatchRecordReportSignatureCellMarkerVO', '报表 API 必须声明签名单元格 marker 类型')
assertIncludes(api, 'getSignatureCellMarkers', '报表 API 必须提供签名位读取接口')
assertIncludes(api, 'saveSignatureCellMarkers', '报表 API 必须提供签名位保存接口')
assertIncludes(edhrApi, 'signatureCellMarkers', '复盘 formViewModel 类型必须包含签名位 marker')

assertIncludes(readonlyForm, 'edhrSignature', '只读模板组件必须识别模板单元格 edhrSignature 元数据')
assertIncludes(readonlyForm, 'signatureCellMarkers', '只读模板组件必须识别接口返回的 signatureCellMarkers')
assertIncludes(readonlyForm, 'signatureRecords', '只读模板组件必须接收单表签名记录')
assertIncludes(readonlyForm, 'is-signature-cell', '只读模板组件必须给签名单元格明确样式')
assertIncludes(editableForm, '电子签名', '模板内签名格必须提供电子签名入口')
assertIncludes(editableForm, 'signatureAction', '模板内签名格必须用事件触发正式签名动作')
assertNotIncludes(editableForm, '签名人姓名', '模板内签名格不得显示手填签名人姓名输入')
assertNotIncludes(editableForm, 'placeholder="签名时间"', '模板内签名格不得显示手填签名时间选择')
assertIncludes(executionPage, 'edhr-page-shell__signature-cell', '真实执行页签名格必须渲染为电子签名单元格')
assertIncludes(executionPage, 'handleSignatureCellAction(field)', '真实执行页签名格必须通过格内按钮触发现有电子签名链路')
assertIncludes(executionPage, 'resolveSignatureCellActionDisabledReason(field)', '真实执行页签名格必须对不可直签动作给出禁用原因')
assertIncludes(executionPage, "case 'FIELD_CHANGE'", '真实执行页签名格 FIELD_CHANGE 必须复用字段变更签名弹窗')
assertIncludes(executionPage, "case 'FORM_REVIEW'", '真实执行页签名格 FORM_REVIEW 必须复用表单复核签名弹窗')
assertIncludes(executionPage, '签名格必须通过电子签名完成，不支持手动输入。', '真实执行页签名格必须明确禁止手动输入')
assertNotIncludes(executionPage, "v-else-if=\"field.componentKind === 'signature'\"\n                    :model-value", '真实执行页签名格不得继续渲染禁用输入框作为签名值')

assertIncludes(historyPage, ':signature-records="selectedExecution.signatureRecords"', '历史页必须把单表签名记录传入模板组件')
assertNotIncludes(historyPage, 'edhr-batch-history__signature-section', '历史页不得默认把模板外电子签名明细作为主视图')

assertIncludes(formListPage, '签名', '电子批记录表单列表必须提供签名位入口')
assertIncludes(formListPage, "openTemplateAction(selectedReport, 'signature')", '签名位入口必须带选中报表上下文')
assertIncludes(formListPage, 'BatchRecordReportApi.getSignatureCellMarkers', '表单预览必须读取签名位')

console.log('eDHR inline signature cell static checks passed')
