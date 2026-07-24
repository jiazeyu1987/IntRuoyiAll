import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const envLocalPath = path.join(root, '.env.local')
const viteConfigPath = path.join(root, 'vite.config.ts')
const wrapperPath = path.join(root, 'src', 'views', 'mes', 'pro', 'batchrecordtemplate', 'DesignerWrapper.vue')

test('local frontend runtime declares batch-record preview proxy target', () => {
  const envSource = fs.readFileSync(envLocalPath, 'utf8')
  assert.match(
    envSource,
    /VITE_PROXY_TARGET='http:\/\/127\.0\.0\.1:48081'/,
    'local frontend runtime must declare VITE_PROXY_TARGET for same-origin jmreport preview proxying'
  )
})

test('vite config enables /jmreport proxy whenever VITE_PROXY_TARGET is present', () => {
  const source = fs.readFileSync(viteConfigPath, 'utf8')
  assert.match(
    source,
    /const enableJmreportProxy = isBatchRecordPreviewMode \|\| !!env\.VITE_PROXY_TARGET/,
    'vite config must enable jmreport proxy when VITE_PROXY_TARGET is present'
  )
  assert.match(
    source,
    /\.\.\.\(enableJmreportProxy \? \{\s*\['\/jmreport'\]/s,
    'vite config must mount /jmreport proxy behind enableJmreportProxy'
  )
})

test('designer wrapper still requires same-origin preview support for jmreport view mode', () => {
  const source = fs.readFileSync(wrapperPath, 'utf8')
  assert.match(
    source,
    /ensureSameOriginPreviewSupport\(\)/,
    'DesignerWrapper must keep the explicit same-origin preview precondition'
  )
  assert.match(
    source,
    /appendToken\(normalizePreviewPath\(data\.path\), false\)/,
    'DesignerWrapper must continue using same-origin jmreport preview path when support is available'
  )
})
