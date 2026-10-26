export interface HistorySource<T> {
  load: (pageNo: number, pageSize: number) => Promise<{ list: T[]; total: number }>
}

// 各类型沿用服务端排序；按类型顺序衔接分页，只读取当前页涉及的记录。
export async function loadHistoryPage<T>(
  sources: HistorySource<T>[],
  pageNo: number,
  pageSize: number
) {
  const heads = await Promise.all(sources.map((source) => source.load(1, pageSize)))
  const total = heads.reduce((sum, page) => sum + page.total, 0)
  const start = (pageNo - 1) * pageSize
  let sourceStart = 0
  const rows: T[] = []
  for (let index = 0; index < sources.length; index++) {
    const head = heads[index]
    const localStart = Math.max(0, start - sourceStart)
    const localEnd = Math.min(head.total, start + pageSize - sourceStart)
    sourceStart += head.total
    if (localStart >= localEnd) continue
    const firstPage = Math.floor(localStart / pageSize) + 1
    const lastPage = Math.floor((localEnd - 1) / pageSize) + 1
    for (let page = firstPage; page <= lastPage; page++) {
      const result = page === 1 ? head : await sources[index].load(page, pageSize)
      if (result.total !== head.total) throw new Error('追溯记录数量已变化，请重新查询。')
      const offset = (page - 1) * pageSize
      rows.push(...result.list.slice(Math.max(0, localStart - offset), localEnd - offset))
    }
  }
  return { list: rows, total }
}
