import request from '@/config/axios'
import { requireCategoryApprovalSourceMode, type DccApprovalSourceMode } from './fileCategories'

export interface DccFileTypeTaxonomyVO {
  id?: number
  parentId?: number | null
  levelNo?: number
  code: string
  name: string
  active: boolean
  sort: number
  remark?: string
  createTime?: number
  children?: DccFileTypeTaxonomyVO[]
}

export type DccFileTypeTaxonomySaveReqVO = Pick<
  DccFileTypeTaxonomyVO,
  'id' | 'parentId' | 'code' | 'name' | 'active' | 'sort' | 'remark'
>

export const getFileTypeTaxonomyList = async (): Promise<DccFileTypeTaxonomyVO[]> => {
  return await request.get({ url: '/dcc/file-type-taxonomies' })
}

export interface DccFileTypeTaxonomyUploadOption extends Omit<DccFileTypeTaxonomyVO, 'id' | 'parentId' | 'children'> {
  id: number | string
  parentId?: number | string | null
  /** Parents and explicitly blocked leaves have null; selectable leaves use the formal category source. */
  approvalSourceMode: DccApprovalSourceMode | null
  uploadBlocker: string | null
  children?: DccFileTypeTaxonomyUploadOption[]
}

export const getFileTypeTaxonomyUploadOptions = async (): Promise<DccFileTypeTaxonomyUploadOption[]> => {
  const decode = (rows: DccFileTypeTaxonomyUploadOption[]): DccFileTypeTaxonomyUploadOption[] => {
    if (!Array.isArray(rows)) throw new Error('正式文件类型候选响应缺失')
    return rows.map(row => {
      if (!row || typeof row !== 'object') throw new Error('文件类型候选合同缺失')
      const source = row.approvalSourceMode === null ? null : requireCategoryApprovalSourceMode(row.approvalSourceMode)
      const blocker = row.uploadBlocker
      if (blocker !== null && (typeof blocker !== 'string' || !blocker.trim()))
        throw new Error('文件类型候选阻断事实缺失或无效')
      if (source !== null && blocker !== null) throw new Error('文件类型审批来源与阻断事实合同冲突')
      return { ...row, approvalSourceMode: source, uploadBlocker: blocker,
        ...(row.children === undefined ? {} : { children: decode(row.children) }) }
    })
  }
  const rows = decode(await request.get({ url: '/dcc/file-type-taxonomies/upload-options' }))
  const flat: DccFileTypeTaxonomyUploadOption[] = []
  const collect = (values: DccFileTypeTaxonomyUploadOption[]) => {
    for (const row of values) { flat.push(row); if (row.children) collect(row.children) }
  }
  collect(rows)
  for (const row of flat) {
    const parent = Boolean(row.children?.length) || flat.some(child => child.parentId != null && String(child.parentId) === String(row.id))
    if (parent && (row.approvalSourceMode !== null || row.uploadBlocker !== null))
      throw new Error('文件类型父节点仅承载路径，审批来源和阻断事实必须明确为null')
    if (!parent && row.approvalSourceMode === null && row.uploadBlocker === null)
      throw new Error('文件类型叶节点审批来源或明确阻断事实缺失')
  }
  return rows
}

export const createFileTypeTaxonomy = async (data: DccFileTypeTaxonomySaveReqVO): Promise<number> => {
  return await request.post({ url: '/dcc/file-type-taxonomies', data })
}

export const updateFileTypeTaxonomy = async (
  id: number,
  data: DccFileTypeTaxonomySaveReqVO
): Promise<boolean> => {
  return await request.put({ url: `/dcc/file-type-taxonomies/${id}`, data })
}

export const deleteFileTypeTaxonomy = async (id: number): Promise<boolean> => {
  return await request.delete({ url: `/dcc/file-type-taxonomies/${id}` })
}

export const resolveFileTypeActiveCategory = (id: number | string): Promise<number | string> =>
  request.get({ url: `/dcc/file-type-taxonomies/${id}/active-category`, ignoreErrorMessage: true })
