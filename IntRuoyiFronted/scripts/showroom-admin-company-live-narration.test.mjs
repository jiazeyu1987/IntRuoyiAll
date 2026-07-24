import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom frontstage narration api should request the direct display narration endpoint', () => {
  const source = readText('src/api/showroom-frontstage/index.ts')

  assert.match(source, /getDisplayNarration:\s*async\s*\(params:\s*ShowroomNarrationQuery\)/)
  assert.match(source, /request\.get\(/)
  assert.match(source, /buildDisplayUrl\('\/showroom\/display\/narration'\)/)
  assert.match(source, /params\s*\}/)
  assert.doesNotMatch(
    source,
    /getDisplayNarration[\s\S]*createWebsiteConfigContext[\s\S]*narrationQuery/
  )
})

test('company workbench should load live narration from text and audioUrl payloads', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  for (const token of [
    'ShowroomFrontstageApi.getDisplayNarration',
    'liveNarration.zhText = zhPayload?.text || \'\'',
    'liveNarration.zhAudioUrl = zhPayload?.audioUrl || \'\'',
    'liveNarration.enText = enPayload?.text || \'\'',
    'liveNarration.enAudioUrl = enPayload?.audioUrl || \'\''
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
