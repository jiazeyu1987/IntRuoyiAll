import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, extname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const repoRoot = join(scriptDir, '..', '..')
const scanRoots = [
  join(repoRoot, 'IntRuoyiBackend/yudao-module-dcc/src/main/java'),
  join(repoRoot, 'IntRuoyiFronted/src/api/dcc/controlledFile'),
  join(repoRoot, 'IntRuoyiFronted/src/views/dcc/controlled-file')
]
const allowedExtensions = new Set(['.java', '.ts', '.vue'])
const forbiddenTerms = ['工作稿', '工作版本', '现行版', '现行版本']

function listFiles(root) {
  const entries = readdirSync(root)
  return entries.flatMap((entry) => {
    const absolute = join(root, entry)
    const info = statSync(absolute)
    if (info.isDirectory()) return listFiles(absolute)
    return allowedExtensions.has(extname(entry)) ? [absolute] : []
  })
}

test('controlled-file user-visible terminology does not use retired draft/current labels', () => {
  const hits = []
  for (const file of scanRoots.flatMap(listFiles)) {
    const source = readFileSync(file, 'utf8')
    for (const term of forbiddenTerms) {
      if (source.includes(term)) {
        hits.push(`${file.replace(repoRoot + '\\', '')}: ${term}`)
      }
    }
  }
  assert.deepEqual(hits, [])
})

test('controlled-file pages describe effective and pending status without retired labels', () => {
  const uploadPage = readFileSync(join(repoRoot, 'IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
  const browserPage = readFileSync(join(repoRoot, 'IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue'), 'utf8')
  const handlingSummary = readFileSync(join(repoRoot, 'IntRuoyiFronted/src/views/dcc/controlled-file/shared/handlingSummary.ts'), 'utf8')
  const publicationLabels = readFileSync(join(repoRoot, 'IntRuoyiFronted/src/views/dcc/controlled-file/shared/publicationFollowupPresentation.ts'), 'utf8')

  assert.match(uploadPage, /label="当前有效版本"/)
  assert.match(uploadPage, /创建受控文件/)
  assert.match(browserPage, /最新待提交受控文件版本/)
  assert.match(handlingSummary, /当前有效版本/)
  assert.match(publicationLabels, /WORKING: '待提交'/)
})