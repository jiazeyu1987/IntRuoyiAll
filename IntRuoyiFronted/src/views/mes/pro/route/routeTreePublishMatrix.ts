import type { TreeChange, TreePreview } from '@/api/mes/pro/route/tree'

type MatrixNode = TreePreview['affected'][number]
export interface RouteTreeMatrixRow {
  field: string
  cells: Map<string, TreeChange[]>
}

// 同类配置占一行；同一子节点中的多个工序变更保留在各自单元格内，避免同名工序互相覆盖。
const rowField = (field: string) => {
  const path = field.replace(/^configSnapshots\./, '')
  if (path.startsWith('flowGraph.')) return 'flowGraph'
  if (path.startsWith('productionProcessConfigs.')) {
    for (const key of ['parameterRules', 'deviceSelectionGroups', 'lossReasons', 'overagePercent']) {
      if (path.split('.').includes(key)) return key
    }
  }
  return path.split('.')[0]
}

export const buildRouteTreePublishMatrix = (nodes: MatrixNode[]): RouteTreeMatrixRow[] => {
  const rows = new Map<string, RouteTreeMatrixRow>()
  for (const node of nodes) {
    if (node.blocker) continue
    for (const change of node.changes) {
      const field = rowField(change.field)
      let row = rows.get(field)
      if (!row) {
        row = { field, cells: new Map() }
        rows.set(field, row)
      }
      const nodeId = String(node.routeId)
      const cell = row.cells.get(nodeId) || []
      cell.push(change)
      row.cells.set(nodeId, cell)
    }
  }
  return [...rows.values()]
}
