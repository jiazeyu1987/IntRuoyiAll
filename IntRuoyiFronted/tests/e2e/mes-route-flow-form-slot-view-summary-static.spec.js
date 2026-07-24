const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

assert.ok(
  component.includes('data-form-slot-view-summary="true"'),
  '查看模式表单槽位必须渲染独立摘要区域，不能只显示字段值文本。'
)
assert.match(
  component,
  /v-if="selectedProcessDetailField\.key === 'formSlots'"[\s\S]*data-form-slot-view-summary="true"[\s\S]*v-for="item in buildFormSlotViewSummaryItems\(\)"/,
  '表单槽位查看摘要必须只在 formSlots 字段下按每个槽位渲染。'
)
assert.match(
  component,
  /const buildFormSlotViewSummaryItems = \(\) => \{[\s\S]*selectedRecordBindings\.value[\s\S]*getFormBindingDisplayName[\s\S]*formatRecordBindingFillerSummary[\s\S]*formatRecordBindingProcessIndependentSummary/,
  '查看摘要必须由当前工序的每个动态表单槽位构建，包含表单名、填写人和工序独立状态。'
)
assert.match(
  component,
  /const formatRecordBindingFillerSummary = \(binding: RouteFlowRecordBinding\) => \{[\s\S]*candidateSourceNames[\s\S]*candidateSourceIds[\s\S]*'未配置'/,
  '填写人摘要必须优先展示 candidateSourceNames，并在旧数据缺失时显示“未配置”。'
)
assert.match(
  component,
  /const formatRecordBindingProcessIndependentSummary = \(binding: RouteFlowRecordBinding\) =>[\s\S]*isRecordBindingProcessIndependent\(binding\) \? '是' : '否'/,
  '工序独立摘要必须由 instanceScope 派生为“是/否”。'
)

for (const label of ['填写人', '工序独立']) {
  assert.ok(component.includes(label), `查看摘要必须包含中文标签：${label}`)
}

console.log('mes-route-flow-form-slot-view-summary-static PASS')
