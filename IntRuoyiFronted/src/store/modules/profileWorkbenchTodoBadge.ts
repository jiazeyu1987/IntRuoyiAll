import request from '@/config/axios'
import { getMyDistributionTaskPage } from '@/api/dcc/controlledFile/distribution'
import { getMyTrainingTaskPage } from '@/api/dcc/controlledFile/training'
import {
  EDHR_WORK_TASK_STATUS_TODO,
  getEdhrWorkTaskMyPage
} from '@/api/mes/pro/edhr/workTask'
import { ProWorkOrderApi } from '@/api/mes/pro/workorder'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import {
  MesProWorkOrderStatusEnum,
  MesProWorkOrderTypeEnum
} from '@/views/mes/utils/constants'
import { normalizeAssignmentPage } from '@/views/showroom-admin/assignment/contracts'
import { defineStore } from 'pinia'
import { store } from '../index'

const TODO_TOTAL_REFRESH_INTERVAL_MS = 30 * 1000
const PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE = 1
const SHOWROOM_ASSIGNMENT_TODO_BADGE_PAGE_SIZE = 20

let pendingTodoTotalRequest: Promise<void> | undefined

export const hasProfileWorkbenchTodoBadgeRoute = (routes: AppRouteRecordRaw[] = []): boolean =>
  routes.some((route) =>
    Boolean(
      route.meta?.personalWorkbenchTodoBadge ||
        hasProfileWorkbenchTodoBadgeRoute(route.children || [])
    )
  )

const normalizeTodoTotal = (total: unknown) => {
  const numericTotal = Number(total)
  if (!Number.isFinite(numericTotal) || numericTotal < 0) {
    throw new Error(`个人工作台待处理数量异常：${String(total)}`)
  }
  return Math.trunc(numericTotal)
}

const normalizePageTotal = (page: PageResult<unknown[]> | undefined | null, source: string) => {
  if (!page || page.total === undefined || page.total === null) {
    throw new Error(`${source}接口返回缺少待处理数量。`)
  }
  return normalizeTodoTotal(page.total)
}

const resolveTodoBadgeError = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '个人工作台待处理数量加载失败'
}

const hasRouteName = (routes: AppRouteRecordRaw[] = [], targetName: string): boolean =>
  routes.some((route) => route.name === targetName || hasRouteName(route.children || [], targetName))

const resolveCurrentUserId = () => {
  const userStore = useUserStoreWithOut()
  const id = Number(userStore.getUser?.id ?? userStore.user?.id)
  return Number.isFinite(id) && id > 0 ? id : undefined
}

const canViewDccTraining = () => checkPermi(['dcc:controlled-file:training:mine'])
const canViewDccDistribution = () => checkPermi(['dcc:controlled-file:query'])
const canViewEdhrWorkTasks = () =>
  checkPermi(['mes:pro-edhr-work-task:query']) ||
  checkPermi(['mes:pro-edhr-batch-execution:query'])
const canViewWorkOrders = () => checkPermi(['mes:pro-work-order:query'])
const canViewShowroomAssignments = () => {
  const userStore = useUserStoreWithOut()
  const permissionStore = usePermissionStoreWithOut()
  return (
    userStore.getRoles.includes('super_admin') ||
    hasRouteName(permissionStore.getRouters, 'ShowroomAdminAssignment')
  )
}

const loadDccTrainingTodoTotal = async () => {
  const pages = await Promise.all([
    getMyTrainingTaskPage({
      pageNo: 1,
      pageSize: PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE,
      status: 'PENDING_VIEW'
    }),
    getMyTrainingTaskPage({
      pageNo: 1,
      pageSize: PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE,
      status: 'READY_TO_ACKNOWLEDGE'
    })
  ])
  return pages.reduce((total, page) => total + normalizePageTotal(page, '文控培训'), 0)
}

const loadDccDistributionTodoTotal = async () => {
  const page = await getMyDistributionTaskPage({
    pageNo: 1,
    pageSize: PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE,
    status: 'READY_TO_ACKNOWLEDGE'
  })
  return normalizePageTotal(page, '文控分发')
}

const loadEdhrWorkTaskTodoTotal = async () => {
  const page = await getEdhrWorkTaskMyPage({
    pageNo: 1,
    pageSize: PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE,
    status: EDHR_WORK_TASK_STATUS_TODO
  })
  return normalizePageTotal(page, 'eDHR 工作任务')
}

const loadWorkOrderTodoTotal = async () => {
  const page = await ProWorkOrderApi.getWorkOrderPage({
    pageNo: 1,
    pageSize: PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE,
    status: MesProWorkOrderStatusEnum.CONFIRMED,
    type: MesProWorkOrderTypeEnum.SELF,
    temporaryFrozen: false
  } as any)
  return normalizePageTotal(page as PageResult<unknown[]>, '排产工单')
}

const loadShowroomAssignmentTodoTotal = async () => {
  const currentUserId = resolveCurrentUserId()
  if (!currentUserId) {
    throw new Error('当前登录用户 ID 缺失，无法加载展厅补充指派数量。')
  }
  const page = await request.get<unknown[]>({
    url: '/showroom/assignment/page',
    params: {
      status: 'OPEN',
      assigneeUserId: currentUserId,
      pageNo: 1,
      pageSize: SHOWROOM_ASSIGNMENT_TODO_BADGE_PAGE_SIZE
    }
  })
  const assignments = normalizeAssignmentPage(page)
  if (assignments.length === SHOWROOM_ASSIGNMENT_TODO_BADGE_PAGE_SIZE) {
    throw new Error('展厅补充指派接口缺少 total，无法准确统计超过单页上限的待办数量。')
  }
  return assignments.length
}

const loadProfileWorkbenchTodoTotal = async () => {
  const loaders: Array<Promise<number>> = []
  if (canViewDccDistribution()) {
    loaders.push(loadDccDistributionTodoTotal())
  }
  if (canViewDccTraining()) {
    loaders.push(loadDccTrainingTodoTotal())
  }
  if (canViewEdhrWorkTasks()) {
    loaders.push(loadEdhrWorkTaskTodoTotal())
  }
  if (canViewWorkOrders()) {
    loaders.push(loadWorkOrderTodoTotal())
  }
  if (canViewShowroomAssignments()) {
    loaders.push(loadShowroomAssignmentTodoTotal())
  }

  const totals = await Promise.all(loaders)
  return totals.reduce((sum, total) => sum + total, 0)
}

export const useProfileWorkbenchTodoBadgeStore = defineStore('profileWorkbenchTodoBadge', {
  state: () => ({
    todoTotal: 0,
    loaded: false,
    loading: false,
    error: '',
    lastLoadedAt: 0
  }),
  getters: {
    getHasVisibleTodoBadge: (state) => state.loaded && state.todoTotal > 0,
    getTodoBadgeText: (state) =>
      state.loaded && state.todoTotal > 0 ? String(state.todoTotal) : ''
  },
  actions: {
    applyTodoTotal(total: unknown) {
      this.todoTotal = normalizeTodoTotal(total)
      this.loaded = true
      this.error = ''
      this.lastLoadedAt = Date.now()
    },
    async refreshTodoTotal() {
      if (pendingTodoTotalRequest) {
        return pendingTodoTotalRequest
      }

      pendingTodoTotalRequest = (async () => {
        this.loading = true
        this.error = ''
        try {
          const total = await loadProfileWorkbenchTodoTotal()
          this.applyTodoTotal(total)
        } catch (error) {
          this.error = resolveTodoBadgeError(error)
          throw error
        } finally {
          this.loading = false
          pendingTodoTotalRequest = undefined
        }
      })()

      return pendingTodoTotalRequest
    },
    async ensureTodoTotalLoaded() {
      if (this.loaded && Date.now() - this.lastLoadedAt < TODO_TOTAL_REFRESH_INTERVAL_MS) {
        return
      }
      await this.refreshTodoTotal()
    }
  }
})

export const useProfileWorkbenchTodoBadgeStoreWithOut = () => {
  return useProfileWorkbenchTodoBadgeStore(store)
}
