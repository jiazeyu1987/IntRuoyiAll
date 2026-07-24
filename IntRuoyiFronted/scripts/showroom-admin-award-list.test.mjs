import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractConstArrowFunction = (source, functionName) => {
  const marker = `const ${functionName} = `
  const startIndex = source.indexOf(marker)
  assert.notEqual(startIndex, -1, `missing function marker: ${marker}`)
  const functionStartIndex = startIndex + marker.length
  const bodyStartIndex = source.indexOf('{', functionStartIndex)
  assert.notEqual(bodyStartIndex, -1, `missing function body for: ${functionName}`)
  let depth = 0
  for (let index = bodyStartIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(functionStartIndex, index + 1)
      }
    }
  }
  throw new Error(`missing function end for: ${functionName}`)
}

const loadNormalizeAwardRows = () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const helperNames = [
    'normalizeObject',
    'normalizeId',
    'normalizeTotal',
    'normalizeOptionalBoolean',
    'resolveStringValue'
  ]
  const helperSources = helperNames
    .map((name) => `const ${name} = ${extractConstArrowFunction(source, name)}`)
    .join(';\n')
  const functionSource = extractConstArrowFunction(source, 'normalizeAwardRows')
  const transformed = stripTypeScriptTypes(
    `${helperSources};\nconst normalizeAwardRows = ${functionSource};\nresult = normalizeAwardRows`,
    { mode: 'transform' }
  )
  const sandbox = { result: null }
  vm.createContext(sandbox)
  vm.runInContext(transformed, sandbox)
  return sandbox.result
}

test('normalizeAwardRows displays fields from displayRevision when top-level award fields are absent', () => {
  const normalizeAwardRows = loadNormalizeAwardRows()

  const rows = normalizeAwardRows([
    {
      awardId: 47,
      awardCode: 'AWARD-001',
      currentRevisionId: 189,
      incomplete: false,
      live: true,
      revision: {
        revisionId: 189,
        revisionNo: 2,
        status: 'PUBLISHED',
        nameCn: '社会贡献奖 Draft',
        nameEn: 'Social Contribution Award Draft',
        issuer: '草稿颁发单位',
        awardDateText: '草稿日期',
        coverImageUrl: '/draft-cover.png'
      },
      displayRevision: {
        revisionId: 189,
        revisionNo: 2,
        status: 'PUBLISHED',
        nameCn: '社会贡献奖',
        nameEn: 'Social Contribution Award',
        issuer: '嘉定区江桥镇人民政府',
        awardDateText: '2022年度；二0二三年二月',
        coverImageUrl:
          '/admin-api/infra/file/28/get/showroom/product/cover/20260618/product-AWARD-001-imported-cover.png'
      }
    }
  ])

  assert.equal(rows.length, 1)
  assert.equal(rows[0].awardId, 47)
  assert.equal(rows[0].awardCode, 'AWARD-001')
  assert.equal(rows[0].nameCn, '社会贡献奖')
  assert.equal(rows[0].nameEn, 'Social Contribution Award')
  assert.equal(rows[0].issuer, '嘉定区江桥镇人民政府')
  assert.equal(rows[0].awardDateText, '2022年度；二0二三年二月')
  assert.equal(
    rows[0].coverImageUrl,
    '/admin-api/infra/file/28/get/showroom/product/cover/20260618/product-AWARD-001-imported-cover.png'
  )
  assert.equal(rows[0].incomplete, false)
  assert.equal(rows[0].revisionNo, 2)
})

test('award list table binds normalized flat fields instead of raw nested response shape', () => {
  const source = readText('src/views/showroom-admin/components/AwardListTable.vue')

  assert.match(source, /prop="nameCn"/)
  assert.match(source, /row\.nameEn \|\| '未填写'/)
  assert.match(source, /row\.coverImageUrl/)
})
