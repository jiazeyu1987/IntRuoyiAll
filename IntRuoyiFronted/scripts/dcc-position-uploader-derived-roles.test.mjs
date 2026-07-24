import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc positions page marks uploader-derived roles explicitly', () => {
  const source = readText('src/views/dcc/controlled-file/positions/index.vue')

  assert.match(
    source,
    /UPLOADER_DERIVED_POSITION_NAMES = new Set\(\['编制人直接主管', '部门负责人', '部门授权代表'\]\)/
  )
  assert.match(
    source,
    /AUTHORIZED_REPRESENTATIVE_POSITION_NAMES = new Set\(\['部门授权代表'\]\)/
  )
  assert.match(source, /return '按上传人动态解析'/)
  assert.match(source, /return '授权代表真实来源待确认，运行时将阻塞'/)
  assert.match(source, /'该岗位缺少确认的授权代表来源，当前不允许指定固定人员'/)
})

test('dcc positions page hides manual assignment maintenance for uploader-derived roles', () => {
  const source = readText('src/views/dcc/controlled-file/positions/index.vue')

  assert.match(source, /v-if="!isUploaderDerivedPosition\(row\)"/)
  assert.match(source, /isAuthorizedRepresentativePosition\(row\) \? '来源待定' : '按上传人计算'/)
  assert.match(source, /isAuthorizedRepresentativePosition\(row\)\s*\?\s*'待定'/)
})
