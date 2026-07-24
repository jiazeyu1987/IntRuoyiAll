import { handleTree } from '@/utils/tree'
import { ProWorkOrderApi } from '@/api/mes/pro/workorder'

const MAX_SCOPE_PAGE_SIZE = 200

function collectWorkOrderIds(rows: any[]): number[] {
  const result: number[] = []
  const walk = (list: any[]) => {
    list.forEach((row) => {
      if (row?.id) {
        result.push(row.id)
      }
      if (row?.children?.length) {
        walk(row.children)
      }
    })
  }
  walk(rows || [])
  return result
}

export async function loadScopedWorkOrderIds(params: Record<string, any>): Promise<number[]> {
  const scopedParams = {
    ...params,
    temporaryFrozen: params.temporaryFrozen ?? false
  }
  const firstPage = await ProWorkOrderApi.getWorkOrderPage({
    ...scopedParams,
    pageNo: 1,
    pageSize: MAX_SCOPE_PAGE_SIZE
  })
  const total = firstPage?.total || 0
  if (!total) {
    return []
  }

  const mergedRows = [...(firstPage.list || [])]
  const totalPages = Math.ceil(total / MAX_SCOPE_PAGE_SIZE)
  for (let pageNo = 2; pageNo <= totalPages; pageNo += 1) {
    const page = await ProWorkOrderApi.getWorkOrderPage({
      ...scopedParams,
      pageNo,
      pageSize: MAX_SCOPE_PAGE_SIZE
    })
    mergedRows.push(...(page.list || []))
  }

  return collectWorkOrderIds(handleTree(mergedRows, 'id', 'parentId'))
}
