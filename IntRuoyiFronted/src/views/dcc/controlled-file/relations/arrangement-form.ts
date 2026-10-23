import type { FileCandidate } from './selector-state'

export interface ArrangementForm { relatedMasterId: string; assigneeUserId?: string; dueAt?: string }
export interface ArrangementCommand { relatedMasterId: string; assigneeUserId: string; dueAt: string }
export const validateArrangementForm = (rows: ArrangementForm[], relations: FileCandidate[], assignees: {id:string;name:string}[]): ArrangementCommand[] => {
  const unique=new Set<string>()
  return rows.map(row=>{
    if(!/^[1-9]\d*$/.test(row.relatedMasterId)||!relations.some(relation=>relation.masterId===row.relatedMasterId))throw new Error('整改安排不属于本次关联文件')
    if(unique.has(row.relatedMasterId))throw new Error('同一关联文件存在重复整改安排')
    unique.add(row.relatedMasterId)
    if(typeof row.assigneeUserId!=='string'||!/^[1-9]\d*$/.test(row.assigneeUserId)||!assignees.some(user=>user.id===row.assigneeUserId))throw new Error('请选择本次可用整改负责人')
    if(typeof row.dueAt!=='string'||!validDeadline(row.dueAt))throw new Error('请填写有效整改期限，精确到秒')
    return {relatedMasterId:row.relatedMasterId,assigneeUserId:row.assigneeUserId,dueAt:row.dueAt}
  })
}
const validDeadline=(text:string)=>{
  const matched=/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(text)
  if(!matched)return false
  const [year,month,day,hour,minute,second]=matched.slice(1).map(Number)
  if(year<1||month<1||month>12||day<1||hour>23||minute>59||second>59)return false
  const date=new Date(0);date.setUTCFullYear(year,month-1,day);date.setUTCHours(hour,minute,second,0)
  return date.getUTCFullYear()===year&&date.getUTCMonth()===month-1&&date.getUTCDate()===day
}
