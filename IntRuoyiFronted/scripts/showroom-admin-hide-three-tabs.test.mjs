import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const matchRouteBlock = (source, pathName, routeName) =>
  source.match(
    new RegExp(
      String.raw`\{\s*path: '${pathName}'[\s\S]*?name: '${routeName}'[\s\S]*?meta: \{[\s\S]*?\}\s*\}`,
      'm'
    )
  )?.[0] || ''

test('showroom-admin hides history, assignment, and discussion child tabs from navigation', () => {
  const source = readText('src/router/modules/showroom.ts')
  const historyBlock = matchRouteBlock(source, 'history', 'ShowroomAdminHistory')
  const assignmentBlock = matchRouteBlock(source, 'assignment', 'ShowroomAdminAssignment')
  const discussionBlock = matchRouteBlock(source, 'discussion', 'ShowroomAdminDiscussion')

  assert.ok(historyBlock, 'history route block should exist')
  assert.ok(assignmentBlock, 'assignment route block should exist')
  assert.ok(discussionBlock, 'discussion route block should exist')

  assert.match(historyBlock, /hidden: true/)
  assert.match(assignmentBlock, /hidden: true/)
  assert.match(discussionBlock, /hidden: true/)

  assert.match(historyBlock, /canTo: true/)
  assert.match(assignmentBlock, /canTo: true/)
  assert.match(discussionBlock, /canTo: true/)
})
