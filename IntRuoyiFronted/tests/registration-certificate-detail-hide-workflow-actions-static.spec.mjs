import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const detailPath = 'src/views/dcc/registration-certificate/detail/index.vue'
const actionPanelPath = 'src/views/dcc/registration-certificate/workflow/ActionPanel.vue'

for (const file of [detailPath, actionPanelPath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const detail = read(detailPath)

assert.match(
  detail,
  /data-testid="registration-certificate-detail-page"/,
  'detail page must keep its stable detail-page anchor'
)
for (const token of [
  'getRegistrationCertificateDetail',
  'getRegistrationCertificateHistory',
  '<template #header>备注</template>',
  '<template #header>受托生产企业</template>',
  '<template #header>历史记录</template>'
]) {
  assert.match(
    detail,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `detail page must keep ${token}`
  )
}

assert.match(
  detail,
  /const\s+viewMode\s*=\s*computed\(\(\)\s*=>\s*\{[\s\S]*route\.query\.mode === 'access-request'[\s\S]*return 'access-request'[\s\S]*return 'current'[\s\S]*\}\)/,
  'detail page must model explicit access-request mode separately from normal detail mode'
)
assert.match(
  detail,
  /<RegistrationCertificateActionPanel\s+v-if="viewMode === 'access-request'"/,
  'normal detail mode must not mount the workflow action panel'
)
assert.match(detail, /initial-action="access"/, 'access-request mode must open access action')
assert.match(detail, /read-only/, 'access-request mode must keep maintenance actions locked')
assert.match(
  detail,
  /:downloadable-files="downloadableFiles"/,
  'access-request mode must keep formal downloadable file options'
)
for (const token of [
  `:initial-action="viewMode === 'current' ? 'draft' : 'access'"`,
  `:read-only="viewMode !== 'current'"`,
  ':certificate-status="detail.status"',
  ':row-version="detail.rowVersion"',
  ':snapshot-revision="detail.snapshotRevision"'
]) {
  assert.doesNotMatch(
    detail,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `normal detail page must not keep current-certificate workflow token ${token}`
  )
}

for (const label of ['延续', '变更/作废', '支持文件', '访问申请', '审批结果']) {
  assert.doesNotMatch(
    detail,
    new RegExp(`label=["']${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}["']`),
    `detail page must not declare visible workflow tab ${label}`
  )
}
