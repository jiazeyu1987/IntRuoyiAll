import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('system user row actions are text-only buttons arranged in two rows', () => {
  const source = readText('src/views/system/user/index.vue')
  const operationIndex = source.indexOf('label="操作"')
  assert.notEqual(operationIndex, -1, 'user table must keep the operation column')

  const operationEndIndex = source.indexOf('</el-table-column>', operationIndex)
  assert.notEqual(operationEndIndex, -1, 'operation column must have a closing tag')
  const operationColumn = source.slice(operationIndex, operationEndIndex)

  assert.match(
    operationColumn,
    /class="[^"]*system-user-row-actions[^"]*"/,
    'operation column must use an explicit flat action panel'
  )
  assert.match(
    operationColumn,
    /getUserColumnWidthString\('actions',\s*(1[6-9]\d|2[0-2]\d)\)/,
    'operation column must use a compact width for two-row text actions'
  )
  assert.doesNotMatch(operationColumn, />\s*更多\s*</, 'operation column must not render 更多')
  assert.doesNotMatch(
    operationColumn,
    /<el-dropdown[\s\S]*handleCommand\(command,\s*scope\.row\)/,
    'operation column must not keep row actions inside a dropdown'
  )
  assert.doesNotMatch(
    source,
    /const handleCommand\s*=/,
    'unused row dropdown command dispatcher must be removed'
  )
  assert.doesNotMatch(
    operationColumn,
    /<Icon\s+icon="ep:(edit|delete|key|circle-check)"/,
    'row action buttons must be text-only without icons'
  )

  const actionRows = [
    ...operationColumn.matchAll(/<div class="system-user-row-actions__row">([\s\S]*?)<\/div>/g)
  ].map((match) => match[1])
  assert.equal(actionRows.length, 2, 'row action panel must render exactly two action rows')

  const expectedRows = [
    ['修改', '删除'],
    ['重置密码', '分配角色']
  ]
  for (const [index, labels] of expectedRows.entries()) {
    for (const label of labels) {
      assert.match(
        actionRows[index],
        new RegExp(`>\\s*${label}\\s*<`),
        `row ${index + 1} must render ${label}`
      )
    }
  }

  const actionLabels = ['修改', '删除', '重置密码', '分配角色']
  for (const label of actionLabels) {
    assert.match(
      operationColumn,
      new RegExp(`>\\s*${label}\\s*<`),
      `row action must render ${label}`
    )
  }

  const clickContracts = [
    /@click="openForm\('update', scope\.row\.id\)"/,
    /@click="handleDelete\(scope\.row\.id\)"/,
    /@click="handleResetPwd\(scope\.row\)"/,
    /@click="handleRole\(scope\.row\)"/
  ]
  for (const contract of clickContracts) {
    assert.match(operationColumn, contract, `row action must keep click contract ${contract}`)
  }
})
