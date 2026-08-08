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

const resolveStaticProp = (columnTag) => {
  const propMatch = columnTag.match(/\sprop="([^"]+)"/)
  if (propMatch) return propMatch[1]
  const boundLiteralMatch = columnTag.match(/\s:prop="'([^']+)'"/)
  if (boundLiteralMatch) return boundLiteralMatch[1]
  return undefined
}

const isStructuralColumn = (columnTag, prop) => {
  if (/\stype="(?:selection|index|expand)"/.test(columnTag)) return true
  if (['actions', 'action', 'operation', 'operations'].includes(prop || '')) return true
  if (/\slabel="操作"/.test(columnTag)) return true
  if (/\sfixed="right"/.test(columnTag) && !prop) return true
  return false
}

const packageJson = JSON.parse(readSource('package.json'))
assert.equal(
  packageJson.scripts['e2e:unified-list-template-all-sortable:static'],
  'node tests/e2e/unified-list-template-all-headers-sortable-static.spec.js',
  'package.json must expose the all standard-list sortable headers contract'
)

const failures = []
const vueFiles = walkVueFiles(path.join(repoRoot, 'src', 'views'))
  .map((absolutePath) => path.relative(repoRoot, absolutePath).replaceAll(path.sep, '/'))

for (const relativePath of vueFiles) {
  const source = readSource(relativePath)
  if (!source.includes('<UnifiedListTemplate')) continue
  const slots = extractTableSlots(source)
  assert.ok(slots.length > 0, `${relativePath} must expose a standard list table slot`)
  slots.forEach((slot, slotIndex) => {
    if (!slot.openingTag.includes('sortColumnAttrs')) {
      failures.push(`${relativePath} #table[${slotIndex + 1}] must receive sortColumnAttrs`)
    }
    if (!slot.openingTag.includes('handleTemplateSortChange')) {
      failures.push(`${relativePath} #table[${slotIndex + 1}] must receive handleTemplateSortChange`)
    }

    const tableTags = slot.body.match(/<el-table(?=[\s>])[\s\S]*?>/g) || []
    tableTags.forEach((tableTag, tableIndex) => {
      if (!tableTag.includes('@sort-change="handleTemplateSortChange"')) {
        failures.push(`${relativePath} #table[${slotIndex + 1}] el-table[${tableIndex + 1}] must delegate sort-change to template helper`)
      }
    })

    const columnTags = slot.body.match(/<el-table-column\b[\s\S]*?>/g) || []
    columnTags.forEach((columnTag) => {
      const prop = resolveStaticProp(columnTag)
      if (!prop || isStructuralColumn(columnTag, prop)) return
      if (
        !columnTag.includes(`sortColumnAttrs('${prop}')`) &&
        !columnTag.includes(`sortColumnAttrs({ key: '${prop}'`)
      ) {
        failures.push(`${relativePath} prop="${prop}" must bind sortColumnAttrs('${prop}')`)
      }
    })
  })
}

assert.deepEqual(failures, [])

console.log('PASS: all UnifiedListTemplate business headers are wired to standard sorting')
