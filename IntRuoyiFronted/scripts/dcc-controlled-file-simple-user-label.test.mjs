import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const directUsernameInterpolationPattern =
  /`\$\{(?:item|user)\.nickname\} \(\$\{(?:item|user)\.username\}\)`/

test('dcc controlled-file shared user label helper renders nickname, username and dept', () => {
  const source = readText('src/views/dcc/controlled-file/shared/utils.ts')

  assert.match(source, /export const formatDccSimpleUserLabel =/)
  assert.match(source, /export const buildDccSimpleUserLabelMap =/)
  assert.match(source, /export const isDccUnreadableText =/)
  assert.match(source, /username/)
  assert.match(source, /deptName/)
  assert.match(source, /detailParts/)
  assert.match(source, /isDccUnreadableText\(nickname\) \? username : nickname \|\| username/)
  assert.match(source, /\$\{primary\} \(\$\{detailParts\.join\(' \/ '\)\}\)/)
})

test('dcc controlled-file user label helper rejects question-mark mojibake names', () => {
  const source = readText('src/views/dcc/controlled-file/shared/utils.ts')

  assert.match(source, /text\.includes\('\?'\)/)
  assert.match(source, /text\.includes\('�'\)/)
  assert.match(source, /text\.includes\('□'\)/)
})

test('dcc controlled-file simple-user consumers stop interpolating username directly', () => {
  const noDirectInterpolationConsumers = [
    'src/views/dcc/controlled-file/positions/index.vue',
    'src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue',
    'src/views/dcc/controlled-file/routes/index.vue',
    'src/views/dcc/controlled-file/routes/components/RouteForm.vue',
    'src/views/dcc/controlled-file/signatures/index.vue',
    'src/views/dcc/controlled-file/upload/index.vue',
    'src/views/dcc/controlled-file/training/presentation.ts',
    'src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue',
    'src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue'
  ]
  const helperConsumers = [
    'src/views/dcc/controlled-file/positions/index.vue',
    'src/views/dcc/controlled-file/routes/index.vue',
    'src/views/dcc/controlled-file/routes/components/RouteForm.vue',
    'src/views/dcc/controlled-file/signatures/index.vue',
    'src/views/dcc/controlled-file/upload/index.vue',
    'src/views/dcc/controlled-file/training/presentation.ts',
    'src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue',
    'src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue'
  ]

  for (const relativePath of noDirectInterpolationConsumers) {
    const source = readText(relativePath)

    assert.doesNotMatch(source, directUsernameInterpolationPattern, `${relativePath} should not interpolate username from simple-list users directly`)
  }

  for (const relativePath of helperConsumers) {
    const source = readText(relativePath)
    assert.match(
      source,
      /formatDccSimpleUserLabel|buildDccSimpleUserLabelMap/,
      `${relativePath} should use the shared DCC simple-user label helper`
    )
  }
})

test('dcc signature authorization user column uses readable user label helper', () => {
  const source = readText('src/views/dcc/controlled-file/signatures/index.vue')
  const block = source.match(/const getAuthorizationUserLabel[\s\S]*?\n\}/)?.[0] || ''

  assert.match(block, /formatDccSimpleUserLabel/)
  assert.match(block, /nickname:\s*row\.userName \|\| row\.nickname/)
  assert.doesNotMatch(block, /const nickname = row\.nickname \|\| row\.userName/)
})
