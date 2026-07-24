import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const extractCssRule = (source, selector) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = source.match(new RegExp(`${escapedSelector}\\s*\\{([\\s\\S]*?)\\n\\}`, 'm'))
  assert.ok(match, `Missing CSS rule: ${selector}`)
  return match[1]
}

const loadTsModule = async (relativePath) => {
  const source = readText(relativePath)
  const transformed = stripTypeScriptTypes(source, { mode: 'transform' })
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transformed).toString('base64')}`
  return await import(moduleUrl)
}

test('hall list exposes a real canvas layout action and dialog wiring', () => {
  const hallListSource = readText('src/views/showroom-admin/components/HallListTable.vue')
  const adminSource = readText('src/views/showroom-admin/index.vue')
  const apiSource = readText('src/api/showroom-admin/index.ts')
  const dialogSource = readText('src/views/showroom-admin/components/HallCanvasLayoutDialog.vue')

  assert.match(hallListSource, /画布布局/)
  assert.match(hallListSource, /'open-canvas'/)
  assert.match(adminSource, /HallCanvasLayoutDialog/)
  assert.match(adminSource, /@open-canvas="openHallCanvasLayout"/)
  assert.match(adminSource, /hallCanvasDialogVisible/)
  assert.match(apiSource, /ShowroomHallCanvasLayoutReqVO/)
  assert.match(apiSource, /updateHallCanvasLayout/)
  assert.match(apiSource, /\/showroom\/hall\/update-item-canvas-layout/)
  assert.match(dialogSource, /Website 预览/)
  assert.match(dialogSource, /previewMode/)
  assert.match(dialogSource, /data-preview-mode="website"/)
  assert.match(apiSource, /previewImageUrl/)
})

test('hall canvas dialog supports an editor-only stretched background image', () => {
  const apiSource = readText('src/api/showroom-admin/index.ts')
  const dialogSource = readText('src/views/showroom-admin/components/HallCanvasLayoutDialog.vue')
  const contractsSource = readText('src/views/showroom-admin/hall/contracts.ts')

  assert.match(contractsSource, /canvasBackgroundImageUrl/)
  assert.match(apiSource, /ShowroomHallCanvasBackgroundReqVO/)
  assert.match(apiSource, /updateHallCanvasBackground/)
  assert.match(apiSource, /\/showroom\/hall\/update-canvas-background/)
  assert.match(dialogSource, /showroom\/hall\/canvas-background/)
  assert.match(dialogSource, /上传背景图/)
  assert.match(dialogSource, /清除背景图/)
  assert.match(dialogSource, /仅辅助布局，不发布/)
  assert.match(dialogSource, /showroom-hall-canvas-dialog__background/)
  assert.match(dialogSource, /object-fit:\s*fill/)
  assert.match(dialogSource, /v-if="previewMode === 'editor' && canvasBackgroundImageUrl"/)
})

test('editor canvas blocks do not cover the auxiliary background image', () => {
  const dialogSource = readText('src/views/showroom-admin/components/HallCanvasLayoutDialog.vue')
  const editorBlockRule = extractCssRule(
    dialogSource,
    '.showroom-hall-canvas-dialog__block.is-editor'
  )

  assert.match(editorBlockRule, /background:\s*transparent/)
  assert.match(
    dialogSource,
    /\.showroom-hall-canvas-dialog__block\.is-editor span[\s\S]*background:\s*rgba\(255,\s*255,\s*255,\s*0\.86\)/,
    'editor block names should keep a compact readable text backing'
  )
  assert.match(
    dialogSource,
    /\.showroom-hall-canvas-dialog__block\.is-editor small[\s\S]*background:\s*rgba\(255,\s*255,\s*255,\s*0\.78\)/,
    'editor block codes should keep a compact readable text backing'
  )
  assert.doesNotMatch(
    editorBlockRule,
    /background:\s*rgba\(255,\s*255,\s*255,\s*0\.78\)/,
    'editor blocks must not use a full-cell translucent white fill that hides the background'
  )
})

test('canvas dialog toolbar keeps the title from overlapping dense actions', () => {
  const dialogSource = readText('src/views/showroom-admin/components/HallCanvasLayoutDialog.vue')
  const toolbarRule = extractCssRule(dialogSource, '.showroom-hall-canvas-dialog__toolbar')
  const titleRule = extractCssRule(dialogSource, '.showroom-hall-canvas-dialog__title')
  const titleStrongRule = extractCssRule(dialogSource, '.showroom-hall-canvas-dialog__title strong')
  const actionsRule = extractCssRule(dialogSource, '.showroom-hall-canvas-dialog__actions')
  const backgroundToolsRule = extractCssRule(
    dialogSource,
    '.showroom-hall-canvas-dialog__background-tools'
  )

  assert.match(toolbarRule, /flex-direction:\s*column/)
  assert.match(toolbarRule, /align-items:\s*stretch/)
  assert.match(titleRule, /flex-wrap:\s*wrap/)
  assert.match(titleStrongRule, /min-width:\s*0/)
  assert.match(actionsRule, /flex-wrap:\s*wrap/)
  assert.match(actionsRule, /min-width:\s*0/)
  assert.match(backgroundToolsRule, /flex-wrap:\s*wrap/)
  assert.match(backgroundToolsRule, /min-width:\s*0/)
})

test('canvas layout geometry keeps full coverage when splitting, deleting, swapping, and resizing', async () => {
  const {
    assertCanvasIntegrity,
    clampCanvasBoundaryDelta,
    createDefaultCanvasBlocks,
    deleteCanvasBlock,
    resizeCanvasBoundary,
    splitCanvasBlock,
    swapCanvasBlockProducts
  } = await loadTsModule('src/views/showroom-admin/hall/canvasLayout.ts')

  const products = [
    { productId: 101, productCode: 'P-101', nameCn: '产品A', previewImageUrl: 'https://cdn.example.com/a.png' },
    { productId: 102, productCode: 'P-102', nameCn: '产品B', previewImageUrl: '' },
    { productId: 103, productCode: 'P-103', nameCn: '产品C', previewImageUrl: 'https://cdn.example.com/c.png' },
    { productId: 104, productCode: 'P-104', nameCn: '产品D', previewImageUrl: 'https://cdn.example.com/d.png' }
  ]
  const initial = createDefaultCanvasBlocks(products)
  assert.equal(initial.length, 4)
  assertCanvasIntegrity(initial)
  assert.equal(initial[0].previewImageUrl, 'https://cdn.example.com/a.png')
  assert.equal(initial[1].previewImageUrl, '')
  assert.deepEqual(
    initial.map((block) => [block.x, block.y, block.width, block.height]),
    [
      [0, 0, 0.5, 0.5],
      [0.5, 0, 0.5, 0.5],
      [0, 0.5, 0.5, 0.5],
      [0.5, 0.5, 0.5, 0.5]
    ]
  )

  const split = splitCanvasBlock(initial, initial[0].blockId, {
    productId: 105,
    productCode: 'P-105',
    nameCn: '产品E',
    previewImageUrl: 'https://cdn.example.com/e.png'
  })
  assert.equal(split.length, 5)
  assertCanvasIntegrity(split)
  assert.equal(split.find((block) => block.productId === 105)?.previewImageUrl, 'https://cdn.example.com/e.png')

  const swapped = swapCanvasBlockProducts(split, split[0].blockId, split[1].blockId)
  assert.equal(swapped[0].productId, split[1].productId)
  assert.equal(swapped[1].productId, split[0].productId)
  assert.deepEqual(
    swapped.map(({ x, y, width, height }) => ({ x, y, width, height })),
    split.map(({ x, y, width, height }) => ({ x, y, width, height }))
  )

  const resized = resizeCanvasBoundary(initial, {
    orientation: 'vertical',
    x: 0.5,
    y1: 0,
    y2: 0.5,
    delta: 0.1
  })
  assertCanvasIntegrity(resized)
  assert.equal(resized[0].width, 0.6)
  assert.equal(resized[1].x, 0.6)
  assert.equal(resized[1].width, 0.4)
  assert.equal(resized[2].width, 0.5)

  const clampedDelta = clampCanvasBoundaryDelta(initial, {
    orientation: 'vertical',
    x: 0.5,
    y1: 0,
    y2: 0.5,
    delta: 0.9
  })
  assert.equal(clampedDelta, 0.46)
  const clamped = resizeCanvasBoundary(initial, {
    orientation: 'vertical',
    x: 0.5,
    y1: 0,
    y2: 0.5,
    delta: clampedDelta
  })
  assertCanvasIntegrity(clamped)
  assert.equal(clamped[1].width, 0.04)

  const deleted = deleteCanvasBlock(initial, initial[0].blockId, 'right')
  assert.equal(deleted.length, 3)
  assertCanvasIntegrity(deleted)
  assert.equal(deleted.find((block) => block.productId === 102)?.x, 0)
  assert.equal(deleted.find((block) => block.productId === 102)?.width, 1)
})

test('default average canvas layout does not overlap when row widths require rounding', async () => {
  const { assertCanvasIntegrity, createDefaultCanvasBlocks } = await loadTsModule(
    'src/views/showroom-admin/hall/canvasLayout.ts'
  )
  for (const count of [10, 23]) {
    const blocks = createDefaultCanvasBlocks(
      Array.from({ length: count }, (_, index) => ({
        productId: 2000 + index,
        productCode: `P-${2000 + index}`,
        nameCn: `产品${index + 1}`
      }))
    )
    assert.equal(blocks.length, count)
    assertCanvasIntegrity(blocks)
  }
})

test('vertical edge groups remain movable after horizontal boundaries become misaligned', async () => {
  const { assertCanvasIntegrity, clampCanvasBoundaryDelta, createCanvasBlock, resizeCanvasBoundary } = await loadTsModule(
    'src/views/showroom-admin/hall/canvasLayout.ts'
  )
  const products = [
    { productId: 301, productCode: 'P-301', nameCn: '产品A', previewImageUrl: '' },
    { productId: 302, productCode: 'P-302', nameCn: '产品B', previewImageUrl: '' },
    { productId: 303, productCode: 'P-303', nameCn: '产品C', previewImageUrl: '' },
    { productId: 304, productCode: 'P-304', nameCn: '产品D', previewImageUrl: '' },
    { productId: 305, productCode: 'P-305', nameCn: '产品E', previewImageUrl: '' }
  ]
  const tJunctionBlocks = [
    createCanvasBlock(products[0], 0, { x: 0, y: 0, width: 0.35, height: 0.4 }),
    createCanvasBlock(products[1], 1, { x: 0.35, y: 0, width: 0.35, height: 0.6 }),
    createCanvasBlock(products[2], 2, { x: 0, y: 0.4, width: 0.35, height: 0.6 }),
    createCanvasBlock(products[3], 3, { x: 0.35, y: 0.6, width: 0.35, height: 0.4 }),
    createCanvasBlock(products[4], 4, { x: 0.7, y: 0, width: 0.3, height: 1 })
  ]
  assertCanvasIntegrity(tJunctionBlocks)

  const moved = resizeCanvasBoundary(tJunctionBlocks, {
    orientation: 'vertical',
    x: 0.35,
    y1: 0.4,
    y2: 0.6,
    delta: 0.1
  })

  assertCanvasIntegrity(moved)
  assert.equal(moved[0].width, 0.45)
  assert.equal(moved[1].x, 0.45)
  assert.equal(moved[1].width, 0.25)
  assert.equal(moved[2].width, 0.45)
  assert.equal(moved[3].x, 0.45)
  assert.equal(moved[3].width, 0.25)
  assert.deepEqual(
    { x: moved[4].x, y: moved[4].y, width: moved[4].width, height: moved[4].height },
    { x: 0.7, y: 0, width: 0.3, height: 1 }
  )

  const clampedDelta = clampCanvasBoundaryDelta(tJunctionBlocks, {
    orientation: 'vertical',
    x: 0.35,
    y1: 0.4,
    y2: 0.6,
    delta: 0.5
  })
  assert.equal(clampedDelta, 0.31)
  const clamped = resizeCanvasBoundary(tJunctionBlocks, {
    orientation: 'vertical',
    x: 0.35,
    y1: 0.4,
    y2: 0.6,
    delta: clampedDelta
  })
  assertCanvasIntegrity(clamped)
  assert.equal(clamped[1].width, 0.04)
  assert.equal(clamped[3].width, 0.04)
})
