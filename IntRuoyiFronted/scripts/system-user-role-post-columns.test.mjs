import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { pathToFileURL } from 'node:url'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('user assignment helpers map ids into readable role and post names', async () => {
  const utilityModulePath = path.join(root, 'src/views/system/user/utils.ts')
  const { buildDisplayNameLookup, resolveDisplayNames, formatDisplayNames } = await import(
    pathToFileURL(utilityModulePath).href
  )

  const roleLookup = buildDisplayNameLookup([
    { id: 1, name: '管理员' },
    { id: 2, name: '访客' }
  ])
  const postLookup = buildDisplayNameLookup([
    { id: 10, name: '班长' },
    { id: 11, name: '操作员' }
  ])

  assert.deepEqual(resolveDisplayNames([2, '1', 999], roleLookup), ['访客', '管理员'])
  assert.deepEqual(resolveDisplayNames(['11', 10], postLookup), ['操作员', '班长'])
  assert.equal(formatDisplayNames(['管理员', '访客']), '管理员、访客')
  assert.equal(formatDisplayNames([]), '-')
})

test('user page wires role and post columns with role/post lookup loading', () => {
  const source = readText('src/views/system/user/index.vue')
  assert.match(source, /label="角色"/)
  assert.match(source, /label="岗位"/)
  assert.match(source, /RoleApi\.(getRolePage|getSimpleRoleList)/)
  assert.match(source, /PostApi\.(getPostPage|getSimplePostList)/)
  assert.match(source, /PermissionApi\.getUserRoleList/)
  assert.match(source, /formatDisplayNames|resolveDisplayNames/)
})
