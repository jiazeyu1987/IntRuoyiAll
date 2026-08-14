import { ProFeedbackApi } from '@/api/mes/pro/feedback'
import type {
  FrontlineActiveOrderVO,
  FrontlineDeviceRouteProcessVO,
  FrontlineEmployeeCandidateVO,
  FrontlinePqcProcessVO,
  FrontlinePqcSwitchActualEmployeeReqVO,
  FrontlinePqcSwitchActualEmployeeRespVO,
  FrontlinePqcTemplateVO,
  FrontlineRuntimeConfigVO,
  FrontlineRuntimeEmployeeVO,
  FrontlineSwitchActualEmployeeReqVO,
  FrontlineSwitchActualEmployeeRespVO,
  FrontlineTemplateVO
} from '@/api/mes/pro/feedback'

export const FRONTLINE_PQC_NO_PENDING_ORDER_TEXT = '当前暂无活跃订单'

export class FrontlinePqcStaleActiveOrderSelectionError extends Error {
  constructor() {
    super('PQC活跃订单选择已被更新请求替代')
    this.name = 'FrontlinePqcStaleActiveOrderSelectionError'
  }
}

export class FrontlineProductionStaleActiveOrderSelectionError extends Error {
  constructor() {
    super('生产工单选择已被更新请求替代')
    this.name = 'FrontlineProductionStaleActiveOrderSelectionError'
  }
}

interface FrontlineProductionRuntimeCacheEntry {
  runtimeConfig: FrontlineRuntimeConfigVO
  employeeOptions: FrontlineEmployeeCandidateVO[]
}

interface FrontlineEmployeeSwitchCacheEntry {
  result: FrontlineSwitchActualEmployeeRespVO
}

interface FrontlineProductionRuntimeCache {
  runtimeConfigByProcessKey: Record<string, FrontlineProductionRuntimeCacheEntry>
  employeeSwitchByKey: Record<string, FrontlineEmployeeSwitchCacheEntry>
}

export interface FrontlineDeviceEmployeeState {
  activeOrderOptions: FrontlineActiveOrderVO[]
  productionProcessOptions: FrontlineDeviceRouteProcessVO[]
  processOptions: Array<FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO>
  employeeOptions: FrontlineEmployeeCandidateVO[]
  selectedActiveOrder?: FrontlineActiveOrderVO
  selectedProcess?: FrontlineDeviceRouteProcessVO | FrontlinePqcProcessVO
  selectedEmployee?: FrontlineEmployeeCandidateVO
  runtimeConfig?: FrontlineRuntimeConfigVO
  template?: FrontlineTemplateVO | FrontlinePqcTemplateVO
  loadingActiveOrders: boolean
  loadingProcesses: boolean
  loadingEmployees: boolean
  loadingTemplate: boolean
  preloadingRuntimeCache: boolean
  productionRuntimeCache: FrontlineProductionRuntimeCache
  pqcProcessOptionsCache: Map<string, FrontlinePqcProcessVO[]>
  pqcProcessOptionsRequests: Map<string, Promise<FrontlinePqcProcessVO[]>>
  pqcEmployeeOptionsCache?: FrontlineEmployeeCandidateVO[]
  pqcEmployeeOptionsRequest?: Promise<FrontlineEmployeeCandidateVO[]>
  productionActiveOrderSelectionRequestToken: number
  pqcActiveOrderSelectionRequestToken: number
  processSelectionRequestToken: number
  employeeSwitchRequestToken: number
  lastError?: string
}

export const createFrontlineDeviceEmployeeState = (): FrontlineDeviceEmployeeState => ({
  activeOrderOptions: [],
  productionProcessOptions: [],
  processOptions: [],
  employeeOptions: [],
  loadingActiveOrders: false,
  loadingProcesses: false,
  loadingEmployees: false,
  loadingTemplate: false,
  preloadingRuntimeCache: false,
  productionRuntimeCache: {
    runtimeConfigByProcessKey: {},
    employeeSwitchByKey: {}
  },
  pqcProcessOptionsCache: new Map<string, FrontlinePqcProcessVO[]>(),
  pqcProcessOptionsRequests: new Map<string, Promise<FrontlinePqcProcessVO[]>>(),
  productionActiveOrderSelectionRequestToken: 0,
  pqcActiveOrderSelectionRequestToken: 0,
  processSelectionRequestToken: 0,
  employeeSwitchRequestToken: 0
})

export const buildFrontlineEmployeeSwitchPayload = (
  process: FrontlineDeviceRouteProcessVO | undefined,
  actualEmployeeId: number | undefined
): FrontlineSwitchActualEmployeeReqVO => {
  if (!process) {
    throw new Error('当前工序不能为空')
  }
  if (!actualEmployeeId) {
    throw new Error('实际填写员工不能为空')
  }
  return {
    routeId: process.routeId,
    routeProcessId: process.routeProcessId,
    processId: process.processId,
    actualEmployeeId
  }
}

export const buildFrontlinePqcEmployeeSwitchPayload = (
  activeOrder: FrontlineActiveOrderVO | undefined,
  process: FrontlinePqcProcessVO | undefined,
  actualEmployeeId: number | undefined
): FrontlinePqcSwitchActualEmployeeReqVO => {
  if (!activeOrder) {
    throw new Error('当前活跃订单不能为空')
  }
  if (!process) {
    throw new Error('当前工序不能为空')
  }
  if (!actualEmployeeId) {
    throw new Error('实际填写员工不能为空')
  }
  return {
    workOrderId: activeOrder.workOrderId,
    routeId: process.routeId,
    regulationVersionId: process.regulationVersionId,
    qaProcessId: process.qaProcessId,
    actualEmployeeId
  }
}

export const buildFrontlinePqcActiveOrderProcessCacheKey = (activeOrder: FrontlineActiveOrderVO) =>
  String(activeOrder.activeOrderId)

export const buildFrontlineActiveOrderPickerKey = (activeOrder: FrontlineActiveOrderVO) =>
  String(activeOrder.activeOrderId)

export const isSameFrontlineActiveOrder = (
  left?: FrontlineActiveOrderVO,
  right?: FrontlineActiveOrderVO
) => Boolean(left && right && left.activeOrderId === right.activeOrderId)

export const loadFrontlineDeviceProcesses = async (state: FrontlineDeviceEmployeeState) => {
  state.loadingProcesses = true
  state.lastError = undefined
  try {
    const processes = await ProFeedbackApi.getFrontlineDeviceAccountProcesses()
    state.productionProcessOptions = processes
    state.processOptions = state.selectedActiveOrder
      ? processes.filter((process) => process.routeId === state.selectedActiveOrder?.routeId)
      : []
    retainFrontlineRuntimeCacheForProcesses(state, processes)
    return processes
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingProcesses = false
  }
}

export const loadFrontlinePqcActiveOrders = async (state: FrontlineDeviceEmployeeState) => {
  state.loadingActiveOrders = true
  state.lastError = undefined
  try {
    const activeOrders = await ProFeedbackApi.getFrontlinePqcActiveOrders()
    state.activeOrderOptions = activeOrders
    pruneFrontlinePqcProcessCache(state, activeOrders)
    clearFrontlinePqcSelectionIfUnavailable(state, activeOrders)
    return activeOrders
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingActiveOrders = false
  }
}

export const loadFrontlineProductionActiveOrders = async (state: FrontlineDeviceEmployeeState) => {
  state.loadingActiveOrders = true
  state.lastError = undefined
  try {
    const activeOrders = await ProFeedbackApi.getFrontlineProductionActiveOrders()
    state.activeOrderOptions = activeOrders
    if (
      state.selectedActiveOrder &&
      !activeOrders.some((activeOrder) =>
        isSameFrontlineActiveOrder(activeOrder, state.selectedActiveOrder)
      )
    ) {
      state.selectedActiveOrder = undefined
      state.selectedProcess = undefined
      state.selectedEmployee = undefined
      state.runtimeConfig = undefined
      state.template = undefined
      state.processOptions = []
      state.employeeOptions = []
    }
    return activeOrders
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingActiveOrders = false
  }
}

export const clearFrontlinePqcSelectionIfUnavailable = (
  state: FrontlineDeviceEmployeeState,
  activeOrders: FrontlineActiveOrderVO[]
) => {
  if (!state.selectedActiveOrder) {
    return
  }
  const selectedActiveOrder = state.selectedActiveOrder
  const stillAvailable = activeOrders.some((activeOrder) =>
    activeOrder.activeOrderId === selectedActiveOrder.activeOrderId
  )
  if (stillAvailable) {
    return
  }
  state.selectedActiveOrder = undefined
  state.selectedProcess = undefined
  state.selectedEmployee = undefined
  state.runtimeConfig = undefined
  state.template = undefined
  state.processOptions = []
  state.employeeOptions = []
}

const pruneFrontlinePqcProcessCache = (
  state: FrontlineDeviceEmployeeState,
  activeOrders: FrontlineActiveOrderVO[]
) => {
  const activeKeys = new Set(activeOrders.map(buildFrontlinePqcActiveOrderProcessCacheKey))
  for (const cacheKey of state.pqcProcessOptionsCache.keys()) {
    if (!activeKeys.has(cacheKey)) {
      state.pqcProcessOptionsCache.delete(cacheKey)
    }
  }
}

const getPqcProcessesWithCache = async (
  state: FrontlineDeviceEmployeeState,
  activeOrder: FrontlineActiveOrderVO
) => {
  const cacheKey = buildFrontlinePqcActiveOrderProcessCacheKey(activeOrder)
  const cachedProcesses = state.pqcProcessOptionsCache.get(cacheKey)
  if (cachedProcesses) {
    return cachedProcesses
  }
  const existingRequest = state.pqcProcessOptionsRequests.get(cacheKey)
  if (existingRequest) {
    return await existingRequest
  }
  const request = ProFeedbackApi.getPqcProcesses(activeOrder.activeOrderId).then((processes) => {
    state.pqcProcessOptionsCache.set(cacheKey, processes)
    return processes
  })
  state.pqcProcessOptionsRequests.set(cacheKey, request)
  try {
    return await request
  } finally {
    if (state.pqcProcessOptionsRequests.get(cacheKey) === request) {
      state.pqcProcessOptionsRequests.delete(cacheKey)
    }
  }
}

const getFrontlinePqcEmployeeCandidatesWithCache = async (
  state: FrontlineDeviceEmployeeState
) => {
  if (state.pqcEmployeeOptionsCache) {
    return state.pqcEmployeeOptionsCache
  }
  if (state.pqcEmployeeOptionsRequest) {
    return await state.pqcEmployeeOptionsRequest
  }
  const request = ProFeedbackApi.getFrontlinePqcEmployeeCandidates().then((employees) => {
    state.pqcEmployeeOptionsCache = employees
    return employees
  })
  state.pqcEmployeeOptionsRequest = request
  try {
    return await request
  } finally {
    if (state.pqcEmployeeOptionsRequest === request) {
      state.pqcEmployeeOptionsRequest = undefined
    }
  }
}

export const preloadFrontlinePqcSwitchingCache = async (
  state: FrontlineDeviceEmployeeState
) => {
  state.lastError = undefined
  try {
    await Promise.all([
      loadFrontlinePqcActiveOrders(state),
      getFrontlinePqcEmployeeCandidatesWithCache(state)
    ])
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  }
}

export const selectFrontlineProductionActiveOrder = async (
  state: FrontlineDeviceEmployeeState,
  activeOrder: FrontlineActiveOrderVO
) => {
  const requestToken = ++state.productionActiveOrderSelectionRequestToken
  state.processSelectionRequestToken += 1
  state.employeeSwitchRequestToken += 1
  state.selectedActiveOrder = activeOrder
  state.selectedProcess = undefined
  state.selectedEmployee = undefined
  state.runtimeConfig = undefined
  state.template = undefined
  state.productionProcessOptions = []
  state.processOptions = []
  state.employeeOptions = []
  state.loadingProcesses = false
  state.lastError = undefined

  const orderLabel = activeOrder.workOrderCode || String(activeOrder.workOrderId)
  if (!activeOrder.routeId) {
    const error = new Error('生产工单 ' + orderLabel + ' 缺少正式工艺路线。')
    state.lastError = error.message
    throw error
  }
  state.loadingProcesses = true
  try {
    const processes = await ProFeedbackApi.getFrontlineDeviceAccountProcesses()
    if (state.productionActiveOrderSelectionRequestToken !== requestToken) {
      throw new FrontlineProductionStaleActiveOrderSelectionError()
    }
    state.productionProcessOptions = processes
    retainFrontlineRuntimeCacheForProcesses(state, processes)
    const routeProcesses = processes.filter(
      (process) => process.routeId === activeOrder.routeId
    )
    if (routeProcesses.length === 0) {
      throw new Error('生产工单 ' + orderLabel + ' 的正式工艺路线没有可用工序。')
    }
    state.processOptions = routeProcesses
    return routeProcesses
  } catch (error) {
    if (state.productionActiveOrderSelectionRequestToken !== requestToken) {
      throw new FrontlineProductionStaleActiveOrderSelectionError()
    }
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    if (state.productionActiveOrderSelectionRequestToken === requestToken) {
      state.loadingProcesses = false
    }
  }
}

export const selectFrontlinePqcActiveOrder = async (
  state: FrontlineDeviceEmployeeState,
  activeOrder: FrontlineActiveOrderVO
) => {
  const requestToken = ++state.pqcActiveOrderSelectionRequestToken
  const cacheKey = buildFrontlinePqcActiveOrderProcessCacheKey(activeOrder)
  const cachedProcesses = state.pqcProcessOptionsCache.get(cacheKey)
  state.selectedActiveOrder = activeOrder
  state.selectedProcess = undefined
  state.selectedEmployee = undefined
  state.template = undefined
  state.employeeOptions = []
  if (cachedProcesses) {
    state.processOptions = cachedProcesses
    return cachedProcesses
  }
  state.processOptions = []
  state.loadingProcesses = true
  state.lastError = undefined
  try {
    const processes = await getPqcProcessesWithCache(state, activeOrder)
    if (state.pqcActiveOrderSelectionRequestToken !== requestToken) {
      throw new FrontlinePqcStaleActiveOrderSelectionError()
    }
    state.processOptions = processes
    return processes
  } catch (error) {
    if (state.pqcActiveOrderSelectionRequestToken !== requestToken) {
      throw new FrontlinePqcStaleActiveOrderSelectionError()
    }
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    if (state.pqcActiveOrderSelectionRequestToken === requestToken) {
      state.loadingProcesses = false
    }
  }
}

export const selectFrontlineProcess = async (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO
) => {
  const requestToken = ++state.processSelectionRequestToken
  state.selectedProcess = process
  state.selectedEmployee = undefined
  state.template = undefined
  state.runtimeConfig = undefined
  state.employeeOptions = []
  state.lastError = undefined

  const cachedRuntimeConfig = readFrontlineRuntimeConfigCache(state, process)
  if (cachedRuntimeConfig) {
    return applyFrontlineRuntimeConfig(state, process, cachedRuntimeConfig.runtimeConfig)
  }

  state.loadingEmployees = true
  try {
    const runtimeConfig = await ProFeedbackApi.getFrontlineRuntimeConfig({
      routeId: process.routeId,
      routeProcessId: process.routeProcessId,
      processId: process.processId
    })
    if (state.processSelectionRequestToken !== requestToken) {
      return state.employeeOptions
    }
    cacheFrontlineRuntimeConfig(state, process, runtimeConfig)
    return applyFrontlineRuntimeConfig(state, process, runtimeConfig)
  } catch (error) {
    if (state.processSelectionRequestToken !== requestToken) {
      return state.employeeOptions
    }
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    if (state.processSelectionRequestToken === requestToken) {
      state.loadingEmployees = false
    }
  }
}

const toEmployeeCandidate = (employee: FrontlineRuntimeEmployeeVO): FrontlineEmployeeCandidateVO => ({
  userId: employee.systemUserId || employee.employeeProfileId,
  username: employee.employeeCode,
  nickname: employee.displayName || employee.employeeName,
  employeeProfileId: employee.employeeProfileId,
  systemUserId: employee.systemUserId,
  employeeCode: employee.employeeCode,
  employeeName: employee.displayName || employee.employeeName,
  displayName: employee.displayName,
  employeeType: employee.employeeType
})

export const selectFrontlinePqcProcess = async (
  state: FrontlineDeviceEmployeeState,
  process: FrontlinePqcProcessVO
) => {
  const cachedEmployees = state.pqcEmployeeOptionsCache
  state.selectedProcess = process
  state.selectedEmployee = undefined
  state.template = undefined
  if (cachedEmployees) {
    state.employeeOptions = cachedEmployees
    return cachedEmployees
  }
  state.employeeOptions = []
  state.loadingEmployees = true
  state.lastError = undefined
  try {
    const employees = await getFrontlinePqcEmployeeCandidatesWithCache(state)
    state.employeeOptions = employees
    return employees
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingEmployees = false
  }
}

export const switchFrontlineActualEmployee = async (
  state: FrontlineDeviceEmployeeState,
  actualEmployeeId: number
): Promise<FrontlineSwitchActualEmployeeRespVO> => {
  const selectedProcess = state.selectedProcess
  if (!selectedProcess || !('routeProcessId' in selectedProcess)) {
    throw new Error('当前生产工序不能为空')
  }
  const payload = buildFrontlineEmployeeSwitchPayload(selectedProcess, actualEmployeeId)
  const requestToken = ++state.employeeSwitchRequestToken
  const cachedSwitch = readFrontlineEmployeeSwitchCache(state, payload)
  if (cachedSwitch) {
    return applyFrontlineEmployeeSwitchResult(state, cachedSwitch.result)
  }

  state.template = undefined
  state.loadingTemplate = true
  state.lastError = undefined
  try {
    const result = await ProFeedbackApi.switchFrontlineActualEmployee(payload)
    if (state.employeeSwitchRequestToken !== requestToken) {
      return result
    }
    cacheFrontlineEmployeeSwitchResult(state, payload, result)
    return applyFrontlineEmployeeSwitchResult(state, result)
  } catch (error) {
    if (state.employeeSwitchRequestToken !== requestToken) {
      throw error
    }
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    if (state.employeeSwitchRequestToken === requestToken) {
      state.loadingTemplate = false
    }
  }
}

export const switchFrontlinePqcActualEmployee = async (
  state: FrontlineDeviceEmployeeState,
  actualEmployeeId: number
): Promise<FrontlinePqcSwitchActualEmployeeRespVO> => {
  const payload = buildFrontlinePqcEmployeeSwitchPayload(
    state.selectedActiveOrder,
    state.selectedProcess as FrontlinePqcProcessVO | undefined,
    actualEmployeeId
  )
  state.template = undefined
  state.loadingTemplate = true
  state.lastError = undefined
  try {
    const result = await ProFeedbackApi.switchFrontlinePqcActualEmployee(payload)
    state.selectedEmployee = state.employeeOptions.find((employee) => employee.userId === result.actualEmployeeId)
    state.template = result.template
    return result
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingTemplate = false
  }
}

const resolveFrontlineErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }
  return String(error)
}

const createFrontlineProcessRuntimeCacheKey = (
  process: Pick<FrontlineDeviceRouteProcessVO, 'routeId' | 'routeProcessId' | 'processId'>
) => `${process.routeId}:${process.routeProcessId}:${process.processId}`

const createFrontlineEmployeeSwitchCacheKey = (payload: FrontlineSwitchActualEmployeeReqVO) =>
  `${payload.routeId}:${payload.routeProcessId}:${payload.processId}:${payload.actualEmployeeId}`

const readFrontlineRuntimeConfigCache = (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO
) => state.productionRuntimeCache.runtimeConfigByProcessKey[createFrontlineProcessRuntimeCacheKey(process)]

const cacheFrontlineRuntimeConfig = (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO,
  runtimeConfig: FrontlineRuntimeConfigVO
) => {
  state.productionRuntimeCache.runtimeConfigByProcessKey[createFrontlineProcessRuntimeCacheKey(process)] = {
    runtimeConfig,
    employeeOptions: runtimeConfig.employees.map(toEmployeeCandidate)
  }
  runtimeConfig.employeeSwitchSnapshots.forEach((snapshot) => {
    cacheFrontlineEmployeeSwitchResult(state, {
      routeId: process.routeId,
      routeProcessId: process.routeProcessId,
      processId: process.processId,
      actualEmployeeId: snapshot.actualEmployeeId
    }, snapshot)
  })
}

const applyFrontlineRuntimeConfig = (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO,
  runtimeConfig: FrontlineRuntimeConfigVO
) => {
  const cacheEntry = readFrontlineRuntimeConfigCache(state, process)
  state.runtimeConfig = runtimeConfig
  state.employeeOptions = cacheEntry?.employeeOptions || runtimeConfig.employees.map(toEmployeeCandidate)
  return state.employeeOptions
}

const readFrontlineEmployeeSwitchCache = (
  state: FrontlineDeviceEmployeeState,
  payload: FrontlineSwitchActualEmployeeReqVO
) => state.productionRuntimeCache.employeeSwitchByKey[createFrontlineEmployeeSwitchCacheKey(payload)]

const cacheFrontlineEmployeeSwitchResult = (
  state: FrontlineDeviceEmployeeState,
  payload: FrontlineSwitchActualEmployeeReqVO,
  result: FrontlineSwitchActualEmployeeRespVO
) => {
  state.productionRuntimeCache.employeeSwitchByKey[createFrontlineEmployeeSwitchCacheKey(payload)] = {
    result
  }
}

const applyFrontlineEmployeeSwitchResult = (
  state: FrontlineDeviceEmployeeState,
  result: FrontlineSwitchActualEmployeeRespVO
) => {
  state.selectedEmployee = state.employeeOptions.find((employee) => employee.userId === result.actualEmployeeId)
  state.template = result.template
  return result
}

const retainFrontlineRuntimeCacheForProcesses = (
  state: FrontlineDeviceEmployeeState,
  processes: FrontlineDeviceRouteProcessVO[]
) => {
  const allowedProcessKeys = new Set(processes.map(createFrontlineProcessRuntimeCacheKey))
  for (const key of Object.keys(state.productionRuntimeCache.runtimeConfigByProcessKey)) {
    if (!allowedProcessKeys.has(key)) {
      delete state.productionRuntimeCache.runtimeConfigByProcessKey[key]
    }
  }
  for (const key of Object.keys(state.productionRuntimeCache.employeeSwitchByKey)) {
    const processKey = key.split(':').slice(0, 3).join(':')
    if (!allowedProcessKeys.has(processKey)) {
      delete state.productionRuntimeCache.employeeSwitchByKey[key]
    }
  }
}

export const preloadFrontlineProductionRuntimeCache = async (
  state: FrontlineDeviceEmployeeState,
  processes: FrontlineDeviceRouteProcessVO[] = state.processOptions.filter(
    (process): process is FrontlineDeviceRouteProcessVO => 'routeProcessId' in process
  )
) => {
  const uniqueProcesses = processes.filter((process, index, items) =>
    items.findIndex((item) =>
      createFrontlineProcessRuntimeCacheKey(item) === createFrontlineProcessRuntimeCacheKey(process)
    ) === index
  )
  const uncachedProcesses = uniqueProcesses.filter((process) =>
    !readFrontlineRuntimeConfigCache(state, process)
  )
  if (uncachedProcesses.length === 0) {
    return
  }

  state.preloadingRuntimeCache = true
  state.lastError = undefined
  try {
    await Promise.all(uncachedProcesses.map(async (process) => {
      const runtimeConfig = await ProFeedbackApi.getFrontlineRuntimeConfig({
        routeId: process.routeId,
        routeProcessId: process.routeProcessId,
        processId: process.processId
      })
      cacheFrontlineRuntimeConfig(state, process, runtimeConfig)
    }))
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.preloadingRuntimeCache = false
  }
}
