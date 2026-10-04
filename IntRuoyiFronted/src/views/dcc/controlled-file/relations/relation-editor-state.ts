import type { FileCandidate } from './selector-state'

export interface CurrentRelationContext { sourceControlledFileId: string; rowVersion: string; files: FileCandidate[] }
export interface RelationSaveCommand { selectedFileIds: string[]; expectedMasterIds: string[]; expectedVersion: string; idempotencyKey: string; reason: string }
export interface RelationSaveResult { sourceControlledFileId: string; rowVersion: string; relatedMasterIds: string[] }
export class RelationEditorState {
  current: CurrentRelationContext | undefined
  reason = ''; error = ''
  private command: { fingerprint: string; idempotencyKey: string } | undefined
  private generation = 0
  constructor(private readonly newIdempotencyKey: () => string) {}
  setCurrent(current: CurrentRelationContext) {
    if (!/^[1-9]\d*$/.test(current.sourceControlledFileId) || !/^(0|[1-9]\d*)$/.test(current.rowVersion)) throw new Error('关联上下文身份无效')
    this.generation++; this.current = { ...current, files: current.files.map(row=>({...row})) }; this.command = undefined; this.error = ''
  }
  invalidate() { this.generation++; this.current = undefined; this.command = undefined; this.error = '' }
  async save(selected: FileCandidate[], persist: (sourceFileId: string,command: RelationSaveCommand) => Promise<RelationSaveResult>) {
    if (!this.current) throw new Error('请先读取当前关联')
    if (!this.reason.trim()) throw new Error('请填写关联操作原因')
    const current=this.current,generation=this.generation
    const fields={selectedFileIds:selected.map(row=>row.controlledFileId).sort(),expectedMasterIds:current.files.map(row=>row.masterId).sort(),expectedVersion:current.rowVersion,reason:this.reason}
    const fingerprint=JSON.stringify({sourceFileId:current.sourceControlledFileId,...fields})
    if(this.command?.fingerprint!==fingerprint){
      const key=this.newIdempotencyKey();if(!key||key.length>128)throw new Error('关联保存请求身份无效')
      this.command={fingerprint,idempotencyKey:key}
    }
    const result=await persist(current.sourceControlledFileId,{...fields,idempotencyKey:this.command.idempotencyKey})
    if(generation!==this.generation)throw new Error('关联保存所属上下文已变化')
    if(result.sourceControlledFileId!==current.sourceControlledFileId || typeof result.rowVersion!=='string' || !/^(0|[1-9]\d*)$/.test(result.rowVersion)
      || !Array.isArray(result.relatedMasterIds) || new Set(result.relatedMasterIds).size!==selected.length
      || selected.some(row=>!result.relatedMasterIds.includes(row.masterId)))throw new Error('关联保存结果身份不一致')
    this.setCurrent({sourceControlledFileId:result.sourceControlledFileId,rowVersion:result.rowVersion,files:selected})
    return result
  }
}
