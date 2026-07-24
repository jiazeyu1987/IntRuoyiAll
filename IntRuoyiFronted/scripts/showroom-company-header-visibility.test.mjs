import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractCompanyHeaderActions = () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const match = source.match(
    /<div class="showroom-company-workbench__actions">([\s\S]*?)<\/div>\s*<\/div>/
  )
  assert.ok(match, 'company workbench header actions block must exist')
  return { source, actions: match[1] }
}

test('company workbench header hides the screenshot highlighted display elements', () => {
  const { actions } = extractCompanyHeaderActions()

  assert.doesNotMatch(actions, /current\.live \? '已发布'/)
  assert.doesNotMatch(actions, /resolveCompanyStatus(?:Text|TagType)/)
  assert.doesNotMatch(actions, /进入版本中心/)
  assert.doesNotMatch(actions, /handleOpenVersionCenter/)
  assert.doesNotMatch(actions, /releaseScope\.siteKey/)
  assert.doesNotMatch(actions, /releaseScope\.stage/)
})

test('company workbench header keeps edit and manual release controls', () => {
  const { source, actions } = extractCompanyHeaderActions()

  assert.match(actions, /@click="openEditDialog"/)
  assert.match(actions, /编辑公司/)
  assert.match(actions, /v-if="canPublishShowroomRelease"/)
  assert.match(actions, /@click="handlePublishShowroomRelease"/)
  assert.match(actions, /手动发布展厅/)
  assert.match(source, /const releaseScope = \{\s*siteKey: 'yingtai-showroom',\s*stage: 'TEST' as const\s*\}/)
  assert.match(source, /ShowroomAdminApi\.publishRelease\(buildReleasePublishPayload\(\)\)/)
})
