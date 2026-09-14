import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(currentDir, '../..')
const executionPage = fs.readFileSync(
  path.resolve(frontendRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)
const editableForm = fs.readFileSync(
  path.resolve(
    frontendRoot,
    'src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
  ),
  'utf8'
)

const executionFormCall = executionPage.match(
  /<EdhrExecutionTemplateEditableForm[\s\S]*?<\/EdhrExecutionTemplateEditableForm>/
)?.[0]

assert.ok(executionFormCall, 'eDHR 执行填写页必须保留原表模式模板组件。')
assert.ok(
  executionFormCall.includes(':show-rule-legend="false"'),
  'eDHR 执行填写页原表模式必须关闭截图红框中的规则类型图例。'
)
assert.ok(
  executionFormCall.includes('class="edhr-fill-workspace__form"'),
  '关闭图例后必须保留原表模式表格容器。'
)
assert.ok(
  executionFormCall.includes('<template #field="{ context }">'),
  '关闭图例后必须保留原表模式字段填写插槽。'
)

for (const token of [
  'showRuleLegend?: boolean',
  'showRuleLegend: true',
  'v-if="props.showRuleLegend"',
  'edhr-template-editable-form__rule-type-badge'
]) {
  assert.ok(editableForm.includes(token), `共享模板组件必须保留图例和单元格角标能力：${token}`)
}

console.log('PASS: eDHR original form hides the rule legend only on the execution page')
