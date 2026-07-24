import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const assertContains = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

test('system user toolbar removes reset and groups business actions under advanced', () => {
  const source = readText('src/views/system/user/index.vue')

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

  const advancedLabels = ['新增', '导入', '导出', '钉钉导入', '批量删除', '删除组织']
  for (const label of advancedLabels) {
    assert.ok(label.length <= 4, `advanced action label must be at most four chars: ${label}`)
    assertContains(source, new RegExp(`>\\s*${label}\\s*<`), `advanced menu must render ${label}`)
  }

  for (const command of [
    'create',
    'import',
    'export',
    'dingTalkImport',
    'deleteBatch',
    'deleteDept'
  ]) {
    assertContains(
      source,
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
    /case 'export':[\s\S]*handleExport\(\)/,
    'export command must keep the original export flow'
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
