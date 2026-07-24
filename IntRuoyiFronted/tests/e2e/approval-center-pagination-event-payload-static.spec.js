const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const approvalCenterSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/approval-center/index.vue'),
  'utf8'
)
const unifiedListTemplateSource = fs.readFileSync(
  path.join(repoRoot, 'src/components/UnifiedListTemplate/index.vue'),
  'utf8'
)

const collectVueFiles = (dir) => {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  return entries.flatMap((entry) => {
    const entryPath = path.join(dir, entry.name)
    if (entry.isDirectory()) return collectVueFiles(entryPath)
    return entry.isFile() && entry.name.endsWith('.vue') ? [entryPath] : []
  })
}

assert.match(
  unifiedListTemplateSource,
  /@pagination="\$emit\('pagination',\s*\$event\)"/,
  'UnifiedListTemplate must forward Pagination payload so page/limit are not lost'
)

assert.match(
  unifiedListTemplateSource,
  /type\s+UnifiedListPaginationPayload\s*=\s*\{[\s\S]*?page\?:\s*number[\s\S]*?limit\?:\s*number[\s\S]*?\}/,
  'UnifiedListTemplate must type pagination payload with page and limit'
)

assert.match(
  unifiedListTemplateSource,
  /pagination:\s*\[payload:\s*UnifiedListPaginationPayload\]/,
  'UnifiedListTemplate pagination emit must expose the payload type'
)

assert.match(
  approvalCenterSource,
  /@pagination="handlePagination"/,
  'Approval center must use an explicit pagination handler instead of relying on v-model timing'
)

assert.match(
  approvalCenterSource,
  /type\s+PaginationPayload\s*=\s*\{[\s\S]*?page\?:\s*number[\s\S]*?limit\?:\s*number[\s\S]*?\}/,
  'Approval center must type pagination payload with page and limit'
)

assert.match(
  approvalCenterSource,
  /const\s+handlePagination\s*=\s*async\s*\(payload\?:\s*PaginationPayload\)\s*=>\s*\{[\s\S]*?queryParams\.pageNo\s*=\s*payload\.page[\s\S]*?queryParams\.pageSize\s*=\s*payload\.limit[\s\S]*?await\s+getList\(\)/,
  'Approval center pagination handler must apply page/limit payload before loading the list'
)

const paginationPayloadDroppers = collectVueFiles(path.join(repoRoot, 'src'))
  .filter((filePath) => {
    const source = fs.readFileSync(filePath, 'utf8')
    return source.includes('@pagination="emit(\'pagination\')"') ||
      source.includes('@pagination="$emit(\'pagination\')"')
  })
  .map((filePath) => path.relative(repoRoot, filePath).replace(/\\/g, '/'))

assert.deepStrictEqual(
  paginationPayloadDroppers,
  [],
  'Pagination wrapper components must forward $event instead of dropping page/limit payload'
)

console.log('PASS: approval center pagination event payload is preserved')
