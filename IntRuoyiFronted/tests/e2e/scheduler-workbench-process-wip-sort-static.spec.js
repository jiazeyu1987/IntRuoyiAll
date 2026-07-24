const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const processWipTablePath = path.join(
  repoRoot,
  'src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue'
)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const processWipTableSource = fs.readFileSync(processWipTablePath, 'utf8')

const findMatchingTemplateEnd = (source, openingEnd) => {
  const tokenPattern = /<\/?template\b[^>]*>/g
  tokenPattern.lastIndex = openingEnd
  let depth = 1
  let match
  while ((match = tokenPattern.exec(source))) {
    if (match[0].startsWith('</template')) {
      depth -= 1
      if (depth === 0) {
        return {
          start: match.index,
          end: tokenPattern.lastIndex
        }
      }
      continue
    }
    depth += 1
  }
  throw new Error('missing closing template tag')
}

const extractFirstTableSlotAfter = (source, marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `missing marker: ${marker}`)
  const slotPattern = /<template\s+#table\b[^>]*>/g
  slotPattern.lastIndex = markerIndex
  const match = slotPattern.exec(source)
  assert.ok(match, `missing table slot after marker: ${marker}`)
  const closing = findMatchingTemplateEnd(source, slotPattern.lastIndex)
  return {
    openingTag: match[0],
    body: source.slice(slotPattern.lastIndex, closing.start)
  }
}

assert(
  /<template\s+#table="\{\s*sortColumnAttrs,\s*handleSortChange:\s*handleTemplateSortChange\s*\}"/.test(
    processWipTableSource
  ),
  'ProcessWipTable 必须继续把标准列表模板排序 helper 透传给表格 slot。'
)

const processWipSlot = extractFirstTableSlotAfter(pageSource, '<ProcessWipTable')
const requiredColumns = [
  'routeCode',
  'routeName',
  'processCode',
  'processName',
  'wipOrderCount',
  'shiftCapacityTotal',
  'shiftStatus',
  'nightShiftEnabled',
  'plannedStartDate',
  'unfinishedDemandQuantity',
  'estimatedStartTime',
  'estimatedCompletionTime',
  'todayFeedbackQuantity'
]

assert(
  processWipSlot.openingTag.includes('sortColumnAttrs'),
  '排产员工作台工序列表 table slot 必须接收 sortColumnAttrs。'
)
assert(
  processWipSlot.openingTag.includes('handleTemplateSortChange'),
  '排产员工作台工序列表 table slot 必须接收排序变更处理器。'
)

const tableTag = (processWipSlot.body.match(/<el-table(?=[\s>])[\s\S]*?>/) || [])[0]
assert.ok(tableTag, '排产员工作台工序列表必须渲染 el-table。')
assert(
  tableTag.includes('@sort-change="handleTemplateSortChange"'),
  '排产员工作台工序列表 el-table 必须把 sort-change 交给标准列表模板。'
)

for (const prop of requiredColumns) {
  const columnTag = (processWipSlot.body.match(
    new RegExp(`<el-table-column\\b(?=[^>]*\\sprop="${prop}")[\\s\\S]*?>`)
  ) || [])[0]
  assert.ok(columnTag, `排产员工作台工序列表缺少列 prop="${prop}"。`)
  assert(
    columnTag.includes(`sortColumnAttrs('${prop}')`),
    `排产员工作台工序列表列 ${prop} 必须绑定 sortColumnAttrs。`
  )
}

console.log('PASS: scheduler workbench process WIP headers are sortable')
