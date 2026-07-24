import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')

const read = (relativePath) =>
  readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workflowSource = read('src/api/dcc/controlledFile/workflow.ts')
const detailSource = read('src/views/dcc/controlled-file/detail/index.vue')
const mineSource = read('src/views/dcc/controlled-file/mine/index.vue')
const browserSource = read('src/views/dcc/controlled-file/browser/index.vue')

test('dcc controlled-file download flow uses authenticated blob download helper', () => {
  assert.match(
    workflowSource,
    /export const triggerControlledFileDownload = async/,
    'workflow.ts must expose a dedicated authenticated download helper for DCC controlled files'
  )
  assert.doesNotMatch(
    workflowSource,
    /export const buildControlledFileDownloadUrl =/,
    'workflow.ts should not keep exporting a raw download URL builder that encourages unauthenticated new-tab downloads'
  )
})

test('dcc controlled-file detail page no longer opens raw download urls in a new tab', () => {
  assert.match(
    detailSource,
    /triggerControlledFileDownload/,
    'detail page must use the authenticated controlled-file download helper'
  )
  assert.doesNotMatch(
    detailSource,
    /window\.open\(buildControlledFileDownloadUrl\(controlledFileId\.value\), '_blank'\)/,
    'detail page must not open the raw controlled-file download URL in a new tab'
  )
})

test('dcc controlled-file list pages no longer open raw download urls in a new tab', () => {
  assert.match(
    mineSource,
    /triggerControlledFileDownload/,
    'mine page must use the authenticated controlled-file download helper'
  )
  assert.match(
    browserSource,
    /triggerControlledFileDownload/,
    'browser page must use the authenticated controlled-file download helper'
  )
  assert.doesNotMatch(
    mineSource,
    /window\.open\(url, '_blank'\)/,
    'mine page must not open controlled-file download urls in a new tab'
  )
  assert.doesNotMatch(
    browserSource,
    /window\.open\(buildControlledFileDownloadUrl\(id\), '_blank'\)/,
    'browser page must not open the raw controlled-file download URL in a new tab'
  )
})
