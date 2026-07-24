export interface ShowroomHallProductMapping {
  itemType: 'PRODUCT' | 'AWARD'
  itemId: number
  productId: number
  displayOrder: number
  layoutX: number | null
  layoutY: number | null
  layoutWidth: number | null
  layoutHeight: number | null
}

export interface ShowroomHallRecord {
  hallId: number
  hallCode: string
  name: string
  nameEn: string
  description: string
  descriptionEn: string
  canvasBackgroundImageUrl: string
  productMappings: ShowroomHallProductMapping[]
  productCount: number
}

export interface HallEditorForm {
  hallCode: string
  name: string
  nameEn: string
  description: string
  descriptionEn: string
}

export interface HallProductOption {
  itemType: 'PRODUCT' | 'AWARD'
  itemId: number
  itemKey: string
  productId: number
  productMasterId: number | null
  productCode: string
  nameCn: string
  revisionNo: number
  incomplete: boolean
  previewImageUrl: string
  hallIds: number[]
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜工作台缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`展柜工作台缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`展柜工作台缺少数值字段：${fieldName}`)
  }
  return value
}

const expectNullableNumber = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return expectNumber(value, fieldName)
}

const readOptionalString = (value: unknown) => (typeof value === 'string' ? value : '')

const readOptionalRecord = (value: unknown): Record<string, unknown> | null => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null
  }
  return value as Record<string, unknown>
}

const resolveProductPreviewImageUrl = (product: Record<string, unknown>, revision: Record<string, unknown>) => {
  const displayDetail = readOptionalRecord(product.displayDetail)
  const detail = readOptionalRecord(product.detail)
  const displayFields = readOptionalRecord(displayDetail?.fields)
  const detailFields = readOptionalRecord(detail?.fields)
  return (
    readOptionalString(product.coverImageUrl) ||
    readOptionalString(revision.coverImageUrl) ||
    readOptionalString(displayDetail?.coverImage) ||
    readOptionalString(detail?.coverImage) ||
    readOptionalString(displayFields?.cover_image) ||
    readOptionalString(displayFields?.coverImage) ||
    readOptionalString(detailFields?.cover_image) ||
    readOptionalString(detailFields?.coverImage)
  )
}

export const normalizeHallRecord = (value: unknown): ShowroomHallRecord => {
  const record = expectRecord(value, 'hall')
  const rawMappings = record.itemMappings
  if (!Array.isArray(rawMappings)) {
    throw new Error('展柜工作台缺少数组字段：itemMappings')
  }
  return {
    hallId: expectNumber(record.hallId, 'hallId'),
    hallCode: expectString(record.hallCode, 'hallCode'),
    name: expectString(record.name, 'name'),
    nameEn: expectString(record.nameEn, 'nameEn'),
    description: expectString(record.description, 'description', true),
    descriptionEn: expectString(record.descriptionEn, 'descriptionEn', true),
    canvasBackgroundImageUrl: expectString(record.canvasBackgroundImageUrl, 'canvasBackgroundImageUrl', true),
    productMappings: rawMappings.map((mapping, index) => {
      const mappingRecord = expectRecord(mapping, `itemMappings[${index}]`)
      const itemType = expectString(mappingRecord.itemType, `itemMappings[${index}].itemType`) as 'PRODUCT' | 'AWARD'
      const itemId = expectNumber(mappingRecord.itemId, `itemMappings[${index}].itemId`)
      if (!['PRODUCT', 'AWARD'].includes(itemType)) {
        throw new Error(`展柜工作台展项类型非法：itemMappings[${index}].itemType`)
      }
      return {
        itemType,
        itemId,
        productId: itemType === 'PRODUCT' ? itemId : 0,
        displayOrder: expectNumber(mappingRecord.displayOrder, `itemMappings[${index}].displayOrder`),
        layoutX: typeof mappingRecord.layoutX === 'number' ? mappingRecord.layoutX : null,
        layoutY: typeof mappingRecord.layoutY === 'number' ? mappingRecord.layoutY : null,
        layoutWidth: typeof mappingRecord.layoutWidth === 'number' ? mappingRecord.layoutWidth : null,
        layoutHeight: typeof mappingRecord.layoutHeight === 'number' ? mappingRecord.layoutHeight : null
      }
    }),
    productCount: expectNumber(record.productCount, 'productCount')
  }
}

export const createHallEditorForm = (hall?: ShowroomHallRecord | null): HallEditorForm => {
  return {
    hallCode: hall?.hallCode || '',
    name: hall?.name || '',
    nameEn: hall?.nameEn || '',
    description: hall?.description || '',
    descriptionEn: hall?.descriptionEn || ''
  }
}

export const buildHallSavePayload = (hallId: number | undefined, form: HallEditorForm) => {
  const payload = {
    hallCode: form.hallCode.trim(),
    name: form.name.trim(),
    nameEn: form.nameEn.trim(),
    description: form.description.trim(),
    descriptionEn: form.descriptionEn.trim()
  }
  if (!payload.hallCode || !payload.name || !payload.nameEn) {
    throw new Error('展柜编码、展柜名称、英文名称为必填项')
  }
  if (hallId) {
    return {
      hallId,
      name: payload.name,
      nameEn: payload.nameEn,
      description: payload.description,
      descriptionEn: payload.descriptionEn
    }
  }
  return payload
}

export const normalizeProductOptions = (products: unknown[]): HallProductOption[] => {
  if (!Array.isArray(products)) {
    throw new Error('展柜映射缺少真实产品数组：products')
  }
  return products.map((item, index) => {
    const product = expectRecord(item, `products[${index}]`)
    const revision = expectRecord(product.revision, `products[${index}].revision`)
    return {
      productId: expectNumber(product.productId, `products[${index}].productId`),
      itemType: 'PRODUCT',
      itemId: expectNumber(product.productId, `products[${index}].productId`),
      itemKey: `PRODUCT:${expectNumber(product.productId, `products[${index}].productId`)}`,
      productMasterId: expectNullableNumber(product.productMasterId, `products[${index}].productMasterId`),
      productCode: expectString(product.productCode, `products[${index}].productCode`),
      nameCn: expectString(revision.nameCn, `products[${index}].revision.nameCn`, true),
      revisionNo: expectNumber(revision.revisionNo, `products[${index}].revision.revisionNo`),
      incomplete: typeof product.incomplete === 'boolean' ? product.incomplete : false,
      previewImageUrl: resolveProductPreviewImageUrl(product, revision),
      hallIds: []
    }
  })
}

export const normalizeHallProductCandidateOptions = (products: unknown[]): HallProductOption[] => {
  if (!Array.isArray(products)) {
    throw new Error('展柜候选接口缺少真实产品数组：products')
  }
  return products.map((item, index) => {
    const product = expectRecord(item, `products[${index}]`)
    const rawHallIds = product.hallIds
    if (!Array.isArray(rawHallIds)) {
      throw new Error(`展柜候选接口缺少真实展柜数组：products[${index}].hallIds`)
    }
    const itemType = (readOptionalString(product.itemType) || 'PRODUCT') as 'PRODUCT' | 'AWARD'
    const itemId = typeof product.itemId === 'number'
        ? expectNumber(product.itemId, `products[${index}].itemId`)
        : expectNumber(product.productId, `products[${index}].productId`)
    return {
      itemType,
      itemId,
      itemKey: `${itemType}:${itemId}`,
      productId: typeof product.productId === 'number' ? product.productId : 0,
      productMasterId: expectNullableNumber(product.productMasterId, `products[${index}].productMasterId`),
      productCode: expectString(product.productCode ?? product.itemCode, `products[${index}].productCode`),
      nameCn: expectString(product.nameCn, `products[${index}].nameCn`, true),
      revisionNo: expectNumber(product.revisionNo, `products[${index}].revisionNo`),
      incomplete: typeof product.incomplete === 'boolean' ? product.incomplete : false,
      previewImageUrl: expectString(product.previewImageUrl ?? '', `products[${index}].previewImageUrl`, true),
      hallIds: rawHallIds.map((hallId, hallIndex) =>
        expectNumber(hallId, `products[${index}].hallIds[${hallIndex}]`)
      )
    }
  })
}

export const createSelectedHallProductIds = (hall: ShowroomHallRecord): string[] => {
  return [...hall.productMappings]
    .sort((left, right) => left.displayOrder - right.displayOrder)
    .map((mapping) => `${mapping.itemType}:${mapping.itemId}`)
}

const createDefaultLayoutForMapping = (index: number, total: number) => {
  const columns = Math.min(6, Math.max(1, Math.ceil(Math.sqrt(total * 1.6))))
  const rows = Math.max(1, Math.ceil(total / columns))
  const column = index % columns
  const row = Math.floor(index / columns)
  const width = Math.floor((1 / columns) * 1_000_000) / 1_000_000
  const height = Math.floor((1 / rows) * 1_000_000) / 1_000_000
  const layoutX = Number((column * width).toFixed(6))
  const layoutY = Number((row * height).toFixed(6))
  return {
    layoutX,
    layoutY,
    layoutWidth: column === columns - 1 ? Number((1 - layoutX).toFixed(6)) : width,
    layoutHeight: row === rows - 1 ? Number((1 - layoutY).toFixed(6)) : height
  }
}

const hasCompleteNonOverlappingLayout = (
  items: Array<{
    layoutX: number | null
    layoutY: number | null
    layoutWidth: number | null
    layoutHeight: number | null
  }>
) => {
  const blocks = items.map((item) => {
    if (
      item.layoutX === null ||
      item.layoutY === null ||
      item.layoutWidth === null ||
      item.layoutHeight === null ||
      item.layoutWidth <= 0 ||
      item.layoutHeight <= 0
    ) {
      return null
    }
    return {
      x1: item.layoutX,
      y1: item.layoutY,
      x2: item.layoutX + item.layoutWidth,
      y2: item.layoutY + item.layoutHeight
    }
  })
  if (blocks.some((block) => block === null)) {
    return false
  }
  const totalArea = items.reduce((sum, item) => {
    return sum + (item.layoutWidth || 0) * (item.layoutHeight || 0)
  }, 0)
  if (Math.abs(totalArea - 1) > 0.000001) {
    return false
  }
  for (let leftIndex = 0; leftIndex < blocks.length; leftIndex += 1) {
    const left = blocks[leftIndex]!
    for (let rightIndex = leftIndex + 1; rightIndex < blocks.length; rightIndex += 1) {
      const right = blocks[rightIndex]!
      const overlaps =
        left.x1 < right.x2 &&
        left.x2 > right.x1 &&
        left.y1 < right.y2 &&
        left.y2 > right.y1
      if (overlaps) {
        return false
      }
    }
  }
  return true
}

export const buildHallMappingPayload = (hall: ShowroomHallRecord, itemKeys: string[]) => {
  if (!Array.isArray(itemKeys) || itemKeys.length === 0) {
    throw new Error('至少需要一条展项映射')
  }
  const existingMappings = new Map(
    hall.productMappings.map((mapping) => [`${mapping.itemType}:${mapping.itemId}`, mapping])
  )
  const seenItemKeys = new Set<string>()
  const items = itemKeys.map((itemKey, index) => {
    const [itemType, rawItemId] = String(itemKey).split(':')
    const itemId = Number(rawItemId)
    if (!['PRODUCT', 'AWARD'].includes(itemType) || !Number.isFinite(itemId)) {
      throw new Error(`第 ${index + 1} 个展项缺少有效 itemType/itemId`)
    }
    if (seenItemKeys.has(itemKey)) {
      throw new Error(`展项映射存在重复：${itemKey}`)
    }
    seenItemKeys.add(itemKey)
    const existing = existingMappings.get(itemKey)
    const defaultLayout = createDefaultLayoutForMapping(index, itemKeys.length)
    return {
      itemType,
      itemId,
      displayOrder: index + 1,
      layoutX: existing?.layoutX ?? defaultLayout.layoutX,
      layoutY: existing?.layoutY ?? defaultLayout.layoutY,
      layoutWidth: existing?.layoutWidth ?? defaultLayout.layoutWidth,
      layoutHeight: existing?.layoutHeight ?? defaultLayout.layoutHeight
    }
  })
  if (!hasCompleteNonOverlappingLayout(items)) {
    items.forEach((item, index) => {
      const defaultLayout = createDefaultLayoutForMapping(index, itemKeys.length)
      item.layoutX = defaultLayout.layoutX
      item.layoutY = defaultLayout.layoutY
      item.layoutWidth = defaultLayout.layoutWidth
      item.layoutHeight = defaultLayout.layoutHeight
    })
  }
  return {
    hallId: hall.hallId,
    items
  }
}
