import type { DccFileTypeTaxonomyUploadOption } from '@/api/dcc/controlledFile/fileTypeTaxonomies'

export interface UploadFileTypePath { id: string; names: string[]; leaf: boolean }
export const uploadFileTypeIdentity = (value: unknown): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('文件类型身份发生精度损失')
  if (typeof value !== 'number' && typeof value !== 'string') throw new Error('文件类型身份缺失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('文件类型身份不合法')
  return id
}
export const buildUploadFileTypePaths = (rows: DccFileTypeTaxonomyUploadOption[]): Map<string, UploadFileTypePath> => {
  if (!Array.isArray(rows)) throw new Error('正式文件类型候选缺失')
  const byId = new Map<string, DccFileTypeTaxonomyUploadOption>()
  const collect = (values: DccFileTypeTaxonomyUploadOption[]) => {
    for (const row of values) {
      const id = uploadFileTypeIdentity(row.id)
      if (byId.has(id) || row.active !== true || !row.name?.trim()) throw new Error('正式文件类型候选包含重复或失效身份')
      byId.set(id, row)
      if (row.children) collect(row.children)
    }
  }
  collect(rows)
  const paths = new Map<string, UploadFileTypePath>(), visiting = new Set<string>()
  const resolve = (id: string): UploadFileTypePath => {
    const saved = paths.get(id)
    if (saved) return saved
    const row = byId.get(id)
    if (!row || visiting.has(id)) throw new Error('文件类型上级路径缺失或循环')
    visiting.add(id)
    const parentId = row.parentId == null || String(row.parentId) === '0' ? null : uploadFileTypeIdentity(row.parentId)
    const names = [...(parentId ? resolve(parentId).names : []), row.name]
    const leaf = ![...byId.values()].some(child => child.parentId != null && String(child.parentId) === id)
    const result = { id, names, leaf }
    visiting.delete(id); paths.set(id, result)
    return result
  }
  for (const id of byId.keys()) resolve(id)
  return paths
}
