const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const iframeSource = fs.readFileSync(
  path.join(frontendRoot, 'src/components/IFrame/src/IFrame.vue'),
  'utf8'
)
const designerWrapperSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue'),
  'utf8'
)

assert.match(
  iframeSource,
  /type SameOriginChromeMode =[\s\S]*'off'[\s\S]*'jmreport-viewer'[\s\S]*'jmreport-viewer-fit-width'[\s\S]*'jmreport-designer-edit'/,
  'IFrame must expose a dedicated JMReport designer edit mode'
)

assert.match(
  iframeSource,
  /JMREPORT_DESIGNER_EDIT_ROW_HEIGHT_STYLE_ID/,
  'designer edit mode must inject a named, idempotent row-height adaptation style'
)

assert.match(
  iframeSource,
  /const adaptSameOriginDesignerEditRowHeight = async \(loadToken: number\)/,
  'designer edit mode must run a same-origin adaptation pass after iframe load'
)

assert.match(
  iframeSource,
  /isDesignerEditCanvasPainted/,
  'designer edit row-height adaptation must wait until the JMReport canvas has painted before changing row heights'
)

assert.match(
  iframeSource,
  /DESIGNER_EDIT_PAINT_STABLE_DELAY_MS/,
  'designer edit row-height adaptation must wait for the painted canvas to stabilize before reloading row heights'
)

assert.doesNotMatch(
  iframeSource,
  /MutationObserver/,
  'designer edit row-height adaptation must not loop on style mutations because that can keep the canvas blank'
)

assert.match(
  iframeSource,
  /rowlen|rowHeight|luckysheet/i,
  'designer edit adaptation must target the underlying sheet row height rather than only shrinking inputs'
)

assert.match(
  designerWrapperSource,
  /reportMode\.value === 'edit'[\s\S]*\?[\s\S]*'jmreport-designer-edit'/,
  'Batch record template edit mode must pass jmreport-designer-edit to IFrame'
)

assert.doesNotMatch(
  designerWrapperSource,
  /viewMode\.value === 'preview' \? 'jmreport-viewer' : 'off'/,
  'DesignerWrapper must not leave edit mode with sameOriginChromeMode=off'
)

console.log('PASS: JMReport designer edit row height static contract')
