// IDs are decimal strings so Java Long identities survive browser round trips exactly.
export interface FileCandidate {
  tenantId: string; controlledFileId: string; masterId: string; projectId: string
  projectName: string; folderName: string; projectFolderId?: string | null
  fileNumber: string; fileName: string; versionNo: string; status: string
  controlled: boolean; pendingEffect: boolean; executable: boolean; canPreview: boolean
}
export interface SelectorContext { contextKey: string; tenantId: string; masterId?: string; selectionMode?: 'multiple' | 'single'; forbidSelfRelation?: boolean; allowEmptySelection?: boolean }
export interface SelectorQuery { projectId?: string; folderId?: string; keyword: string; pageNo: number; pageSize: number }
export interface SelectorPage { list: FileCandidate[]; total: number }
interface RequestToken { generation: number; sequence: number; query: SelectorQuery }
export class FileSelectorState {
  context: SelectorContext
  selected: FileCandidate[] = []; rows: FileCandidate[] = []
  total = 0; loading = false; saving = false; error = ''
  private generation = 0; private sequence = 0
  get contextGeneration() { return this.generation }
  constructor(context: SelectorContext) { this.context = { ...context } }
  reset(context: SelectorContext, selected: FileCandidate[]) {
    this.generation++; this.sequence++; this.context = { ...context }
    this.rows = []; this.selected = []; this.total = 0; this.error = ''; this.loading = false; this.saving = false
    selected.forEach(row => {
      this.assertIdentity(row)
      if (!this.selected.some(item => item.masterId === row.masterId)) this.selected.push({ ...row })
    })
    if (this.context.selectionMode === 'single' && this.selected.length > 1) throw new Error('DCC_SELECTOR_SINGLE_SELECTION_REQUIRED')
  }
  private assertIdentity(row: FileCandidate) {
    this.assertRowIdentity(row)
    if (row.masterId === this.context.masterId && this.context.forbidSelfRelation !== false) throw new Error('DCC_SELECTOR_SELF_RELATION')
  }
  private assertRowIdentity(row: FileCandidate) {
    const ids = [row.tenantId, row.masterId, row.controlledFileId, row.projectId]
    if (ids.some(id => typeof id !== 'string' || !/^[1-9]\d*$/.test(id))) throw new Error('DCC_SELECTOR_IDENTITY_INVALID')
    if (row.tenantId !== this.context.tenantId) throw new Error('DCC_SELECTOR_TENANT_MISMATCH')
    if (row.pendingEffect && row.executable) throw new Error('DCC_SELECTOR_VERSION_STATE_INVALID')
  }
  select(row: FileCandidate) {
    if (this.saving) throw new Error('DCC_SELECTOR_SAVING')
    this.assertIdentity(row)
    if (!row.controlled) throw new Error('DCC_SELECTOR_NOT_CONTROLLED')
    if (this.context.selectionMode === 'single') { this.selected = [{ ...row }]; return }
    if (!this.selected.some(item => item.masterId === row.masterId)) this.selected.push({ ...row })
  }
  remove(masterId: string) {
    if (this.saving) throw new Error('DCC_SELECTOR_SAVING')
    this.selected = this.selected.filter(row => row.masterId !== masterId)
  }
  begin(query: SelectorQuery): RequestToken {
    this.rows = []; this.total = 0; this.error = ''; this.loading = true
    return { generation: this.generation, sequence: ++this.sequence, query: { ...query } }
  }
  private matches(token: RequestToken) { return token.generation === this.generation && token.sequence === this.sequence }
  resolve(token: RequestToken, page: SelectorPage) {
    if (!this.matches(token)) return
    if (!Number.isSafeInteger(page.total) || page.total < 0 || !Array.isArray(page.list)) throw new Error('DCC_SELECTOR_INVALID_PAGE')
    page.list.forEach(row => this.assertRowIdentity(row))
    if (new Set(page.list.map(row => row.masterId)).size !== page.list.length) throw new Error('DCC_SELECTOR_DUPLICATE_PAGE_IDENTITY')
    this.rows = page.list.map(row => ({ ...row })); this.total = page.total; this.loading = false
  }
  reject(token: RequestToken, error: unknown) {
    if (!this.matches(token)) return
    this.loading = false; this.rows = []; this.total = 0; this.error = error instanceof Error ? error.message : String(error)
  }
  label(row: FileCandidate) {
    if (row.status === 'OBSOLETE') return '源文件已作废'
    if (!row.controlled) return '无可用受控版本'
    return row.pendingEffect ? '待生效' : '受控版本'
  }
  assertPreview(row: FileCandidate) {
    this.assertIdentity(row)
    if (!row.canPreview) throw new Error('DCC_SELECTOR_CONTENT_PERMISSION_DENIED')
  }
  async save(persist: (rows: FileCandidate[]) => Promise<unknown>, confirmed: boolean) {
    if (!confirmed || this.saving) return false
    const generation = this.generation; this.saving = true; this.error = ''
    try {
      if (this.context.allowEmptySelection === false && this.selected.length === 0) throw new Error('请先选择文件')
      if (this.context.selectionMode === 'single' && this.selected.length !== 1) throw new Error('请选择一个文件')
      this.selected.forEach(row => { this.assertIdentity(row); if (!row.controlled) throw new Error('DCC_SELECTOR_NOT_CONTROLLED') })
      await persist(this.selected.map(row => ({ ...row })))
      return generation === this.generation
    } catch (error) {
      if (generation === this.generation) this.error = error instanceof Error ? error.message : String(error)
      return false
    } finally { if (generation === this.generation) this.saving = false }
  }
}
