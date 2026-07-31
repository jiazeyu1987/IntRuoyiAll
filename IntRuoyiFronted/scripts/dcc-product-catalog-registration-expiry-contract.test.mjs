import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.resolve(root, relativePath), 'utf8')

test('DCC product catalog registration expiry compare is wired through current-page keys and colors', () => {
  const apiSource = readText('src/api/dcc/controlledFile/productCatalog.ts')
  const panelSource = readText(
    'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
  )

  assert.match(apiSource, /DccProductCatalogRegistrationExpiryCompareReqVO/)
  assert.match(apiSource, /DccProductCatalogRegistrationExpiryCompareRespVO/)
  assert.match(apiSource, /compareRegistrationExpiry/)
  assert.match(apiSource, /\/dcc\/product-catalog\/registration-expiry\/compare/)
  assert.match(apiSource, /status:\s*'MATCH'\s*\|\s*'MISMATCH'\s*\|\s*'FETCH_FAILED'\s*\|\s*'NO_LINK'\s*\|\s*'UNSUPPORTED'/)

  assert.match(panelSource, /注册证有效期/)
  assert.doesNotMatch(panelSource, /handleRefresh/)
  assert.match(panelSource, /handleCompareRegistrationExpiry/)
  assert.match(panelSource, /compareRegistrationExpiry/)
  const compareHandlerMatch = panelSource.match(
    /const handleCompareRegistrationExpiry = async \(\) => \{[\s\S]*?\n\}/
  )
  assert.ok(compareHandlerMatch, 'missing registration expiry compare handler')
  const compareHandlerSource = compareHandlerMatch[0]
  assert.match(compareHandlerSource, /list\.value\.map\(\(row\) => \(\{\s*dataSource:\s*row\.dataSource,\s*originalRowNo:\s*row\.originalRowNo/s)
  assert.doesNotMatch(compareHandlerSource, /registrationInfoLink:\s*row\.registrationInfoLink/)
  assert.match(panelSource, /expiryCompareResultMap/)
  assert.match(panelSource, /getExpiryCompareClass/)
  assert.match(panelSource, /expiry-compare-match/)
  assert.match(panelSource, /expiry-compare-mismatch/)
  assert.match(panelSource, /MATCH[\s\S]*expiry-compare-match/)
  assert.match(panelSource, /(MISMATCH|FETCH_FAILED)[\s\S]*expiry-compare-mismatch/)
  assert.match(panelSource, /expiryCompareResultMap\.value\s*=\s*new Map\(\)/)
})
