const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.match(
  pageSource,
  /<el-form-item[\s\S]*?label="优先级"[\s\S]*?class="scheduler-workbench__policy-item scheduler-workbench__policy-item--priority"/,
  '优先级必须使用独立策略表单项类，避免标签被默认栅格挤压。'
)

assert.match(
  pageSource,
  /<el-form-item[\s\S]*?label="保护项"[\s\S]*?class="scheduler-workbench__policy-item scheduler-workbench__policy-checks">/,
  '保护项必须使用统一策略表单项类，避免标签与复选框区域重叠。'
)

for (const cssFragment of [
  '.scheduler-workbench__policy-item',
  'grid-template-columns: 128px minmax(0, 1fr);',
  '.scheduler-workbench__policy-item :deep(.el-form-item__label)',
  'white-space: nowrap;',
  'padding-right: 12px;',
  '.scheduler-workbench__policy-checks :deep(.el-form-item__content)',
  'flex-wrap: wrap;'
]) {
  assert.ok(pageSource.includes(cssFragment), `策略区缺少布局约束: ${cssFragment}`)
}

console.log('mes-scheduler-workbench-policy-label-layout-static.spec.js passed')
