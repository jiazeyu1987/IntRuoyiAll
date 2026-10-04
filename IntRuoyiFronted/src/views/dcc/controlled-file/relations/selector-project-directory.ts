import { getProjectDiscoveryPage } from '@/api/dcc/controlledFile/projectDiscovery'
import type { DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import { getProjectFolders } from '@/api/dcc/controlledFile/projectAttributes'
import { buildProjectFolderTree } from '../basic-data/components/project-folder-tree'
import { mapReferenceDirectoryNodes, referenceIdentity } from './project-reference-contract'
import type { DirectoryNode, SelectorSource } from './DccFileSelector.vue'

/** Candidate navigation only. Source and persisted reference target remain owned by the caller. */
export class SelectorProjectDirectoryState {
  projects: DccDiscoveredProject[] = []
  total = 0
  pageNo = 1
  readonly pageSize = 10
  keyword = ''
  projectLoading = false
  projectError = ''
  directories: DirectoryNode[] = []
  projectId: string | undefined
  projectName = ''
  folderId: string | undefined
  directoryLoading = false
  directoryError = ''
  active = false
  private generation = 0
  private pageSequence = 0
  private directorySequence = 0
  constructor(readonly tenantId: string) { referenceIdentity(tenantId) }
  close() {
    this.active = false; this.generation++; this.pageSequence++; this.directorySequence++
    this.projects = []; this.total = 0; this.projectId = undefined; this.projectName = ''; this.folderId = undefined
    this.directories = []; this.projectLoading = false; this.directoryLoading = false; this.projectError = ''; this.directoryError = ''
  }
  async open(source: SelectorSource) {
    this.close(); this.active = true; this.keyword = ''; this.pageNo = 1
    if (referenceIdentity(source.tenantId) !== this.tenantId) throw new Error('候选项目导航租户与来源不一致')
    const generation = this.generation, directorySequence = this.directorySequence + 1
    await Promise.all([this.loadProjects(), this.selectProject({ id: source.projectId, projectName: source.projectName }, source.folderId)])
    return this.active && generation === this.generation && directorySequence === this.directorySequence
  }
  async loadProjects() {
    if (!this.active) return
    const token = ++this.pageSequence, generation = this.generation
    if (!Number.isInteger(this.pageNo) || this.pageNo < 1) throw new Error('候选项目页码无效')
    this.projectLoading = true; this.projectError = ''; this.projects = []; this.total = 0
    try {
      const result = await getProjectDiscoveryPage({ keyword: this.keyword, pageNo: this.pageNo, pageSize: this.pageSize })
      if (!this.active || token !== this.pageSequence || generation !== this.generation) return
      this.projects = result.list; this.total = result.total
    } catch (cause) {
      if (this.active && token === this.pageSequence && generation === this.generation) this.projectError = cause instanceof Error ? cause.message : String(cause)
    } finally { if (this.active && token === this.pageSequence && generation === this.generation) this.projectLoading = false }
  }
  async searchProjects() { this.pageNo = 1; await this.loadProjects() }
  async selectProject(project: Pick<DccDiscoveredProject, 'id' | 'projectName'>, initialFolderId?: string) {
    if (!this.active) return
    const id = referenceIdentity(project.id), token = ++this.directorySequence, generation = this.generation
    this.projectId = id; this.projectName = project.projectName; this.folderId = undefined; this.directories = []
    this.directoryLoading = true; this.directoryError = ''
    try {
      const folders = await getProjectFolders(id)
      const tree = mapReferenceDirectoryNodes(this.tenantId, id, buildProjectFolderTree(id, folders))
      if (!this.active || token !== this.directorySequence || generation !== this.generation) return
      this.directories = tree
      if (initialFolderId != null) {
        const wanted = referenceIdentity(initialFolderId)
        const find = (nodes: DirectoryNode[]): boolean => nodes.some(node => node.folderId === wanted || find(node.children || []))
        if (!find(tree)) throw new Error('来源正式逻辑文件夹不存在或未授权，无法定位；可明确改用全局搜索')
        this.folderId = wanted
      }
    } catch (cause) {
      if (this.active && token === this.directorySequence && generation === this.generation) { this.directories = []; this.folderId = undefined; this.directoryError = cause instanceof Error ? cause.message : String(cause) }
    } finally { if (this.active && token === this.directorySequence && generation === this.generation) this.directoryLoading = false }
  }
  selectFolder(node: DirectoryNode) {
    if (!this.active || this.directoryLoading || this.directoryError || node.projectId !== this.projectId || !node.folderId) throw new Error('请在当前候选项目选择正式逻辑文件夹')
    const find = (nodes: DirectoryNode[]): boolean => nodes.some(row => row.key === node.key && row.projectId === node.projectId && row.folderId === node.folderId || find(row.children || []))
    if (!find(this.directories)) throw new Error('所选目录不属于当前正式项目目录')
    this.folderId = referenceIdentity(node.folderId)
  }
}
