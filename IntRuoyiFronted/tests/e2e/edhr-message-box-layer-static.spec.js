#!/usr/bin/env node
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const useMessagePath = path.join(repoRoot, 'src/hooks/web/useMessage.ts')
const globalStylePath = path.join(repoRoot, 'src/styles/index.scss')

const useMessageSource = fs.readFileSync(useMessagePath, 'utf8')
const globalStyleSource = fs.readFileSync(globalStylePath, 'utf8')

assert.match(
  useMessageSource,
  /APP_CONFIRM_MESSAGE_BOX_MODAL_CLASS\s*=\s*'app-confirm-message-box-overlay'/,
  'useMessage must define one shared high-priority message-box overlay class'
)

for (const methodName of ['confirm', 'delConfirm', 'exportConfirm', 'prompt']) {
  const methodIndex = useMessageSource.indexOf(`${methodName}(`)
  assert.ok(methodIndex >= 0, `useMessage must keep ${methodName}`)
  const nextMethodIndex = useMessageSource.indexOf('\n    // ', methodIndex + 1)
  const methodSource =
    nextMethodIndex >= 0 ? useMessageSource.slice(methodIndex, nextMethodIndex) : useMessageSource.slice(methodIndex)
  assert.ok(
    methodSource.includes('modalClass: APP_CONFIRM_MESSAGE_BOX_MODAL_CLASS'),
    `${methodName} must render above nested business dialogs`
  )
}

assert.match(
  globalStyleSource,
  /\.app-confirm-message-box-overlay\s*\{[\s\S]*z-index:\s*4000\s*!important/,
  'global styles must keep app confirm message boxes above business dialogs'
)

console.log('PASS: eDHR message-box layer static contract')
