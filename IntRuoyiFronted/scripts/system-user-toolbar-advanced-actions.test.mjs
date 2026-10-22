import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const assertContains = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

test('system user toolbar groups business actions under advanced and leaves exports in CSV', () => {
  const source = readText('src/views/system/user/index.vue')
  const advancedMenu = source.match(/<template #actions>([\s\S]*?)<\/el-dropdown>/)?.[1]
  const dispatcher = source.match(/const handleAdvancedCommand\s*=[\s\S]*?\n\}/)?.[0]
  assert.ok(advancedMenu, 'toolbar must expose its advanced menu')
  assert.ok(dispatcher, 'advanced menu must have a command dispatcher')

  assertContains(
    source,
    /class="system-user-advanced-actions"/,
    'toolbar must render a single advanced action entry'
  )
  assertContains(
    source,
    /@command="handleAdvancedCommand"/,
    'advanced action entry must dispatch menu commands'
  )
  assertContains(source, />\s*高级\s*</, 'advanced action button text must be 高级')
  assert.doesNotMatch(
    source,
    />\s*重置\s*<\/el-button>/,
    'toolbar must remove the standalone reset button'
  )
  assert.doesNotMatch(
    source,
    /<Icon icon="ep:refresh" \/>重置/,
    'toolbar must not keep the reset icon/text pair'
  )
  assert.doesNotMatch(
    source,
    />\s*删除当前组织\s*</,
    'advanced action labels must be shortened to four chars or fewer'
  )

  // User exports moved to CSV: doc/tasks/20260908-unified-csv-approval-evidence-entry.
  assert.doesNotMatch(advancedMenu, /command="export"|>\s*导出\s*</, 'user toolbar must not restore the old export entry')
  assert.doesNotMatch(dispatcher, /case\s+['"]export['"]\s*:|handleExport\s*\(/, 'advanced dispatcher must not restore the old export flow')

  const advancedLabels = ['新增', '导入', '钉钉导入', '批量删除', '删除组织']
  for (const label of advancedLabels) {
    assert.ok(label.length <= 4, `advanced action label must be at most four chars: ${label}`)
    assertContains(advancedMenu, new RegExp(`>\\s*${label}\\s*<`), `advanced menu must render ${label}`)
  }

  for (const command of [
    'create',
    'import',
    'dingTalkImport',
    'deleteBatch',
    'deleteDept'
  ]) {
    assertContains(
      advancedMenu,
      new RegExp(`command="${command}"`),
      `advanced menu must expose command ${command}`
    )
  }

  assertContains(
    source,
    /case 'create':[\s\S]*openForm\('create'\)/,
    'create command must keep the original create flow'
  )
  assertContains(
    source,
    /case 'import':[\s\S]*handleImport\(\)/,
    'import command must keep the original import flow'
  )
  assertContains(
    source,
    /case 'dingTalkImport':[\s\S]*handleDingTalkImport\(\)/,
    'DingTalk command must keep the original import flow'
  )
  assertContains(
    source,
    /case 'deleteBatch':[\s\S]*handleDeleteBatch\(\)/,
    'batch delete command must keep the original batch delete flow'
  )
  assertContains(
    source,
    /case 'deleteDept':[\s\S]*handleDeleteSelectedDept\(\)/,
    'delete organization command must keep the original organization delete flow'
  )
})

test('CSV account evidence keeps its export permission and formal user export APIs', () => {
  const source = readText('src/views/signature-governance/components/CsvPackageGovernanceListPane.vue')
  const entry = source.match(/\{\s*id: 'account-security-evidence',[\s\S]*?\n\s*\}/)?.[0]
  const handler = source.match(/const exportAccountSecurityEvidenceFromCsv\s*=[\s\S]*?\n\}/)?.[0]
  assert.ok(entry, 'CSV must retain the account security evidence entry')
  assert.ok(handler, 'CSV must retain the account security export handler')
  assertContains(entry, /permission: 'system:user:export'/, 'CSV user export must retain its permission')
  assertContains(entry, /actionType: 'export'/, 'CSV account evidence must remain an export action')
  assertContains(handler, /await UserApi\.exportUser\(/, 'CSV must export the user list through the formal API')
  assertContains(handler, /await UserApi\.exportGenericAccountUsers\(/, 'CSV must retain the generic-account export')
})
