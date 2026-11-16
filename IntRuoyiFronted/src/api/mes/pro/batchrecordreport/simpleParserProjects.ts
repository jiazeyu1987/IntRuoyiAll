import request from '@/config/axios'

export interface SimpleParserProject {
  id: number
  projectCode: string
  projectName: string
  status: string
  routeOccupied: boolean
}

export const getSimpleParserProjects = (keyword = ''): Promise<SimpleParserProject[]> =>
  request.get({ url: '/mes/pro/batch-record-report/simple-parser/projects', params: { keyword } })

export async function getSimpleParserProject(id: number): Promise<SimpleParserProject> {
  const project = (await getSimpleParserProjects()).find((item) => item.id === id)
  if (!project) throw new Error('所选项目不存在或已停用，请重新选择项目')
  return project
}
