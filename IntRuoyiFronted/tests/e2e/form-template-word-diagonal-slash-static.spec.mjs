import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(testDir, '..', '..')

const read = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const sharedRules = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const editable = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const readonly = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

assert.match(sharedRules, /export type TemplateRawCell\s*=\s*\{[\s\S]*edhrDiagonalSlash\?:\s*boolean/)
assert.match(sharedRules, /edhrDiagonalSlashDirection\?:\s*'TL2BR'\s*\|\s*'TR2BL'\s*\|\s*'BOTH'/)
assert.match(sharedRules, /export const resolveTemplateCellCssStyle\s*=/)

for (const [name, source] of [
  ['editable', editable],
  ['readonly', readonly]
]) {
  assert.match(
    source,
    /'is-diagonal-slash':\s*Boolean\(rawCell\?\.edhrDiagonalSlash\)/,
    `${name} rendered cell must expose the diagonal slash class`
  )
  assert.match(source, /\.is-diagonal-slash::after\s*\{[\s\S]*clip-path:\s*polygon\(/)
  assert.match(source, /\.is-diagonal-slash::after\s*\{[\s\S]*pointer-events:\s*none/)
  assert.match(source, /clip-path:\s*polygon\(calc\(100% - 1px\) 0, 100% 0, 1px 100%, 0 100%\)/)
  assert.match(source, /'is-diagonal-slash-tl2br':\s*rawCell\?\.edhrDiagonalSlashDirection\s*===\s*'TL2BR'/)
  assert.doesNotMatch(source, /is-diagonal-slash[\s\S]{0,500}linear-gradient/)
  assert.match(source, /:style="cell\.cellStyle"/)
  assert.match(source, /cellStyle:\s*resolveTemplateCellCssStyle\(rawCell,\s*layout\.value\?\.styles\)/)
}

assert.match(readonly, /type RawLayoutCell\s*=\s*\{[\s\S]*edhrDiagonalSlash\?:\s*boolean/)
assert.match(editable, /\.edhr-template-editable-form__cell\.is-static\s*\{[\s\S]*background:\s*#fff/)
assert.match(readonly, /\.edhr-template-sheet__cell\.is-static\s*\{[\s\S]*background:\s*#fff/)

console.log('form-template-word-diagonal-slash-static: PASS')
