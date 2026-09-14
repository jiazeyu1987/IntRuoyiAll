import type { RouteMeta } from 'vue-router'
import { Icon } from '@/components/Icon'
import { useI18n } from '@/hooks/web/useI18n'
import { useApprovalTodoBadgeStoreWithOut } from '@/store/modules/approvalTodoBadge'
import { useProfileWorkbenchTodoBadgeStoreWithOut } from '@/store/modules/profileWorkbenchTodoBadge'

export const useRenderMenuTitle = () => {
  const renderApprovalTodoBadge = (meta: RouteMeta) => {
    const approvalTodoBadgeStore = useApprovalTodoBadgeStoreWithOut()
    if (!meta.approvalTodoBadge || !approvalTodoBadgeStore.getHasVisibleTodoBadge) {
      return undefined
    }
    return (
      <span class="approval-todo-badge" aria-label={`待办数量 ${approvalTodoBadgeStore.todoTotal}`}>
        {approvalTodoBadgeStore.todoTotal}
      </span>
    )
  }

  const renderProfileWorkbenchTodoBadge = (meta: RouteMeta) => {
    const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStoreWithOut()
    if (!meta.personalWorkbenchTodoBadge || !profileWorkbenchTodoBadgeStore.getHasVisibleTodoBadge) {
      return undefined
    }
    return (
      <span
        class="personal-workbench-todo-badge"
        aria-label={`个人工作台待处理数量 ${profileWorkbenchTodoBadgeStore.todoTotal}`}
      >
        {profileWorkbenchTodoBadgeStore.todoTotal}
      </span>
    )
  }

  const renderMenuTitle = (meta: RouteMeta) => {
    const { t } = useI18n()
    const { title, icon } = meta
    if (typeof title !== 'string' || title.trim().length === 0) {
      throw new Error(`菜单标题缺失：请检查路由 meta.title 或后端菜单 name。meta=${JSON.stringify(meta)}`)
    }
    const titleNode = (
      <span class="approval-menu-title">
        <span class="v-menu__title approval-menu-title__text min-w-0 overflow-hidden overflow-ellipsis whitespace-nowrap">
          {t(title)}
        </span>
        {renderApprovalTodoBadge(meta)}
        {renderProfileWorkbenchTodoBadge(meta)}
      </span>
    )

    return icon ? (
      <>
        <Icon icon={meta.icon}></Icon>
        {titleNode}
      </>
    ) : (
      <>{titleNode}</>
    )
  }

  return {
    renderMenuTitle
  }
}
