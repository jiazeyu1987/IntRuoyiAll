import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const collectSourceFiles = (relativeDir) => {
  const absoluteDir = path.join(root, relativeDir)
  const stack = [absoluteDir]
  const files = []

  while (stack.length > 0) {
    const currentDir = stack.pop()
    for (const entry of fs.readdirSync(currentDir, { withFileTypes: true })) {
      const absolutePath = path.join(currentDir, entry.name)
      if (entry.isDirectory()) {
        stack.push(absolutePath)
        continue
      }
      if (entry.isFile() && /\.(vue|ts)$/.test(entry.name)) {
        files.push(path.relative(root, absolutePath))
      }
    }
  }

  return files.sort()
}

test('showroom admin route titles keep 展柜 menu copy and rename company tab to 公司信息', () => {
  const source = readText('src/router/modules/showroom.ts')

  assert.match(source, /title: '展柜'/)
  assert.match(source, /meta: \{ title: '公司信息'/)
  assert.match(source, /meta: \{ title: '展柜管理'/)
  assert.doesNotMatch(source, /title: '展厅'/)
  assert.doesNotMatch(source, /meta: \{ title: '展厅公司'/)
  assert.doesNotMatch(source, /meta: \{ title: '展柜公司'/)
  assert.doesNotMatch(source, /meta: \{ title: '展厅管理'/)
})

test('showroom admin visible copy replaces 展厅 with 展柜 across admin sources', () => {
  const adminFiles = collectSourceFiles('src/views/showroom-admin')

  assert.ok(adminFiles.length > 0, 'showroom-admin source files should exist')

  for (const relativePath of adminFiles) {
    const source = readText(relativePath)
    assert.doesNotMatch(source, /展厅/, `${relativePath} still contains 展厅 copy`)
  }

  const indexSource = readText('src/views/showroom-admin/index.vue')
  const hallListSource = readText('src/views/showroom-admin/components/HallListTable.vue')
  const mappingSource = readText('src/views/showroom-admin/components/HallProductMappingDialog.vue')

  assert.match(indexSource, /新增展柜|编辑展柜/)
  assert.match(hallListSource, /展柜名称/)
  assert.match(hallListSource, /新增展柜/)
  assert.match(mappingSource, /选择当前展柜包含的产品/)
})
