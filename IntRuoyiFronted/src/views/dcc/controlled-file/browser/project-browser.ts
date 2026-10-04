import { getProjectDiscovery, getProjectDiscoveryPage, type DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import { getProjectFolders } from '@/api/dcc/controlledFile/projectAttributes'
import { loadDccProjectBrowserPage, getControlledFileRelationPermissions,
  type ProjectBrowserOptions } from '@/api/dcc/controlledFile/applicationRead'
import { getControlledFile } from '@/api/dcc/controlledFile/workflow'
import { getSimpleUserList } from '@/api/system/user'
import { loadSelectorBrowserPage, listProjectReferences, createProjectReferences, cancelProjectReference, getProjectReferenceUsage } from '@/api/dcc/controlledFile/relations'
import { buildProjectFolderTree } from '../basic-data/components/project-folder-tree'
import { isEnabledTargetProjectLeader, mapReferenceDirectoryNodes, mapReferenceRows, referenceIdentity,
  bindReferencePermissions, assertReferenceTraceIdentity, type ReferenceRow } from '../relations/project-reference-contract'
import type { DirectoryNode } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'

export const PROJECT_BROWSER_VERSION_VIEWS = [
  { value: 'LATEST_CONTROLLED', label: '最新受控版本', group: '浏览范围' },
  { value: 'ALL', label: '全部版本', group: '浏览范围' },
  { value: 'WORKING', label: '工作小版本', group: '工作版本' },
  { value: 'DRAFT', label: '草稿', group: '工作版本' },
  { value: 'PENDING_MATRIX_REVIEW', label: '审批中 · 会签', group: '审批中' },
  { value: 'PENDING_MATRIX_APPROVAL', label: '审批中 · 批准', group: '审批中' },
  { value: 'PENDING_APPLICANT_TRAINING_RECORD', label: '审批中 · 培训记录', group: '审批中' },
  { value: 'PENDING_DOC_CONTROL_REVIEW', label: '审批中 · 文控审核', group: '审批中' },
  { value: 'PENDING_APPLICANT_REWORK', label: '审批中 · 申请人返工', group: '审批中' },
  { value: 'READY_TO_PUBLISH', label: '审批中 · 待受控', group: '审批中' },
  { value: 'FINALIZING', label: '审批中 · 受控处理中', group: '审批中' },
  { value: 'FINALIZATION_FAILED', label: '审批中 · 受控处理失败', group: '审批中' },
  { value: 'SUPERSEDED', label: '历史版本 · 已替换', group: '历史版本' },
  { value: 'OBSOLETE', label: '历史版本 · 已作废', group: '历史版本' },
  { value: 'REJECTED', label: '历史申请 · 已驳回', group: '历史版本' },
  { value: 'WITHDRAWN', label: '历史申请 · 已撤回', group: '历史版本' }
] as const
export type ProjectBrowserVersionView = (typeof PROJECT_BROWSER_VERSION_VIEWS)[number]['value']
const browserVersionOptions = (view: ProjectBrowserVersionView): ProjectBrowserOptions => {
  if (!PROJECT_BROWSER_VERSION_VIEWS.some(option => option.value === view)) throw new Error('请选择正式版本浏览范围')
  return view === 'LATEST_CONTROLLED' ? { latestVersionOnly: true }
    : view === 'ALL' ? { latestVersionOnly: false } : { latestVersionOnly: false, status: view }
}

export class ProjectBrowserState {
  projects: DccDiscoveredProject[] = []
  projectsTotal = 0
  projectPage = 1
  projectKeyword = ''
  projectLoading = false
  projectError = ''
  project: DccDiscoveredProject | undefined
  directories: DirectoryNode[] = []
  folder: DirectoryNode | undefined
  folderLoading = false
  folderError = ''
  actorEnabled = false
  actorError = ''
  scope: 'global' | 'directory' = 'global'
  versionView: ProjectBrowserVersionView = 'LATEST_CONTROLLED'
  keyword = ''
  pageNo = 1
  readonly pageSize = 20
  rows: FileCandidate[] = []
  total = 0
  loading = false
  error = ''
  usage: Record<string, { referenceProjectCount: number; referenced: boolean }> = {}
  private projectsSequence = 0
  private projectSequence = 0
  private filesSequence = 0
  constructor(readonly tenantId: string, readonly actorId: string) {
    referenceIdentity(tenantId); referenceIdentity(actorId)
  }
  get contextKey() { return JSON.stringify([this.tenantId, this.project?.id, this.folder?.folderId]) }
  get canReference() {
    return Boolean(this.project && this.folder && isEnabledTargetProjectLeader(
      { id: this.actorId, status: this.actorEnabled ? 0 : 1 }, this.project))
  }
  async loadActor() {
    this.actorEnabled = false; this.actorError = ''
    try {
      // The official simple-list endpoint returns enabled accounts only.
      const enabled = await getSimpleUserList()
      if (!Array.isArray(enabled)) throw new Error('启用账号列表响应缺失')
      this.actorEnabled = enabled.some(row => referenceIdentity(row.id) === this.actorId)
    } catch (error) { this.actorError = errorText(error) }
  }
  async loadProjects() {
    const token = ++this.projectsSequence
    this.projectLoading = true; this.projectError = ''; this.projects = []; this.projectsTotal = 0
    try {
      const page = await getProjectDiscoveryPage({ pageNo: this.projectPage, pageSize: 20, keyword: this.projectKeyword })
      if (token !== this.projectsSequence) return
      this.projects = page.list; this.projectsTotal = page.total
    } catch (error) { if (token === this.projectsSequence) this.projectError = errorText(error) }
    finally { if (token === this.projectsSequence) this.projectLoading = false }
  }
  invalidateFiles() {
    this.filesSequence++; this.loading = false; this.rows = []; this.total = 0; this.usage = {}; this.error = ''
  }
  async selectProject(id: string) {
    const token = ++this.projectSequence
    this.project = undefined; this.folder = undefined; this.directories = []; this.folderError = ''; this.folderLoading = true
    this.scope = 'directory'; this.invalidateFiles()
    try {
      const project = await getProjectDiscovery(referenceIdentity(id))
      const folders = await getProjectFolders(project.id)
      const directories = mapReferenceDirectoryNodes(this.tenantId, project.id, buildProjectFolderTree(project.id, folders))
      if (token !== this.projectSequence) return
      this.project = project; this.directories = directories
    } catch (error) { if (token === this.projectSequence) this.folderError = errorText(error) }
    finally { if (token === this.projectSequence) this.folderLoading = false }
  }
  async selectFolder(node: DirectoryNode) {
    if (!this.project || node.projectId !== this.project.id || !node.folderId) throw new Error('请选择当前项目的逻辑文件夹')
    this.folder = node; this.scope = 'directory'; this.pageNo = 1
    await this.loadFiles()
  }
  async search(scope: 'global' | 'directory', keyword = this.keyword) {
    this.scope = scope; this.keyword = keyword; this.pageNo = 1
    await this.loadFiles()
  }
  async changeVersionView(view: ProjectBrowserVersionView) {
    browserVersionOptions(view)
    this.versionView = view; this.pageNo = 1
    await this.loadFiles()
  }
  async loadFiles() {
    const token = ++this.filesSequence
    this.loading = true; this.error = ''; this.rows = []; this.total = 0; this.usage = {}
    try {
      if (this.scope === 'directory' && (!this.project || !this.folder?.folderId)) throw new Error('请先选择项目逻辑文件夹')
      const page = await loadDccProjectBrowserPage(this.tenantId, { keyword: this.keyword, pageNo: this.pageNo, pageSize: this.pageSize,
        ...(this.scope === 'directory' ? { projectId: this.project!.id, folderId: this.folder!.folderId } : {}) }, browserVersionOptions(this.versionView))
      if (token !== this.filesSequence) return
      this.rows = page.list; this.total = page.total
      // Counts are the formal server distinct-project usage, never the number of visible folders.
      await this.refreshUsage(token)
    } catch (error) { if (token === this.filesSequence) this.error = errorText(error) }
    finally { if (token === this.filesSequence) this.loading = false }
  }
  loadPage(query: SelectorQuery) { return loadSelectorBrowserPage(this.tenantId, query) }
  async refreshUsage(token = this.filesSequence) {
    const results = await Promise.all(this.rows.map(async row => {
      const value = await getProjectReferenceUsage(row.controlledFileId)
      if (referenceIdentity(value.masterId) !== row.masterId || !Number.isSafeInteger(value.referenceProjectCount)
        || value.referenceProjectCount < 0 || value.referenced !== (value.referenceProjectCount > 0)) throw new Error('引用项目计数响应不合法')
      return [row.masterId, { referenceProjectCount: value.referenceProjectCount, referenced: value.referenced }] as const
    }))
    if (token === this.filesSequence) this.usage = Object.fromEntries(results)
  }
  private assertReferenceContext(projectId: string, folderId: string) {
    if (!this.canReference || this.project?.id !== projectId || this.folder?.folderId !== folderId) throw new Error('仅当前目标项目启用负责人可以引用或取消引用')
  }
  private async mapReferences(result: unknown, projectId: string, folderId: string) {
    if (!Array.isArray(result)) throw new Error('引用列表响应缺失')
    const permissions = await Promise.all(result.map(row => {
      const selectedId = referenceIdentity(row?.reference?.selectedControlledFileId)
      if (referenceIdentity(row?.selectedVersion?.controlledFileId) !== selectedId
        || referenceIdentity(row?.selectedVersion?.tenantId) !== this.tenantId)
        throw new Error('引用列表固定文件或租户身份不一致')
      return getControlledFileRelationPermissions(selectedId)
    }))
    const names = Object.fromEntries(permissions.map(projection => [projection.projectId, projection.projectName]))
    return mapReferenceRows(result, { tenantId: this.tenantId, projectId, folderId }, names)
      .map((row, index) => bindReferencePermissions(row, permissions[index]))
  }
  async readReferenceForAction(row: ReferenceRow) {
    const context = this.contextKey
    if (!this.project || !this.folder || row.reference.projectId !== this.project.id
      || row.reference.folderId !== this.folder.folderId || row.selectedVersion.tenantId !== this.tenantId)
      throw new Error('引用读取上下文与当前项目文件夹不一致')
    const captured: ReferenceRow = JSON.parse(JSON.stringify(row))
    const permission = await getControlledFileRelationPermissions(referenceIdentity(captured.reference.selectedControlledFileId))
    if (context !== this.contextKey) throw new Error('引用读取上下文已变化，请重新选择当前引用')
    return bindReferencePermissions(captured, permission)
  }
  async readReferenceTrace(row: ReferenceRow) {
    const context = this.contextKey
    const selected = await this.readReferenceForAction(row)
    const detail = await getControlledFile(referenceIdentity(selected.reference.selectedControlledFileId))
    if (context !== this.contextKey) throw new Error('引用追溯上下文已变化，请重新选择当前引用')
    assertReferenceTraceIdentity(selected, detail)
    return selected
  }
  async loadReferences(projectId: string, folderId: string) {
    return this.mapReferences(await listProjectReferences(projectId, folderId), projectId, folderId)
  }
  async createReferences(projectId: string, folderId: string, ids: string[], reason: string) {
    this.assertReferenceContext(projectId, folderId)
    const selectedIds = ids.map(referenceIdentity)
    const saved = await createProjectReferences(projectId, folderId, selectedIds, reason)
    if (!Array.isArray(saved) || saved.length !== selectedIds.length
      || new Set(saved.map(row => referenceIdentity(row?.reference?.selectedControlledFileId))).size !== selectedIds.length
      || saved.some(row => !selectedIds.includes(referenceIdentity(row?.reference?.selectedControlledFileId))))
      throw new Error('引用保存响应没有确认完整的所选版本，请核对正式保存结果')
    try { return await this.mapReferences(saved, projectId, folderId) }
    catch (cause) { throw Object.assign(new Error(`引用已保存，但权限与名称读取失败：${errorText(cause)}。请刷新此文件夹引用。`), { referenceSaved: true }) }
  }
  async cancelReference(projectId: string, folderId: string, masterId: string, referenceId: string, reason: string) {
    this.assertReferenceContext(projectId, folderId)
    const count: unknown = await cancelProjectReference(projectId, folderId, referenceIdentity(masterId), referenceIdentity(referenceId), reason)
    if (typeof count !== 'number' || !Number.isSafeInteger(count) || count < 0) throw new Error('取消引用返回的项目计数无效')
    return count
  }
  async referencesChanged() {
    try { await this.refreshUsage() } catch (error) { this.error = `引用已保存，项目计数刷新失败：${errorText(error)}` }
  }
}
const errorText = (error: unknown) => error instanceof Error ? error.message : String(error)
