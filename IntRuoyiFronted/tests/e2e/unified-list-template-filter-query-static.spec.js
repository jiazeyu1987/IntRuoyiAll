const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templateSource = read('src/components/UnifiedListTemplate/index.vue')
const dccSignaturePage = read('src/views/dcc/controlled-file/signatures/index.vue')
const edhrSignaturePage = read('src/views/mes/pro/edhr/SignaturePage.vue')

const escapeRegex = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const extractStyleBlock = (source, selector) => {
  const match = source.match(new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([\\s\\S]*?)\\}`))
  assert.ok(match, `${selector} style block must exist`)
  return match[1]
}

const assertQuickFilterBridgeInsideTemplate = (source, tableKey, quickFilterName) => {
  const tableKeyIndex = source.indexOf(`table-key="${tableKey}"`)
  assert.notEqual(tableKeyIndex, -1, `${tableKey} must use the expected table key`)
  const start = source.lastIndexOf('<UnifiedListTemplate', tableKeyIndex)
  assert.notEqual(start, -1, `${tableKey} must render UnifiedListTemplate`)
  const end = source.indexOf('</UnifiedListTemplate>', tableKeyIndex)
  assert.notEqual(end, -1, `${tableKey} UnifiedListTemplate block must close`)
  const block = source.slice(start, end)

  assert.match(
    block,
    new RegExp(`:quick-filter-state="${escapeRegex(quickFilterName)}\\.state"`),
    `${tableKey} must keep the quick-filter state bound into the standard template`
  )
  assert.match(
    block,
    new RegExp(`@update:quick-filter-state="${escapeRegex(quickFilterName)}\\.updateState"`),
    `${tableKey} must keep the condition Tab state update bridge`
  )
  assert.match(
    block,
    new RegExp(`@quick-filter-query="${escapeRegex(quickFilterName)}\\.applyQuickFilter"`),
    `${tableKey} must query through the unified condition Tab bridge`
  )
  assert.doesNotMatch(
    block,
    /<template #extra-filters>|<template #actions>/,
    `${tableKey} must not reintroduce page-level duplicate filter/action areas around the standard Tab filter`
  )
}

const queryFormStyle = extractStyleBlock(templateSource, '.unified-list-template__query-form')
assert.match(
  queryFormStyle,
  /flex-wrap:\s*wrap;/,
  'UnifiedListTemplate filter toolbar must wrap by default so query/reset actions are not pushed off-screen.'
)
assert.doesNotMatch(
  queryFormStyle,
  /flex-wrap:\s*nowrap;/,
  'UnifiedListTemplate must not keep the filter toolbar in a single unbounded row.'
)

const toolbarActionsStyle = extractStyleBlock(
  templateSource,
  '.unified-list-template__toolbar-actions'
)
assert.match(
  toolbarActionsStyle,
  /flex:\s*0\s+0\s+auto;/,
  'UnifiedListTemplate toolbar action area must keep its intrinsic width.'
)
assert.match(
  toolbarActionsStyle,
  /margin-left:\s*auto;/,
  'UnifiedListTemplate toolbar action area should align right without shrinking to zero.'
)
assert.doesNotMatch(
  toolbarActionsStyle,
  /flex:\s*1\s+1\s+auto;/,
  'UnifiedListTemplate toolbar action area must not shrink behind long filter rows.'
)
assert.match(
  templateSource,
  /withDefaults\(defineProps[\s\S]*showQuickFilter:\s*true/,
  'UnifiedListTemplate must show quick filtering by default because Vue casts absent Boolean props to false.'
)

assertQuickFilterBridgeInsideTemplate(
  dccSignaturePage,
  'dcc.electronicSignature.records',
  'recordQuickFilter'
)
assertQuickFilterBridgeInsideTemplate(
  dccSignaturePage,
  'dcc.electronicSignature.authorizations',
  'authorizationQuickFilter'
)
assertQuickFilterBridgeInsideTemplate(
  edhrSignaturePage,
  'mes.pro.edhr.signature.history',
  'signatureQuickFilter'
)

console.log('PASS: unified list template filter query static contract')
