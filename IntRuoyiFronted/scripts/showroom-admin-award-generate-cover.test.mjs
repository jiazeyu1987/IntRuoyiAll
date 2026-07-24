import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('award list table exposes generate-cover row action and loading binding', () => {
  const source = readText('src/views/showroom-admin/components/AwardListTable.vue')

  assert.match(source, /generateCover|generate-cover/)
  assert.match(source, /生图/)
  assert.match(source, /generatingCoverAwardId/)
  assert.match(source, /row\.awardId/)
})

test('showroom admin page wires award generate-cover api call and refresh flow', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const apiSource = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /handleGenerateAwardCoverImage/)
  assert.match(source, /ShowroomAdminApi\.generateAwardCoverImage/)
  assert.match(source, /已生成并发布新版本/)
  assert.match(source, /loadAwardRows\(\)/)

  assert.match(apiSource, /export interface ShowroomAwardCoverGenerateRespVO/)
  assert.match(apiSource, /generateAwardCoverImage: async/)
  assert.match(apiSource, /\/showroom\/award\/generate-cover-image/)
})
