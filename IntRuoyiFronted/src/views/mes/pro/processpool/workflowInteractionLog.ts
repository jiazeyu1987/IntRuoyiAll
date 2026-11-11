import request from '@/config/axios'
import { ref, onMounted, onActivated, onDeactivated, onBeforeUnmount, nextTick } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { getTenantId, getAccessToken } from '@/utils/auth'

export type WorkflowLogScope = 'TEAM_LEADER' | 'PRODUCTION' | 'PQC' | 'PQC_RELEASE' | 'MARKET_RELEASE'
export interface WorkflowLogEntry {
  id: number; actorName: string; actorUserId: number; actionName: string; resultStatus: string; occurredAt: number
}
export const getOwnWorkflowLogs = () => request.get<WorkflowLogEntry[]>({ url:'/mes/pro/workflow-interaction-log/own-recent' })

export function useWorkflowInteractionLog(scope: WorkflowLogScope | (() => WorkflowLogScope)) {
  const saveError = ref('')
  const saving = ref(0)
  const user = useUserStore()
  let installed = false
  const pending = new Set<HTMLElement>()
  const dialogSelector='.el-dialog,.el-message-box,.el-drawer,dialog,[role="dialog"]'
  const currentSession = () => `${getTenantId()}:${user.getUser.id}:${getAccessToken()}`
  const record = async (outcome: 'CANCELED' | 'BLOCKED', control: string) => {
    const session = currentSession()
    if (!user.getUser.id || !getTenantId() || !getAccessToken()) {
      saveError.value='操作日志保存失败：登录身份已失效。'
      return
    }
    saving.value++
    try {
      await request.post({url:'/mes/pro/workflow-interaction-log', data:{scope:typeof scope==='function'?scope():scope,
        outcome,control}, ignoreErrorMessage:true, skipAuthRefreshReplay:true,
        isRequestContextCurrent:()=>session===currentSession()})
    } catch {
      saveError.value='操作日志保存失败，取消或拦截记录尚未确认，请联系管理员核对。'
    } finally { saving.value-- }
  }
  const blocked = (control='页面校验未通过') => { if(installed)void record('BLOCKED',control) }
  const isVisible = (element: HTMLElement) => element.isConnected && element.getClientRects().length>0
  const isClosing = (element: HTMLElement) => {
    for (let node: HTMLElement | null = element; node; node = node.parentElement) {
      if (Array.from(node.classList).some(name => /-leave-(active|from|to)$/.test(name))) return true
    }
    return false
  }
  // Titles and button labels describe the action; never collect input values or dialog body text.
  const actionLabel = (text?: string | null) => text?.replace(/[^\p{Script=Han}A-Za-z0-9（）()：:、，。/＋+·\s-]/gu,'').trim().slice(0,50)
  const cancel = async (dialog?: HTMLElement) => {
    const session=currentSession()
    const action=actionLabel(dialog?.querySelector?.('.el-dialog__title,.el-message-box__title,.el-drawer__title,h3')?.textContent)
    if (dialog && pending.has(dialog)) return
    if (dialog) pending.add(dialog)
    // Read the accepted close transition after Vue renders; animation duration is not a close guard.
    await nextTick()
    await new Promise<void>(resolve=>requestAnimationFrame(()=>resolve()))
    if(session!==currentSession()) {
      saveError.value='操作日志保存失败：操作期间登录身份已改变，未以其他人员身份补记。'
      if(dialog)pending.delete(dialog)
      return
    }
    const refused = dialog && isVisible(dialog) && !isClosing(dialog)
    await record(refused?'BLOCKED':'CANCELED',refused?`关闭被拦截：${action || '当前任务处理中'}`:action || '关闭操作确认或编辑窗口')
    if(dialog) pending.delete(dialog)
  }
  const click = (event: MouseEvent) => {
    const target=event.target instanceof Element?event.target:null
    if (!target || target.closest('[data-workflow-interaction-log]')) return
    const button=target.closest('button')
    const text=button?.textContent?.replace(/\s+/g,'')
    if(button && (['取消','关闭'].includes(text ?? '') || button.matches('.el-dialog__headerbtn,.el-message-box__headerbtn,.el-drawer__close-btn'))) {
      void cancel(button.closest<HTMLElement>(dialogSelector) ?? undefined)
    }
    if(target.matches('.el-overlay-dialog,.el-overlay-message-box,.el-overlay')) {
      const dialog=target.querySelector<HTMLElement>(dialogSelector)
      if(dialog)void cancel(dialog)
    }
    if(target.matches('[role="dialog"].frontline-production-submit-confirmation-modal')) void cancel(target as HTMLElement)
    if(target.matches('.frontline-production-sop-modal')) {
      const dialog=target.querySelector<HTMLElement>('[role="dialog"]')
      if(dialog)void cancel(dialog)
    }
  }
  const headerClose=(event:Event)=>{
    const target=(event as CustomEvent).detail
    if(target instanceof Element)void cancel(target.closest<HTMLElement>('.el-dialog') ?? undefined)
  }
  const pointer = (event: PointerEvent) => {
    const target=event.target instanceof Element?event.target:null
    const button=target?.closest<HTMLButtonElement>('button:disabled')
    if (button && /提交|确认|保存|放行|重置|数据|查询|反查|审核|复核|审批|导入|上传|导出|打印|创建|撤回/.test(button.textContent ?? ''))
      void record('BLOCKED',`按钮不可用：${actionLabel(button.textContent) || '操作未提交'}`)
  }
  const key = (event: KeyboardEvent) => {
    if(event.key!=='Escape') return
    const dialogs=Array.from(document.querySelectorAll<HTMLElement>('.el-dialog,.el-message-box,.el-drawer,dialog[open],[role="dialog"]')).filter(isVisible)
    if(dialogs.length) void cancel(dialogs.at(-1))
  }
  const observer=new MutationObserver(records=>{
    if(records.some(r=>Array.from(r.addedNodes).some(n=>n instanceof Element &&
      (n.matches('.el-message--error,.el-notification--error,.el-message--warning,.el-notification--warning') || n.querySelector('.el-message--error,.el-notification--error,.el-message--warning,.el-notification--warning'))))) blocked()
  })
  const install=()=>{
    if(installed)return
    installed=true
    document.addEventListener('click',click,true)
    document.addEventListener('pointerdown',pointer,true)
    document.addEventListener('keydown',key,true)
    window.addEventListener('workflow-dialog-close-request',headerClose)
    observer.observe(document.body,{childList:true,subtree:true})
  }
  const uninstall=()=>{
    installed=false
    document.removeEventListener('click',click,true)
    document.removeEventListener('pointerdown',pointer,true)
    document.removeEventListener('keydown',key,true)
    window.removeEventListener('workflow-dialog-close-request',headerClose)
    observer.disconnect()
  }
  onMounted(install);onActivated(install);onDeactivated(uninstall);onBeforeUnmount(uninstall)
  return {saving,saveError,blocked,record}
}
