import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
const browserSource = readText('src/views/dcc/controlled-file/browser/index.vue')
const mineSource = readText('src/views/dcc/controlled-file/mine/index.vue')
const approvalTasksSource = readText('src/views/dcc/controlled-file/approval-tasks/index.vue')
const viewerPresentationSource = readText('src/views/dcc/controlled-file/view/presentation.ts')
const panelPath = 'src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue'
const panelSource = exists(panelPath) ? readText(panelPath) : ''
const previewPanelSource =
  detailSource.match(/<ControlledFileBasicInfoPanel[\s\S]*?\/>/)?.[0] || ''

test('BDD: preview viewer shows a split file/detail layout with editable right-side basic info', () => {
  assert.match(detailSource, /class="detail-viewer-split"/, 'viewer mode must render a split layout')
  assert.match(detailSource, /data-testid="dcc-controlled-preview-layout"/, 'split layout needs a stable test selector')
  assert.match(detailSource, /data-testid="dcc-controlled-preview-file-pane"/, 'left preview pane needs a stable selector')
  assert.match(detailSource, /data-testid="dcc-controlled-preview-detail-pane"/, 'right detail pane needs a stable selector')
  assert.match(detailSource, /ProtectedPdfViewer[\s\S]*:controlled-file-id="controlledFileId"/)
  assert.match(detailSource, /ControlledFileBasicInfoPanel[\s\S]*:column="1"[\s\S]*show-edit/)
  assert.match(detailSource, /edit-test-id="dcc-controlled-preview-detail-edit"/)
  assert.match(panelSource, /:data-testid="editTestId"/)
  assert.match(detailSource, /@edit="openMetadataDialog"/)
})

test('BDD: preview detail header replaces the basic info title with approval distribution version buttons', () => {
  assert.ok(previewPanelSource, 'preview detail must render the shared basic info panel')
  assert.doesNotMatch(previewPanelSource, /title="基础信息"/, 'preview detail must not show the 基础信息 header title')
  assert.match(previewPanelSource, /show-info-actions/, 'preview detail must enable the header action buttons')
  assert.match(previewPanelSource, /@open-approval-info="openPreviewApprovalDialog"/)
  assert.match(previewPanelSource, /@open-distribution-info="openPreviewDistributionDialog"/)
  assert.match(previewPanelSource, /@open-version-info="openPreviewVersionDialog"/)
  for (const [label, testId, eventName] of [
    ['审批', 'dcc-controlled-preview-approval-button', 'openApprovalInfo'],
    ['分发', 'dcc-controlled-preview-distribution-button', 'openDistributionInfo'],
    ['版本', 'dcc-controlled-preview-version-button', 'openVersionInfo']
  ]) {
    assert.match(panelSource, new RegExp(label), `shared panel must render ${label} action`)
    assert.match(panelSource, new RegExp(testId), `shared panel must expose ${testId}`)
    assert.match(panelSource, new RegExp(eventName), `shared panel must emit ${eventName}`)
  }
})

test('BDD: preview detail action buttons open readonly approval distribution version dialogs', () => {
  for (const [title, testId, tableSource] of [
    ['审批矩阵批准情况', 'dcc-controlled-preview-approval-dialog', 'fileDetail\\?\\.routeSnapshots'],
    ['分发信息', 'dcc-controlled-preview-distribution-dialog', 'fileDetail\\?\\.distributionStatuses'],
    ['版本信息', 'dcc-controlled-preview-version-dialog', 'fileDetail\\?\\.versionHistory']
  ]) {
    assert.match(detailSource, new RegExp(`title="${title}"[\\s\\S]*data-testid="${testId}"`))
    assert.match(detailSource, new RegExp(tableSource), `${title} must use loaded file detail data`)
  }
})

test('BDD: normal detail and preview detail share one basic info panel implementation', () => {
  assert.equal(exists(panelPath), true, 'shared basic info panel component must exist')
  assert.match(panelSource, /defineOptions\(\{\s*name:\s*'ControlledFileBasicInfoPanel'\s*\}\)/)
  for (const label of ['文件类别', '受控目录', '文件名称', '产品编号', '产品名称', '培训要求', '现行版本', '流程实例', '提交人', '提交时间', '发布时间', '流程定义', '提交备注']) {
    assert.match(panelSource, new RegExp(label), `shared panel must render ${label}`)
  }
  const usages = detailSource.match(/<ControlledFileBasicInfoPanel/g) || []
  assert.ok(usages.length >= 2, 'detail page must use the shared panel in normal and viewer modes')
  assert.match(detailSource, /ControlledFileBasicInfoPanel[\s\S]*:column="2"/)
})

test('BDD: metadata dialog is available from viewer mode and keeps existing save behavior', () => {
  assert.match(detailSource, /<ControlledFileMetadataDialog[\s\S]*v-model="metadataDialogVisible"/)
  assert.match(detailSource, /@saved="handleMetadataSaved"/)
  assert.ok(
    detailSource.indexOf('<ControlledFileMetadataDialog') > detailSource.indexOf('</template>') - 2000,
    'metadata dialog should live at the page template tail instead of only inside normal detail content'
  )
})

test('BDD: metadata editing is strictly doc_control-only in browser and detail pages', () => {
  for (const [name, source] of [
    ['browser', browserSource],
    ['detail', detailSource]
  ]) {
    assert.match(source, /DOC_CONTROL_ROLE_CODE\s*=\s*'doc_control'/, `${name} must declare doc_control role`)
    assert.match(source, /roles\.includes\(DOC_CONTROL_ROLE_CODE\)/, `${name} must check doc_control directly`)
    assert.doesNotMatch(source, /SUPER_ADMIN_ROLE_CODE/, `${name} must not keep a super_admin metadata role constant`)
    assert.doesNotMatch(source, /super_admin/, `${name} must not expose metadata editing to super_admin`)
    assert.doesNotMatch(source, /hasAnyRoles|hasRole|checkRole/, `${name} must not use generic role fallback helpers`)
  }
})

test('BDD: preview return button routes back to the originating page using returnTo', () => {
  assert.match(viewerPresentationSource, /query\.set\('returnTo'/, 'viewer path builder must encode returnTo')
  assert.match(viewerPresentationSource, /returnTo/, 'viewer presentation must expose returnTo handling')
  assert.match(viewerPresentationSource, /resolveControlledFileViewerReturnTo/, 'viewer presentation must validate returnTo')
  assert.match(detailSource, /<el-button @click="closeViewerMode">[\s\S]*返回[\s\S]*<\/el-button>/, 'viewer button copy must be 返回')
  assert.match(detailSource, /resolveControlledFileViewerReturnTo/, 'detail viewer must resolve validated returnTo')
  assert.match(detailSource, /router\.push\(\s*resolvedReturnTo\s*\)/, 'closeViewerMode must prefer returnTo route push')
  assert.match(detailSource, /delete nextQuery\.returnTo/, 'detail fallback path must strip returnTo')
})

test('BDD: every preview entry passes the current route fullPath as returnTo', () => {
  assert.match(browserSource, /buildControlledFileViewerPath\(id,\s*'browser',\s*route\.fullPath\)/)
  assert.match(mineSource, /buildControlledFileViewerPath\(id,\s*'mine',\s*route\.fullPath\)/)
  assert.match(approvalTasksSource, /buildControlledFileViewerPath\(row\.controlledFile\.id,\s*'approval-tasks',\s*route\.fullPath\)/)
  assert.match(detailSource, /buildControlledFileViewerPath\(controlledFileId\.value,\s*'detail',\s*route\.fullPath\)/)
})
