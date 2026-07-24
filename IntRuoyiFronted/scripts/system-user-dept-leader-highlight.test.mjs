import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { pathToFileURL } from 'node:url'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('user helpers sort each department leader to the top of the filtered list', async () => {
  const utilityModulePath = path.join(root, 'src/views/system/user/utils.ts')
  const { buildDeptLeaderLookup, isDeptLeader, sortUsersByDeptLeader } = await import(
    pathToFileURL(utilityModulePath).href
  )

  const deptLeaderByDeptId = buildDeptLeaderLookup([
    { id: 10, leaderUserId: 102 },
    { id: 20, leaderUserId: 202 }
  ])

  const orderedUsers = sortUsersByDeptLeader(
    [
      { id: 101, deptId: 10, username: 'member-a' },
      { id: 201, deptId: 20, username: 'member-b' },
      { id: 102, deptId: 10, username: 'leader-a' },
      { id: 202, deptId: 20, username: 'leader-b' },
      { id: 103, deptId: 10, username: 'member-c' }
    ],
    deptLeaderByDeptId,
    10
  )

  assert.deepEqual(
    orderedUsers.map((user) => user.id),
    [102, 101, 103, 202, 201]
  )
  assert.equal(isDeptLeader(orderedUsers[0], deptLeaderByDeptId), true)
  assert.equal(isDeptLeader(orderedUsers[1], deptLeaderByDeptId), false)

  const prioritizedChildDeptUsers = sortUsersByDeptLeader(
    [
      { id: 101, deptId: 10, username: 'member-a' },
      { id: 201, deptId: 20, username: 'member-b' },
      { id: 102, deptId: 10, username: 'leader-a' },
      { id: 202, deptId: 20, username: 'leader-b' },
      { id: 103, deptId: 10, username: 'member-c' }
    ],
    deptLeaderByDeptId,
    20
  )
  assert.deepEqual(
    prioritizedChildDeptUsers.map((user) => user.id),
    [202, 201, 102, 101, 103]
  )
})

test('user page wires department leader ordering and green username styling', () => {
  const source = readText('src/views/system/user/index.vue')
  assert.match(source, /sortUsersByDeptLeader/)
  assert.match(source, /isDeptLeaderUser/)
  assert.match(source, /system-user-username--dept-leader/)
  assert.match(source, /DeptApi\.getDeptList\(\{\}\)/)
})
