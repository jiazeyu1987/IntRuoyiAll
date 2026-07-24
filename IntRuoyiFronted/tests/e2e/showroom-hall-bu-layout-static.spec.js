const fs = require('fs')
const path = require('path')

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const marker of [
  'calculateHallBuCanvasLayout',
  "url: '/showroom/hall/calculate-bu-canvas-layout'"
]) {
  if (!apiSource.includes(marker)) {
    throw new Error(`missing BU canvas layout API marker "${marker}" in ${apiPath}`)
  }
}

const dialogPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/HallCanvasLayoutDialog.vue'
)
const dialogSource = fs.readFileSync(dialogPath, 'utf8')

for (const marker of [
  '按 BU 排布',
  'buLayoutCalculating',
  'handleCalculateBuCanvasLayout',
  'ShowroomAdminApi.calculateHallBuCanvasLayout',
  'message.success(\'已按 BU 重新排布画布预览\')'
]) {
  if (!dialogSource.includes(marker)) {
    throw new Error(`missing BU canvas layout dialog marker "${marker}" in ${dialogPath}`)
  }
}

const buHandler = dialogSource.match(/const handleCalculateBuCanvasLayout = async \(\) => \{[\s\S]*?\n\}/)?.[0]
if (!buHandler) {
  throw new Error(`missing handleCalculateBuCanvasLayout implementation in ${dialogPath}`)
}
if (buHandler.includes('updateHallCanvasLayout')) {
  throw new Error(`BU canvas preview must not call save API in ${dialogPath}`)
}
if (!/catch \(error\) \{[\s\S]*message\.error\(resolved\.message\)/.test(buHandler)) {
  throw new Error(`BU canvas preview must surface real API failures in ${dialogPath}`)
}

const saveHandler = dialogSource.match(/const handleSaveCanvasLayout = async \(\) => \{[\s\S]*?\n\}/)?.[0]
if (!saveHandler || !saveHandler.includes('ShowroomAdminApi.updateHallCanvasLayout')) {
  throw new Error(`save handler must keep using updateHallCanvasLayout in ${dialogPath}`)
}

const canvasLayoutPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/hall/canvasLayout.ts'
)
const canvasLayoutSource = fs.readFileSync(canvasLayoutPath, 'utf8')

for (const marker of [
  'assertHallCanvasLayoutPayloadIntegrity',
  "block.itemType === 'PRODUCT'",
  "block.itemType !== 'PRODUCT'",
  '奖项块超出画布右下边界'
]) {
  if (!canvasLayoutSource.includes(marker)) {
    throw new Error(`missing mixed product/award canvas payload marker "${marker}" in ${canvasLayoutPath}`)
  }
}

console.log('PASS: showroom hall BU canvas layout frontend wiring is present')
