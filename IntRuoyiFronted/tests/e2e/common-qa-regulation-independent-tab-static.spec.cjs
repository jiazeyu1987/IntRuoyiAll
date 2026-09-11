const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-regulation-workspace-tabs') &&
    page.includes('label="QA检验规程"') &&
    page.includes('label="通用检验规程"') &&
    page.includes("type RegulationWorkspaceTabName = 'qa' | 'common'"),
  'RED: QA and common inspection regulations must be separate first-level workspaces'
)

const qaSecondaryTabs =
  page.match(/data-qa-regulation-qa-detail-tabs[\s\S]*?<\/el-tabs>/)?.[0] || ''
assert(
  qaSecondaryTabs.includes('label="总览"') &&
    qaSecondaryTabs.includes('label="检验项目"') &&
    qaSecondaryTabs.includes('label="任务预览"') &&
    !qaSecondaryTabs.includes('label="通用检验规程"'),
  'RED: the QA detail tabs must contain only QA-owned views'
)

assert(
  page.includes('data-qa-regulation-qa-workspace') &&
    page.includes('data-qa-regulation-common-workspace') &&
    page.includes("regulationWorkspaceTab === 'qa'") &&
    page.includes("regulationWorkspaceTab === 'common'"),
  'RED: QA and common regulation controls must render in separate workspace containers'
)

assert(
  page.includes('data-qa-common-layout-header') &&
    page.includes('data-qa-common-set-switch') &&
    page.includes('data-qa-regulation-common-word-import') &&
    page.includes('initializeCommonRegulationWorkspace'),
  'RED: the common regulation workspace must own its switch, status, import entry, and independent loading'
)

assert(
  page.includes("regulationWorkspaceTab.value = 'common'") &&
    !page.includes("qaActiveTab.value = 'common'") &&
    !page.includes("type QaRegulationTabName = 'overview' | 'common'"),
  'RED: common regulation navigation must not reuse QA detail-tab state'
)

console.log('GREEN: common inspection regulation independent-tab contract is present')
