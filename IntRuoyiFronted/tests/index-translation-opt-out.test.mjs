import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'

const indexHtml = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')

assert.match(
  indexHtml,
  /<html\s+lang="en"\s+translate="no"\s+class="notranslate">/,
  'index.html must opt the root document out of automatic translation'
)
assert.match(
  indexHtml,
  /<meta\s+name="google"\s+content="notranslate"\s*\/>/,
  'index.html must include the Google notranslate meta tag'
)

console.log('PASS: index.html opts out of automatic Google translation')
