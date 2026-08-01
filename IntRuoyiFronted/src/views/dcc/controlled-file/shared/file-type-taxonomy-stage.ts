import type { DccFileTypeTaxonomyVO } from '@/api/dcc/controlledFile/fileTypeTaxonomies'

export const DCC_TECHNICAL_DOCUMENT_ROOT_NAME = '技术文档'
export const DCC_UNCLASSIFIED_TAXONOMY_STAGE = '未分类'

export type DccFileTypeTaxonomyStageTagType =
  | 'primary'
  | 'success'
  | 'warning'
  | 'info'
  | 'danger'

export interface DccFileTypeTaxonomyStageOption {
  label: string
  value: string
}

export interface DccFileTypeTaxonomyStageTypeOption {
  label: string
  value: string
  stageName: string
  taxonomyId: number
}

export interface DccFileTypeTaxonomyStageTypeName {
  stageName: string
  typeName: string
  taxonomyId: number
}

type TaxonomyNodeRef = {
  row: DccFileTypeTaxonomyVO
  parentId: number | null
}

type DccFileTypeTaxonomyStageSource =
  | number
  | {
      id?: number | null
      fileTypeTaxonomyId?: number | null
    }
  | null
  | undefined

const normalizeTaxonomyId = (value: unknown) => {
  if (value === null || typeof value === 'undefined' || value === '') {
    return undefined
  }
  const id = Number(value)
  return Number.isFinite(id) ? id : undefined
}

const normalizeName = (value: unknown) => String(value || '').trim()

const compareTaxonomySort = (left: DccFileTypeTaxonomyVO, right: DccFileTypeTaxonomyVO) => {
  const sortDiff = (left.sort || 0) - (right.sort || 0)
  if (sortDiff !== 0) {
    return sortDiff
  }
  const levelDiff = (left.levelNo || 0) - (right.levelNo || 0)
  if (levelDiff !== 0) {
    return levelDiff
  }
  return normalizeName(left.name).localeCompare(normalizeName(right.name), 'zh-Hans-CN')
}

const collectTaxonomyNodes = (
  rows: DccFileTypeTaxonomyVO[],
  parentId: number | null = null,
  result: TaxonomyNodeRef[] = [],
  seenIds = new Set<number>()
) => {
  for (const row of rows) {
    const rowId = normalizeTaxonomyId(row.id)
    if (!rowId) {
      continue
    }
    const declaredParentId = normalizeTaxonomyId(row.parentId)
    const effectiveParentId = declaredParentId ?? parentId
    if (!seenIds.has(rowId)) {
      result.push({ row, parentId: effectiveParentId ?? null })
      seenIds.add(rowId)
    }
    if (row.children?.length) {
      collectTaxonomyNodes(row.children, rowId, result, seenIds)
    }
  }
  return result
}

const getTaxonomyNodeId = (node: TaxonomyNodeRef) => normalizeTaxonomyId(node.row.id)

const getSortedTaxonomyNodes = (nodes: TaxonomyNodeRef[]) =>
  [...nodes].sort((left, right) => compareTaxonomySort(left.row, right.row))

const buildTaxonomyChildrenByParentId = (nodes: TaxonomyNodeRef[]) => {
  const knownIds = new Set(nodes.map((node) => getTaxonomyNodeId(node)).filter(Boolean) as number[])
  const childrenByParentId = new Map<number | null, TaxonomyNodeRef[]>()

  for (const node of nodes) {
    const parentId = node.parentId && knownIds.has(node.parentId) ? node.parentId : null
    const children = childrenByParentId.get(parentId) || []
    children.push(node)
    childrenByParentId.set(parentId, children)
  }

  return childrenByParentId
}

export const buildDccFileTypeTaxonomyPathMap = (rows: DccFileTypeTaxonomyVO[]) => {
  const nodes = collectTaxonomyNodes(rows)
  const pathById = new Map<number, string>()
  const childrenByParentId = buildTaxonomyChildrenByParentId(nodes)

  const visit = (node: TaxonomyNodeRef, parentPath = '') => {
    const id = getTaxonomyNodeId(node)
    const name = normalizeName(node.row.name)
    if (!id || !name) {
      return
    }
    const currentPath = parentPath ? `${parentPath}/${name}` : name
    pathById.set(id, currentPath)
    for (const child of getSortedTaxonomyNodes(childrenByParentId.get(id) || [])) {
      visit(child, currentPath)
    }
  }

  for (const root of getSortedTaxonomyNodes(childrenByParentId.get(null) || [])) {
    visit(root)
  }

  return pathById
}

export const buildDccFileTypeTaxonomyStageTypeNameMap = (
  rows: DccFileTypeTaxonomyVO[],
  rootName = DCC_TECHNICAL_DOCUMENT_ROOT_NAME
) => {
  const pathById = buildDccFileTypeTaxonomyPathMap(rows)
  const typeNameByTaxonomyId = new Map<number, DccFileTypeTaxonomyStageTypeName>()

  pathById.forEach((path, id) => {
    const pathParts = path.split('/').map(normalizeName)
    if (pathParts[0] === rootName && pathParts[1] && pathParts[2]) {
      typeNameByTaxonomyId.set(id, {
        taxonomyId: id,
        stageName: pathParts[1],
        typeName: pathParts[2]
      })
    }
  })

  return typeNameByTaxonomyId
}

export const getDccFileTypeTaxonomyStageRows = (
  rows: DccFileTypeTaxonomyVO[],
  rootName = DCC_TECHNICAL_DOCUMENT_ROOT_NAME
) => {
  const nodes = collectTaxonomyNodes(rows)
  const root = getSortedTaxonomyNodes(nodes).find(
    (node) => !node.parentId && normalizeName(node.row.name) === rootName
  )
  const rootId = root ? getTaxonomyNodeId(root) : undefined
  if (!rootId) {
    return []
  }
  return getSortedTaxonomyNodes(nodes.filter((node) => node.parentId === rootId)).map((node) => node.row)
}

export const buildDccFileTypeTaxonomyStageNameMap = (
  rows: DccFileTypeTaxonomyVO[],
  rootName = DCC_TECHNICAL_DOCUMENT_ROOT_NAME
) => {
  const pathById = buildDccFileTypeTaxonomyPathMap(rows)
  const stageNameByTaxonomyId = new Map<number, string>()

  pathById.forEach((path, id) => {
    const pathParts = path.split('/').map(normalizeName)
    if (pathParts[0] === rootName && pathParts[1]) {
      stageNameByTaxonomyId.set(id, pathParts[1])
    }
  })

  return stageNameByTaxonomyId
}

export const buildDccFileTypeTaxonomyStageTypeOptionsMap = (
  rows: DccFileTypeTaxonomyVO[],
  rootName = DCC_TECHNICAL_DOCUMENT_ROOT_NAME
) => {
  const nodes = collectTaxonomyNodes(rows)
  const childrenByParentId = buildTaxonomyChildrenByParentId(nodes)
  const root = getSortedTaxonomyNodes(childrenByParentId.get(null) || []).find(
    (node) => normalizeName(node.row.name) === rootName
  )
  const rootId = root ? getTaxonomyNodeId(root) : undefined
  const optionsByStageName = new Map<string, DccFileTypeTaxonomyStageTypeOption[]>()
  if (!rootId) {
    return optionsByStageName
  }

  for (const stageNode of getSortedTaxonomyNodes(childrenByParentId.get(rootId) || [])) {
    const stageId = getTaxonomyNodeId(stageNode)
    const stageName = normalizeName(stageNode.row.name)
    if (!stageId || !stageName) {
      continue
    }
    const typeOptions = getSortedTaxonomyNodes(childrenByParentId.get(stageId) || [])
      .map((typeNode) => {
        const taxonomyId = getTaxonomyNodeId(typeNode)
        const typeName = normalizeName(typeNode.row.name)
        return taxonomyId && typeName
          ? {
              label: typeName,
              value: typeName,
              stageName,
              taxonomyId
            }
          : undefined
      })
      .filter((option): option is DccFileTypeTaxonomyStageTypeOption => Boolean(option))
    optionsByStageName.set(stageName, typeOptions)
  }

  return optionsByStageName
}

export const toDccFileTypeTaxonomyStageOptions = (
  rows: DccFileTypeTaxonomyVO[]
): DccFileTypeTaxonomyStageOption[] =>
  rows.map((row) => ({
    label: row.name,
    value: row.name
  }))

export const resolveDccFileTypeTaxonomyStageName = (
  source: DccFileTypeTaxonomyStageSource,
  stageNameByTaxonomyId: Map<number, string>
) => {
  const taxonomyId =
    typeof source === 'number'
      ? normalizeTaxonomyId(source)
      : normalizeTaxonomyId(source?.fileTypeTaxonomyId ?? source?.id)
  return taxonomyId ? stageNameByTaxonomyId.get(taxonomyId) : undefined
}

export const resolveDccFileTypeTaxonomyStageTypeName = (
  source: DccFileTypeTaxonomyStageSource,
  typeNameByTaxonomyId: Map<number, DccFileTypeTaxonomyStageTypeName>
) => {
  const taxonomyId =
    typeof source === 'number'
      ? normalizeTaxonomyId(source)
      : normalizeTaxonomyId(source?.fileTypeTaxonomyId ?? source?.id)
  return taxonomyId ? typeNameByTaxonomyId.get(taxonomyId) : undefined
}

export const getDccFileTypeTaxonomyStageTagType = (
  stageName?: string | null
): DccFileTypeTaxonomyStageTagType => {
  const stageTypeMap: Record<string, DccFileTypeTaxonomyStageTagType> = {
    清单: 'info',
    设计和开发策划阶段: 'primary',
    设计和开发输入阶段: 'success',
    设计和开发输出阶段: 'info',
    设计和开发验证: 'warning',
    设计确认: 'danger',
    设计和开发转换阶段: 'primary',
    注册资料汇编: 'success',
    设计和开发变更: 'warning'
  }
  const normalizedStageName = normalizeName(stageName)
  return normalizedStageName ? stageTypeMap[normalizedStageName] ?? 'info' : 'info'
}
