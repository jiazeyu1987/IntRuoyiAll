import request from '@/config/axios'
import type { MesRouteId, ProRouteVO, ProRouteVersionVO } from './index'

export interface RouteTreeNode extends ProRouteVO {
  parentId?: MesRouteId
  nodeType: 'FLOW' | 'GROUP'
  processIds: MesRouteId[]
  pendingStatus?: string
  different: boolean
  children?: RouteTreeNode[]
  flowCount?: number
}
export interface TreeChange {
  field: string
  before: unknown
  after: unknown
}
export interface TreePreview {
  groupName: string
  fingerprint: string
  blockers: string[]
  changes: TreeChange[]
  affected: {
    routeId: MesRouteId
    name: string
    nodeType: 'FLOW' | 'GROUP'
    versionNo: string
    beforeProcesses: string[]
    afterProcesses: string[]
    changes: TreeChange[]
    blocker?: string
  }[]
}
const url = '/mes/pro/route-tree'
export const RouteTreeApi = {
  list: () => request.get<RouteTreeNode[]>({ url: url + '/list' }),
  renameGroup: (id: MesRouteId, name: string) => request.put<boolean>({ url: url + '/group-name', data: { id, name } }),
  deleteGroup: (id: MesRouteId) => request.delete<boolean>({ url: url + '/group', params: { id } }),
  group: (data: { name: string; parentId?: MesRouteId; sourceRouteId?: MesRouteId }) =>
    request.post<MesRouteId>({ url: url + '/group', data }),
  inherit: (parentId: MesRouteId, data: unknown) =>
    request.post<MesRouteId>({ url: url + '/inherit', params: { parentId }, data }),
  movePreview: (id: MesRouteId, parentId?: MesRouteId) =>
    request.get<{ different: boolean; fingerprint: string; currentProcesses: string[]; expectedProcesses: string[] }>({
      url: url + '/move-preview', params: { id, parentId }
    }),
  move: (data: { id: MesRouteId; parentId?: MesRouteId; fingerprint: string; confirmDifference: boolean }) =>
    request.put<boolean>({ url: url + '/move', data }),
  preview: (id: MesRouteId) => request.get<TreePreview>({ url: url + '/publish-preview', params: { id } }),
  submit: (data: { id: MesRouteId; fingerprint: string; reason: string }) =>
    request.post<ProRouteVersionVO>({ url: url + '/submit', data })
}
