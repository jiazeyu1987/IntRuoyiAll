import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'

const root = path.resolve(import.meta.dirname, '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('RouteForm uses wider dialog width for route process area', () => {
  const source = readText('src/views/mes/pro/route/RouteForm.vue')

  assert.match(
    source,
    /width="1320px"/,
    '工艺路线弹框应加宽到 1320px，避免批记录信息显示不全'
  )
})

test('RouteForm closes dialog after update save succeeds', () => {
  const source = readText('src/views/mes/pro/route/RouteForm.vue')

  const updateBranchMatch = source.match(
    /else\s*\{\s*await ProRouteApi\.updateRoute\(data\)([\s\S]*?)message\.success\('修改成功'\)([\s\S]*?)\n\s*}\s*\n\s*emit\('success'\)/
  )
  assert.ok(updateBranchMatch, '更新分支应存在')
  const updateBranchSource = updateBranchMatch[0]
  assert.match(
    updateBranchSource,
    /dialogVisible\.value\s*=\s*false/,
    '编辑工艺路线保存成功后应自动关闭弹框'
  )
})
