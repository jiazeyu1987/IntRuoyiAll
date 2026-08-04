import request from '@/config/axios'

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
