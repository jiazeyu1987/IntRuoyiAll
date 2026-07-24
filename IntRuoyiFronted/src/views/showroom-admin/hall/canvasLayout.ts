export interface CanvasProductOption {
  itemType: 'PRODUCT' | 'AWARD'
  itemId: number
  itemKey: string
  productId: number
  productCode: string
  nameCn: string
  previewImageUrl: string
}

export interface CanvasBlock {
  blockId: string
  itemType: 'PRODUCT' | 'AWARD'
  itemId: number
  itemKey: string
  productId: number
  productCode: string
  productName: string
  previewImageUrl: string
  x: number
  y: number
  width: number
  height: number
}

export interface CanvasBoundary {
  orientation: 'vertical' | 'horizontal'
  x?: number
  y?: number
  x1?: number
  x2?: number
  y1?: number
  y2?: number
  delta: number
}

export type CanvasMergeDirection = 'left' | 'right' | 'top' | 'bottom'

const EPSILON = 0.000001
const MIN_BLOCK_SIZE = 0.04

export const roundCanvasNumber = (value: number) => Number(value.toFixed(6))

export const createCanvasBlock = (
  product: CanvasProductOption,
  index: number,
  rect: Pick<CanvasBlock, 'x' | 'y' | 'width' | 'height'>
): CanvasBlock => {
  return {
    blockId: `block-${product.itemKey}-${index}`,
    itemType: product.itemType,
    itemId: product.itemId,
    itemKey: product.itemKey,
    productId: product.productId,
    productCode: product.productCode,
    productName: product.nameCn || product.productCode,
    previewImageUrl: product.previewImageUrl || '',
    x: roundCanvasNumber(rect.x),
    y: roundCanvasNumber(rect.y),
    width: roundCanvasNumber(rect.width),
    height: roundCanvasNumber(rect.height)
  }
}

export const createDefaultCanvasBlocks = (products: CanvasProductOption[]): CanvasBlock[] => {
  if (!Array.isArray(products) || products.length === 0) {
    return []
  }
  const rows = Math.max(1, Math.floor(Math.sqrt(products.length)))
  const columns = Math.ceil(products.length / rows)
  const blocks: CanvasBlock[] = []
  let index = 0
  for (let row = 0; row < rows && index < products.length; row++) {
    const remaining = products.length - index
    const rowCount = Math.min(columns, remaining)
    const y = roundCanvasNumber(row / rows)
    const nextY = roundCanvasNumber((row + 1) / rows)
    const height = roundCanvasNumber(nextY - y)
    for (let column = 0; column < rowCount; column++) {
      const x = roundCanvasNumber(column / rowCount)
      const nextX = roundCanvasNumber((column + 1) / rowCount)
      blocks.push(
        createCanvasBlock(products[index], index, {
          x,
          y,
          width: roundCanvasNumber(nextX - x),
          height
        })
      )
      index += 1
    }
  }
  assertCanvasIntegrity(blocks)
  return blocks
}

export const assertCanvasIntegrity = (blocks: CanvasBlock[]) => {
  if (!Array.isArray(blocks)) {
    throw new Error('画布布局必须是产品块数组')
  }
  let area = 0
  blocks.forEach((block, index) => {
    assertFiniteRect(block)
    if (block.x < -EPSILON || block.y < -EPSILON) {
      throw new Error(`第 ${index + 1} 个产品块超出画布左上边界`)
    }
    if (block.width <= EPSILON || block.height <= EPSILON) {
      throw new Error(`第 ${index + 1} 个产品块宽高必须大于 0`)
    }
    if (block.x + block.width > 1 + EPSILON || block.y + block.height > 1 + EPSILON) {
      throw new Error(`第 ${index + 1} 个产品块超出画布右下边界`)
    }
    area += block.width * block.height
    for (let otherIndex = index + 1; otherIndex < blocks.length; otherIndex++) {
      if (rectanglesOverlap(block, blocks[otherIndex])) {
        throw new Error('画布产品块不能重叠')
      }
    }
  })
  if (blocks.length > 0 && Math.abs(area - 1) > EPSILON) {
    throw new Error('画布产品块必须完整铺满展柜')
  }
}

export const splitCanvasBlock = (
  blocks: CanvasBlock[],
  blockId: string,
  product: CanvasProductOption
): CanvasBlock[] => {
  const targetIndex = blocks.findIndex((block) => block.blockId === blockId)
  if (targetIndex < 0) {
    throw new Error('请选择要切分的产品块')
  }
  const target = blocks[targetIndex]
  if (Math.min(target.width, target.height) < MIN_BLOCK_SIZE * 2) {
    throw new Error('产品块过小，无法继续切分')
  }
  const next = cloneBlocks(blocks)
  const original = { ...target }
  let created: CanvasBlock
  if (target.width >= target.height) {
    const half = roundCanvasNumber(target.width / 2)
    original.width = half
    created = createCanvasBlock(product, Date.now(), {
      x: target.x + half,
      y: target.y,
      width: target.width - half,
      height: target.height
    })
  } else {
    const half = roundCanvasNumber(target.height / 2)
    original.height = half
    created = createCanvasBlock(product, Date.now(), {
      x: target.x,
      y: target.y + half,
      width: target.width,
      height: target.height - half
    })
  }
  next.splice(targetIndex, 1, normalizeBlock(original), created)
  assertCanvasIntegrity(next)
  return next
}

export const deleteCanvasBlock = (
  blocks: CanvasBlock[],
  blockId: string,
  preferredDirection?: CanvasMergeDirection
): CanvasBlock[] => {
  const target = blocks.find((block) => block.blockId === blockId)
  if (!target) {
    throw new Error('请选择要删除的产品块')
  }
  if (blocks.length <= 1) {
    throw new Error('展柜至少保留一个产品块')
  }
  const directions: CanvasMergeDirection[] = preferredDirection
    ? [preferredDirection]
    : sortMergeDirections(blocks, target)
  for (const direction of directions) {
    try {
      const next = expandNeighborsIntoDeletedBlock(blocks, target, direction)
      assertCanvasIntegrity(next)
      return next
    } catch (error) {
      if (preferredDirection) {
        throw error
      }
    }
  }
  throw new Error('删除后无法用相邻产品块填补空位')
}

export const swapCanvasBlockProducts = (
  blocks: CanvasBlock[],
  sourceBlockId: string,
  targetBlockId: string
): CanvasBlock[] => {
  if (sourceBlockId === targetBlockId) {
    return cloneBlocks(blocks)
  }
  const next = cloneBlocks(blocks)
  const source = next.find((block) => block.blockId === sourceBlockId)
  const target = next.find((block) => block.blockId === targetBlockId)
  if (!source || !target) {
    throw new Error('请选择两个有效产品块')
  }
  const sourceProduct = readProductBinding(source)
  Object.assign(source, readProductBinding(target))
  Object.assign(target, sourceProduct)
  assertCanvasIntegrity(next)
  return next
}

export const clampCanvasBoundaryDelta = (blocks: CanvasBlock[], boundary: CanvasBoundary): number => {
  const groups = findResizeGroups(blocks, boundary)
  let minDelta = Number.NEGATIVE_INFINITY
  let maxDelta = Number.POSITIVE_INFINITY
  if (groups.orientation === 'vertical') {
    groups.leftBlocks.forEach((block) => {
      minDelta = Math.max(minDelta, MIN_BLOCK_SIZE - block.width)
    })
    groups.rightBlocks.forEach((block) => {
      maxDelta = Math.min(maxDelta, block.width - MIN_BLOCK_SIZE)
    })
  } else {
    groups.topBlocks.forEach((block) => {
      minDelta = Math.max(minDelta, MIN_BLOCK_SIZE - block.height)
    })
    groups.bottomBlocks.forEach((block) => {
      maxDelta = Math.min(maxDelta, block.height - MIN_BLOCK_SIZE)
    })
  }
  if (minDelta > maxDelta) {
    throw new Error('产品块不能小于最小尺寸')
  }
  return roundCanvasNumber(Math.min(Math.max(boundary.delta, minDelta), maxDelta))
}

export const resizeCanvasBoundary = (blocks: CanvasBlock[], boundary: CanvasBoundary): CanvasBlock[] => {
  const next = cloneBlocks(blocks)
  const groups = findResizeGroups(next, boundary)
  if (groups.orientation === 'vertical') {
    groups.leftBlocks.forEach((block) => {
      block.width = roundCanvasNumber(block.width + boundary.delta)
    })
    groups.rightBlocks.forEach((block) => {
      block.x = roundCanvasNumber(block.x + boundary.delta)
      block.width = roundCanvasNumber(block.width - boundary.delta)
    })
  } else {
    groups.topBlocks.forEach((block) => {
      block.height = roundCanvasNumber(block.height + boundary.delta)
    })
    groups.bottomBlocks.forEach((block) => {
      block.y = roundCanvasNumber(block.y + boundary.delta)
      block.height = roundCanvasNumber(block.height - boundary.delta)
    })
  }
  if (next.some((block) => block.width < MIN_BLOCK_SIZE || block.height < MIN_BLOCK_SIZE)) {
    throw new Error('产品块不能小于最小尺寸')
  }
  assertCanvasIntegrity(next)
  return next.map(normalizeBlock)
}

export const buildCanvasLayoutPayload = (hallId: number, blocks: CanvasBlock[]) => {
  assertHallCanvasLayoutPayloadIntegrity(blocks)
  return {
    hallId,
    items: blocks.map((block, index) => ({
      itemType: block.itemType,
      itemId: block.itemId,
      displayOrder: index + 1,
      layoutX: roundCanvasNumber(block.x),
      layoutY: roundCanvasNumber(block.y),
      layoutWidth: roundCanvasNumber(block.width),
      layoutHeight: roundCanvasNumber(block.height)
    }))
  }
}

const assertHallCanvasLayoutPayloadIntegrity = (blocks: CanvasBlock[]) => {
  if (!Array.isArray(blocks)) {
    throw new Error('画布布局必须是展项块数组')
  }
  const productBlocks = blocks.filter((block) => block.itemType === 'PRODUCT')
  if (productBlocks.length > 0) {
    assertCanvasIntegrity(productBlocks)
  }
  blocks
    .filter((block) => block.itemType !== 'PRODUCT')
    .forEach((block, index) => {
      assertFiniteRect(block)
      if (block.x < -EPSILON || block.y < -EPSILON) {
        throw new Error(`第 ${index + 1} 个奖项块超出画布左上边界`)
      }
      if (block.width <= EPSILON || block.height <= EPSILON) {
        throw new Error(`第 ${index + 1} 个奖项块宽高必须大于 0`)
      }
      if (block.x + block.width > 1 + EPSILON || block.y + block.height > 1 + EPSILON) {
        throw new Error(`第 ${index + 1} 个奖项块超出画布右下边界`)
      }
    })
}

const expandNeighborsIntoDeletedBlock = (
  blocks: CanvasBlock[],
  target: CanvasBlock,
  direction: CanvasMergeDirection
) => {
  const next = blocks.filter((block) => block.blockId !== target.blockId).map((block) => ({ ...block }))
  const neighbors = findNeighbors(next, target, direction)
  if (neighbors.length === 0) {
    throw new Error('删除方向没有相邻产品块')
  }
  neighbors.forEach((block) => {
    if (direction === 'right') {
      block.x = target.x
      block.width += target.width
    } else if (direction === 'left') {
      block.width += target.width
    } else if (direction === 'bottom') {
      block.y = target.y
      block.height += target.height
    } else {
      block.height += target.height
    }
  })
  return next.map(normalizeBlock)
}

const sortMergeDirections = (blocks: CanvasBlock[], target: CanvasBlock): CanvasMergeDirection[] => {
  return (['right', 'bottom', 'left', 'top'] as CanvasMergeDirection[])
    .map((direction) => ({ direction, count: findNeighbors(blocks, target, direction).length }))
    .filter((item) => item.count > 0)
    .sort((left, right) => left.count - right.count)
    .map((item) => item.direction)
}

const findNeighbors = (blocks: CanvasBlock[], target: CanvasBlock, direction: CanvasMergeDirection) => {
  return blocks.filter((block) => {
    if (direction === 'right') {
      return sameEdge(block.x, target.x + target.width) && overlaps1d(block.y, block.y + block.height, target.y, target.y + target.height)
    }
    if (direction === 'left') {
      return sameEdge(block.x + block.width, target.x) && overlaps1d(block.y, block.y + block.height, target.y, target.y + target.height)
    }
    if (direction === 'bottom') {
      return sameEdge(block.y, target.y + target.height) && overlaps1d(block.x, block.x + block.width, target.x, target.x + target.width)
    }
    return sameEdge(block.y + block.height, target.y) && overlaps1d(block.x, block.x + block.width, target.x, target.x + target.width)
  })
}

const findResizeGroups = (blocks: CanvasBlock[], boundary: CanvasBoundary) => {
  if (boundary.orientation === 'vertical') {
    const x = requireBoundaryNumber(boundary.x, 'x')
    const y1 = requireBoundaryNumber(boundary.y1, 'y1')
    const y2 = requireBoundaryNumber(boundary.y2, 'y2')
    const groupRange = expandVerticalResizeRange(blocks, x, y1, y2)
    const leftBlocks = blocks.filter((block) =>
      sameEdge(block.x + block.width, x) &&
      overlaps1d(block.y, block.y + block.height, groupRange.y1, groupRange.y2)
    )
    const rightBlocks = blocks.filter((block) =>
      sameEdge(block.x, x) &&
      overlaps1d(block.y, block.y + block.height, groupRange.y1, groupRange.y2)
    )
    if (leftBlocks.length === 0 || rightBlocks.length === 0) {
      throw new Error('未找到可调整的相邻产品块')
    }
    return { orientation: 'vertical' as const, leftBlocks, rightBlocks }
  }
  const y = requireBoundaryNumber(boundary.y, 'y')
  const x1 = requireBoundaryNumber(boundary.x1, 'x1')
  const x2 = requireBoundaryNumber(boundary.x2, 'x2')
  const groupRange = expandHorizontalResizeRange(blocks, y, x1, x2)
  const topBlocks = blocks.filter((block) =>
    sameEdge(block.y + block.height, y) &&
    overlaps1d(block.x, block.x + block.width, groupRange.x1, groupRange.x2)
  )
  const bottomBlocks = blocks.filter((block) =>
    sameEdge(block.y, y) &&
    overlaps1d(block.x, block.x + block.width, groupRange.x1, groupRange.x2)
  )
  if (topBlocks.length === 0 || bottomBlocks.length === 0) {
    throw new Error('未找到可调整的相邻产品块')
  }
  return { orientation: 'horizontal' as const, topBlocks, bottomBlocks }
}

const expandVerticalResizeRange = (blocks: CanvasBlock[], x: number, initialY1: number, initialY2: number) => {
  let y1 = Math.min(initialY1, initialY2)
  let y2 = Math.max(initialY1, initialY2)
  let changed = true
  while (changed) {
    changed = false
    blocks.forEach((block) => {
      const touchesBoundary = sameEdge(block.x + block.width, x) || sameEdge(block.x, x)
      if (!touchesBoundary || !overlaps1d(block.y, block.y + block.height, y1, y2)) {
        return
      }
      if (block.y < y1 - EPSILON) {
        y1 = block.y
        changed = true
      }
      const blockBottom = block.y + block.height
      if (blockBottom > y2 + EPSILON) {
        y2 = blockBottom
        changed = true
      }
    })
  }
  return { y1: roundCanvasNumber(y1), y2: roundCanvasNumber(y2) }
}

const expandHorizontalResizeRange = (blocks: CanvasBlock[], y: number, initialX1: number, initialX2: number) => {
  let x1 = Math.min(initialX1, initialX2)
  let x2 = Math.max(initialX1, initialX2)
  let changed = true
  while (changed) {
    changed = false
    blocks.forEach((block) => {
      const touchesBoundary = sameEdge(block.y + block.height, y) || sameEdge(block.y, y)
      if (!touchesBoundary || !overlaps1d(block.x, block.x + block.width, x1, x2)) {
        return
      }
      if (block.x < x1 - EPSILON) {
        x1 = block.x
        changed = true
      }
      const blockRight = block.x + block.width
      if (blockRight > x2 + EPSILON) {
        x2 = blockRight
        changed = true
      }
    })
  }
  return { x1: roundCanvasNumber(x1), x2: roundCanvasNumber(x2) }
}

const normalizeBlock = (block: CanvasBlock): CanvasBlock => ({
  ...block,
  x: roundCanvasNumber(block.x),
  y: roundCanvasNumber(block.y),
  width: roundCanvasNumber(block.width),
  height: roundCanvasNumber(block.height)
})

const cloneBlocks = (blocks: CanvasBlock[]) => blocks.map((block) => ({ ...block }))

const readProductBinding = (block: CanvasBlock) => ({
  itemType: block.itemType,
  itemId: block.itemId,
  itemKey: block.itemKey,
  productId: block.productId,
  productCode: block.productCode,
  productName: block.productName,
  previewImageUrl: block.previewImageUrl
})

const rectanglesOverlap = (left: CanvasBlock, right: CanvasBlock) => {
  return (
    Math.min(left.x + left.width, right.x + right.width) - Math.max(left.x, right.x) > EPSILON &&
    Math.min(left.y + left.height, right.y + right.height) - Math.max(left.y, right.y) > EPSILON
  )
}

const overlaps1d = (a1: number, a2: number, b1: number, b2: number) => {
  return Math.min(a2, b2) - Math.max(a1, b1) > EPSILON
}

const sameEdge = (left: number, right: number) => Math.abs(left - right) <= EPSILON

const requireBoundaryNumber = (value: number | undefined, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`边界缺少 ${fieldName}`)
  }
  return value
}

const assertFiniteRect = (block: CanvasBlock) => {
  for (const value of [block.x, block.y, block.width, block.height]) {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
      throw new Error('产品块坐标必须是有效数字')
    }
  }
}
