import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const assertContains = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

test('system user page uses the standard list template contract', () => {
  const source = readText('src/views/system/user/index.vue')

  assertContains(
    source,
    /<UnifiedListTemplate[\s\S]*table-key="system\.user\.main"/,
    'user page must render UnifiedListTemplate with a stable table key'
  )
  assertContains(
    source,
    /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
    'user page must import UnifiedListTemplate'
  )
  assertContains(
    source,
    /useUserTableColumns\('system\.user\.main'/,
    'user page must use persisted column settings'
  )
  assertContains(
    source,
    /useTableQuickFilter\(\s*'system\.user\.main'/,
    'user page must use standard quick filtering'
  )
  assertContains(
    source,
    /@column-change="saveUserColumnConfig"/,
    'column visibility changes must autosave through the template'
  )
  assertContains(source, /@pagination="getList"/, 'pagination must be emitted by the template')
  assertContains(
    source,
    /data-user-table-column-explicit/,
    'explicit column settings must disable duplicate global enhancement'
  )
  assertContains(
    source,
    /data-user-table-key="system\.user\.main"/,
    'table must expose the same table key for column persistence'
  )
  assertContains(
    source,
    /@header-dragend="handleUserHeaderDragend"/,
    'header resize must persist column widths'
  )
  assertContains(
    source,
    /:allow-drag-last-column="true"/,
    'table must allow user column width dragging'
  )

  for (const columnKey of [
    'id',
    'username',
    'nickname',
    'deptName',
    'roleNamesText',
    'postNamesText',
    'mobile',
    'status',
    'createTime'
  ]) {
    assertContains(
      source,
      new RegExp(`key:\\s*'${columnKey}'`),
      `user page must declare column key ${columnKey}`
    )
    assertContains(
      source,
      new RegExp(`isUserColumnVisible\\('${columnKey}'\\)`),
      `column ${columnKey} must be controlled by visibility settings`
    )
  }

  for (const columnKey of ['selection', 'actions']) {
    assertContains(
      source,
      new RegExp(`key:\\s*'${columnKey}'`),
      `user page must declare column key ${columnKey}`
    )
    assert.doesNotMatch(
      source,
      new RegExp(`v-if="isUserColumnVisible\\('${columnKey}'\\)"`),
      `column ${columnKey} must remain visible because batch and row actions depend on it`
    )
  }

  assertContains(
    source,
    /key:\s*'selection'[\s\S]*hideable:\s*false/,
    'selection column must not be hidden because batch actions depend on it'
  )
  assertContains(
    source,
    /key:\s*'actions'[\s\S]*hideable:\s*false/,
    'actions column must not be hidden'
  )
  assertContains(
    source,
    /key:\s*'username'[\s\S]*queryParamKey:\s*'username'/,
    'quick filter must support username search'
  )
  assertContains(
    source,
    /key:\s*'mobile'[\s\S]*queryParamKey:\s*'mobile'/,
    'quick filter must support mobile search'
  )
  assertContains(
    source,
    /key:\s*'status'[\s\S]*queryParamKey:\s*'status'/,
    'quick filter must support status search'
  )
  assertContains(
    source,
    /key:\s*'createTime'[\s\S]*queryParamKey:\s*'createTime'/,
    'quick filter must support create time search'
  )
  assert.doesNotMatch(
    source,
    /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/,
    'user page must not keep a second standalone pagination component'
  )
})
