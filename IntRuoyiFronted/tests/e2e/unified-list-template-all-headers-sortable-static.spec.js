const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const walkVueFiles = (dir, result = []) => {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const absolutePath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walkVueFiles(absolutePath, result)
      continue
    }
    if (entry.isFile() && entry.name.endsWith('.vue')) {
      result.push(absolutePath)
    }
  }
  return result
}

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

const extractTableSlots = (source) => {
  const slots = []
  const slotPattern = /<template\s+#table\b[^>]*>/g
  let match
  while ((match = slotPattern.exec(source))) {
    const openingTag = match[0]
    const openingEnd = slotPattern.lastIndex
    const closing = findMatchingTemplateEnd(source, openingEnd)
    slots.push({
      openingTag,
      body: source.slice(openingEnd, closing.start)
    })
    slotPattern.lastIndex = closing.end
  }
  return slots
}

const packageJson = JSON.parse(readSource('package.json'))
assert.equal(
  packageJson.scripts['e2e:unified-list-template-all-sortable:static'],
  'node tests/e2e/unified-list-template-all-headers-sortable-static.spec.js',
  'package.json must expose the standard-list explicit sorting contract'
)

const unifiedTemplate = readSource('src/components/UnifiedListTemplate/index.vue')
assert.match(
  unifiedTemplate,
  /const DEFAULT_COLUMN_SORTABLE = false/,
  'standard list template must default to non-sortable headers'
)
assert.doesNotMatch(
  unifiedTemplate,
  /sortable:\s*(?:column|sortableColumn)\.sortable\s*\|\|\s*DEFAULT_COLUMN_SORTABLE/,
  'standard list template must preserve explicit sortable values with nullish semantics'
)

const explicitlySortableAllowList = new Set([
  'src/views/dcc/registration-certificate/index/index.vue:certificateNo:custom',
  'src/views/dcc/registration-certificate/index/index.vue:ownerCompanyName:custom',
  'src/views/dcc/registration-certificate/index/index.vue:productName:custom',
  'src/views/dcc/registration-certificate/index/index.vue:classification:custom',
  'src/views/dcc/registration-certificate/index/index.vue:projectCode:custom',
  'src/views/dcc/registration-certificate/index/index.vue:versionNo:custom',
  'src/views/dcc/registration-certificate/index/index.vue:status:custom',
  'src/views/dcc/registration-certificate/index/index.vue:reminder:custom',
  'src/views/dcc/registration-certificate/index/index.vue:hasProjectCode:custom',
  'src/views/dcc/registration-certificate/index/index.vue:hasRegistrationFile:custom',
  'src/views/dcc/registration-certificate/index/index.vue:approvalDate:custom',
  'src/views/dcc/registration-certificate/index/index.vue:effectiveDate:custom',
  'src/views/dcc/registration-certificate/index/index.vue:expiryDate:custom',
  'src/views/dcc/registration-certificate/index/index.vue:remark:custom',
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue:projectName:custom',
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue:projectCode:custom',
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:associatedFileCount:custom',
  'src/views/mes/pro/scheduleorder/index.vue:priorityNo:custom',
  'src/views/mes/pro/route/RouteProductList.vue:itemCode:true',
  'src/views/mes/pro/route/RouteProductList.vue:itemName:true',
  'src/views/mes/pro/route/RouteProductList.vue:specification:true',
  'src/views/mes/pro/route/RouteProductList.vue:unitName:true',
  'src/views/mes/pro/route/RouteProductList.vue:quantity:true',
  'src/views/mes/pro/route/RouteProductList.vue:productionTime:true',
  'src/views/mes/pro/route/RouteProductList.vue:remark:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:routeCode:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:routeName:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:processCode:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:processName:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:wipOrderCount:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:shiftCapacityTotal:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:shiftStatus:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:nightShiftEnabled:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:plannedStartDate:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:unfinishedDemandQuantity:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:estimatedStartTime:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:estimatedCompletionTime:true',
  'src/views/mes/pro/scheduler-workbench/index.vue:todayFeedbackQuantity:true'
])

const failures = []
const explicitSortEntries = []
const vueFiles = walkVueFiles(path.join(repoRoot, 'src', 'views')).map((absolutePath) =>
  path.relative(repoRoot, absolutePath).replaceAll(path.sep, '/')
)

for (const relativePath of vueFiles) {
  const source = readSource(relativePath)
  if (!source.includes('<UnifiedListTemplate')) continue

  for (const slot of extractTableSlots(source)) {
    const columnTags = slot.body.match(/<el-table-column\b[\s\S]*?>/g) || []
    for (const columnTag of columnTags) {
      if (/\s:?sortable(?:=|\s|>)/.test(columnTag)) {
        failures.push(`${relativePath} must use sortColumnAttrs instead of raw el-table-column sortable`)
      }
    }
  }

  const declarationPattern =
    /\{\s*key:\s*'([^']+)'[^}\r\n]*sortable:\s*(true|'custom')[^}\r\n]*\}/g
  let declaration
  while ((declaration = declarationPattern.exec(source))) {
    const key = declaration[1]
    const mode = declaration[2] === 'true' ? 'true' : 'custom'
    explicitSortEntries.push({ relativePath, key, mode })
  }

  const inlinePattern = /sortColumnAttrs\(\{\s*key:\s*'([^']+)'[^)]*sortable:\s*(true|'custom')[^)]*\}\)/g
  let inline
  while ((inline = inlinePattern.exec(source))) {
    const key = inline[1]
    const mode = inline[2] === 'true' ? 'true' : 'custom'
    explicitSortEntries.push({ relativePath, key, mode })
  }
}

for (const entry of explicitSortEntries) {
  const identity = `${entry.relativePath}:${entry.key}:${entry.mode}`
  if (!explicitlySortableAllowList.has(identity)) {
    failures.push(`${identity} is not part of the approved explicit standard-list sort set`)
    continue
  }

  const source = readSource(entry.relativePath)
  const stringHelper = `sortColumnAttrs('${entry.key}')`
  const objectHelper = `sortColumnAttrs({ key: '${entry.key}'`
  if (!source.includes(stringHelper) && !source.includes(objectHelper)) {
    failures.push(`${identity} must be wired through sortColumnAttrs`)
  }
}

for (const identity of explicitlySortableAllowList) {
  if (
    !explicitSortEntries.some(
      (entry) => `${entry.relativePath}:${entry.key}:${entry.mode}` === identity
    )
  ) {
    failures.push(`${identity} must remain explicitly sortable`)
  }
}

assert.deepEqual(failures, [])

console.log('PASS: standard list explicit sorting contract')
