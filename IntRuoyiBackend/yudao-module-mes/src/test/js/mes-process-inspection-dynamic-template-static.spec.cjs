const fs = require('fs')
const path = require('path')
const assert = require('assert')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const dynamicFormPort = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImpl.java')
const writer = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java')

assert.doesNotMatch(dynamicFormPort, /TEMPLATE_ID\s*=\s*28L/, '过程检验动态模板解析不得固定 templateId=28。')
assert.doesNotMatch(dynamicFormPort, /template\s+28/i, '过程检验动态模板阻断文案不得误导为 template 28。')
assert.match(dynamicFormPort, /Objects\.equals\(binding\.getFormTemplateId\(\),\s*template\.getTemplateId\(\)\)/,
  '过程检验动态模板必须校验路线绑定模板 ID 与已发布模板版本身份一致。')

assert.doesNotMatch(writer, /PROCESS_INSPECTION_FORM_TEMPLATE_ID\s*=\s*28L/, '过程检验写入编排不得固定 templateId=28。')
assert.doesNotMatch(writer, /PROCESS_INSPECTION_FORM_TEMPLATE_ID\.equals\(binding\.getFormTemplateId\(\)\)/,
  '过程检验写入编排不得只接受固定过程检验模板 ID。')
assert.match(writer, /binding\.getFormTemplateId\(\)\s*!=\s*null/, '过程检验动态绑定必须接受路线绑定里的模板 ID。')
assert.doesNotMatch(writer, /template\s+28/i, '过程检验写入编排阻断文案不得误导为 template 28。')
assert.match(writer, /collectDynamicWriteGroup\(dynamicGroups,\s*batchTask,\s*inspection\)/,
  '多项 PQC 共用过程检验动态表单时，writer 必须先按当前 batchTask/FormCenter instance 聚合。')
assert.match(writer, /for\s*\(DynamicWriteGroup\s+group\s*:\s*dynamicGroups\.values\(\)\)[\s\S]*dynamicFormPort\.write\(toDynamicWriteCommand\(plan,\s*batchExecutionId,\s*group\)\)/,
  '过程检验动态表单必须按聚合后的 group 单次写入并生效，不能逐 PQC task 立即 submit。')
assert.doesNotMatch(writer, /dynamicFormPort\.write\(toDynamicWriteCommand\(plan,\s*batchExecutionId,\s*batchTask,\s*inspection\)\)/,
  '过程检验动态表单不得沿用逐 inspection 写入，否则首项生效会锁死后续检验项目。')
