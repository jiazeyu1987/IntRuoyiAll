const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const sourceRoot = path.join(root, 'src')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const walkVueFiles = (dir) => {
  const files = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...walkVueFiles(fullPath))
    } else if (entry.isFile() && fullPath.endsWith('.vue')) {
      files.push(fullPath)
    }
  }
  return files
}

const templateSource = read('src/components/UnifiedListTemplate/index.vue')
const quickFilterHookSource = read('src/hooks/web/useTableQuickFilter.ts')
const scheduleOrderSource = read('src/views/mes/pro/scheduleorder/index.vue')

const standardListBlocks = []
for (const file of walkVueFiles(sourceRoot)) {
  const source = fs.readFileSync(file, 'utf8')
  let index = 0
  while (true) {
    const start = source.indexOf('<UnifiedListTemplate', index)
    if (start === -1) break
    const openEnd = source.indexOf('>', start)
    const close = source.indexOf('</UnifiedListTemplate>', openEnd)
    standardListBlocks.push({
      file: path.relative(sourceRoot, file).replace(/\\/g, '/'),
      openTag: source.slice(start, openEnd + 1),
      block: close === -1 ? source.slice(start, openEnd + 1) : source.slice(start, close)
    })
    index = (close === -1 ? openEnd : close) + 1
  }
}

assert.equal(standardListBlocks.length, 84, '当前系统标准列表模板接入点数量必须先被锁定为 84 个。')

assert.doesNotMatch(
  templateSource,
  /<TableQuickFilter[\s\S]*?<\/TableQuickFilter>/,
  'UnifiedListTemplate 默认筛选 UI 不得继续渲染旧 TableQuickFilter。'
)

assert.match(
  templateSource,
  /<TableMultiFilter[\s\S]*:filter-definitions="resolvedStandardFilterDefinitions"[\s\S]*:state="resolvedStandardFilterState"/,
  'UnifiedListTemplate 必须统一通过 TableMultiFilter 渲染红框条件 Tab。'
)

assert.match(
  templateSource,
  /const shouldRenderStandardConditionFilter = computed\(/,
  'UnifiedListTemplate 必须集中判断标准条件 Tab 是否展示。'
)

assert.match(
  templateSource,
  /const quickDefinitionsAsMultiFilterDefinitions = computed/,
  'UnifiedListTemplate 必须把现有 quick filter definitions 复用为条件 Tab definitions。'
)

assert.match(
  quickFilterHookSource,
  /conditions\?:\s*ListMultiFilterCondition\[\]/,
  'useTableQuickFilter 必须接收标准模板条件 Tab 状态。'
)

assert.match(
  quickFilterHookSource,
  /const applyConditionTabsFilter = async \(\) =>/,
  'useTableQuickFilter 必须能把多个条件 Tab 写回正式 query 参数。'
)

const explicitlyHidden = standardListBlocks.filter((item) =>
  /:show-quick-filter="false"|show-quick-filter="false"/.test(item.openTag)
)
assert.equal(explicitlyHidden.length, 10, '显式隐藏筛选的标准列表数量必须保持为 10 个，避免误开无筛选契约列表。')

const explicitlyMulti = standardListBlocks.filter((item) =>
  /:show-multi-filter="true"|show-multi-filter="true"|:show-multi-filter="showMultiFilter"/.test(item.openTag)
)
assert.equal(explicitlyMulti.length, 2, '已有显式 multi-filter 接入点必须保持为 2 个。')

const defaultSeededConditions = []
for (const file of walkVueFiles(sourceRoot)) {
  const source = fs.readFileSync(file, 'utf8')
  if (/\.setCondition\(/.test(source)) {
    defaultSeededConditions.push(path.relative(sourceRoot, file).replace(/\\/g, '/'))
  }
}
assert.deepEqual(
  defaultSeededConditions,
  [],
  '标准列表条件 Tab 默认必须为空，页面不得用 setCondition 预置筛选条件。'
)

assert.doesNotMatch(
  scheduleOrderSource,
  /completionFilter:\s*'INCOMPLETE'|admissionStatus:\s*['"]READY_TO_ADMIT['"]/,
  '排产工单和同步工单默认 query 参数不得继续携带隐藏筛选条件。'
)

console.log('PASS: unified list template empty condition tabs system contract')
