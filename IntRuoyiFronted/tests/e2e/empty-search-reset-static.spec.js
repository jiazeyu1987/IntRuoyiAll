const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractFunctionBody = (source, functionName) => {
  const startMatch = new RegExp(`const\\s+${functionName}\\s*=\\s*(?:async\\s*)?\\([^)]*\\)\\s*=>\\s*\\{`).exec(source)
  if (!startMatch) return ''
  let index = startMatch.index + startMatch[0].length
  let depth = 1
  while (index < source.length && depth > 0) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    index += 1
  }
  return source.slice(startMatch.index, index)
}

const helperSource = readSource('src/utils/search.ts')
assert.match(helperSource, /export const isSearchModelInputEmpty/, 'must export model-level empty search detector')
assert.match(helperSource, /export const isSearchFormInputEmpty/, 'must export form-ref empty search detector')
assert.match(helperSource, /field !== 'action'/, 'search action pseudo-field must not count as user input')
assert.match(helperSource, /value\.trim\(\) === ''/, 'blank strings must count as empty input')
assert.match(helperSource, /Array\.isArray\(value\)[\s\S]*every/, 'empty date ranges and multiselects must be detected')

const searchComponentSource = readSource('src/components/Search/src/Search.vue')
assert.match(
  searchComponentSource,
  /import \{ isSearchModelInputEmpty \} from '@\/utils\/search'/,
  'generic Search component must import shared empty-search detector'
)
assert.match(
  searchComponentSource,
  /if \(isSearchModelInputEmpty\(model, searchFields\)\) \{[\s\S]*await reset\(\)[\s\S]*return[\s\S]*\}[\s\S]*emit\('search', model\)/,
  'generic Search component must emit reset instead of search when all inputs are empty'
)

const collectVueFiles = (dir) => {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  return entries.flatMap((entry) => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) return collectVueFiles(fullPath)
    if (entry.isFile() && entry.name.endsWith('.vue')) return [fullPath]
    return []
  })
}

const candidateFiles = collectVueFiles(path.join(root, 'src/views'))
  .concat(collectVueFiles(path.join(root, 'src/components')))
  .filter((file) => {
    const source = fs.readFileSync(file, 'utf8')
    return (
      /const\s+handleQuery\s*=\s*(?:async\s*)?\((?:skipEmptyReset = false)?\)\s*=>\s*\{/.test(source) &&
      /const\s+resetQuery\s*=\s*(?:async\s*)?\(\)\s*=>\s*\{/.test(source) &&
      /queryFormRef/.test(source) &&
      /queryParams/.test(source)
    )
  })

assert.ok(candidateFiles.length > 250, 'must cover the bulk of page-level query/reset search forms')

const missingGuard = []
for (const file of candidateFiles) {
  const source = fs.readFileSync(file, 'utf8')
  const body = extractFunctionBody(source, 'handleQuery')
  const resetBody = extractFunctionBody(source, 'resetQuery')
  const resetCallsQuery = /handleQuery\(/.test(resetBody)
  if (
    !source.includes("import { isSearchFormInputEmpty } from '@/utils/search'") ||
    !/const\s+handleQuery\s*=\s*(?:async\s*)?\(skipEmptyReset = false\)\s*=>/.test(source) ||
    !/skipEmptyReset !== true && isSearchFormInputEmpty\(queryFormRef,\s*queryParams(?:\.value)?\)/.test(body) ||
    !/resetQuery\(\)[\s\S]*return/.test(body) ||
    (resetCallsQuery && !/handleQuery\(true\)/.test(resetBody))
  ) {
    missingGuard.push(path.relative(root, file))
  }
}

assert.deepEqual(missingGuard, [], 'all queryFormRef-based search handlers must reset on empty input')

const quickFilterSource = readSource('src/hooks/web/useTableQuickFilter.ts')
assert.match(
  quickFilterSource,
  /if \(isQuickFilterInputEmpty\(\)\) \{[\s\S]*await resetQuickFilter\(\)[\s\S]*return[\s\S]*\}/,
  'standard list quick filter empty search behavior must remain guarded'
)

console.log('PASS: empty search reset static contract')
