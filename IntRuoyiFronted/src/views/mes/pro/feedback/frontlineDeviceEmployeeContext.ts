import { ProFeedbackApi } from '@/api/mes/pro/feedback'
import type {
  FrontlineActiveOrderVO,
  FrontlineDeviceRouteProcessVO,
  FrontlineEmployeeCandidateVO,
  FrontlinePqcSwitchActualEmployeeReqVO,
  FrontlineSwitchActualEmployeeReqVO,
  FrontlineSwitchActualEmployeeRespVO,
  FrontlineTemplateVO
} from '@/api/mes/pro/feedback'

export interface FrontlineDeviceEmployeeState {
  activeOrderOptions: FrontlineActiveOrderVO[]
  processOptions: FrontlineDeviceRouteProcessVO[]
  employeeOptions: FrontlineEmployeeCandidateVO[]
  selectedActiveOrder?: FrontlineActiveOrderVO
  selectedProcess?: FrontlineDeviceRouteProcessVO
  selectedEmployee?: FrontlineEmployeeCandidateVO
  template?: FrontlineTemplateVO
  loadingActiveOrders: boolean
  loadingProcesses: boolean
  loadingEmployees: boolean
  loadingTemplate: boolean
  lastError?: string
}

export const createFrontlineDeviceEmployeeState = (): FrontlineDeviceEmployeeState => ({
  activeOrderOptions: [],
  processOptions: [],
  employeeOptions: [],
  loadingActiveOrders: false,
  loadingProcesses: false,
  loadingEmployees: false,
  loadingTemplate: false
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
  process: FrontlineDeviceRouteProcessVO | undefined,
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
    routeProcessId: process.routeProcessId,
    processId: process.processId,
    actualEmployeeId
  }
}

export const loadFrontlineDeviceProcesses = async (state: FrontlineDeviceEmployeeState) => {
  state.loadingProcesses = true
  state.lastError = undefined
  try {
    const processes = await ProFeedbackApi.getFrontlineDeviceAccountProcesses()
    state.processOptions = processes
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
    return activeOrders
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingActiveOrders = false
  }
}

export const selectFrontlinePqcActiveOrder = async (
  state: FrontlineDeviceEmployeeState,
  activeOrder: FrontlineActiveOrderVO
) => {
  state.selectedActiveOrder = activeOrder
  state.selectedProcess = undefined
  state.selectedEmployee = undefined
  state.template = undefined
  state.processOptions = []
  state.employeeOptions = []
  state.loadingProcesses = true
  state.lastError = undefined
  try {
    const processes = await ProFeedbackApi.getFrontlinePqcActiveOrderProcesses({
      workOrderId: activeOrder.workOrderId,
      routeId: activeOrder.routeId
    })
    state.processOptions = processes
    return processes
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingProcesses = false
  }
}

export const selectFrontlineProcess = async (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO
) => {
  state.selectedProcess = process
  state.selectedEmployee = undefined
  state.template = undefined
  state.employeeOptions = []
  state.loadingEmployees = true
  state.lastError = undefined
  try {
    const employees = await ProFeedbackApi.getFrontlineEmployeeCandidates({
      routeId: process.routeId,
      routeProcessId: process.routeProcessId,
      processId: process.processId
    })
    state.employeeOptions = employees
    return employees
  } catch (error) {
    state.lastError = resolveFrontlineErrorMessage(error)
    throw error
  } finally {
    state.loadingEmployees = false
  }
}

export const selectFrontlinePqcProcess = async (
  state: FrontlineDeviceEmployeeState,
  process: FrontlineDeviceRouteProcessVO
) => {
  state.selectedProcess = process
  state.selectedEmployee = undefined
  state.template = undefined
  state.employeeOptions = []
  state.loadingEmployees = true
  state.lastError = undefined
  try {
    const employees = await ProFeedbackApi.getFrontlinePqcEmployeeCandidates()
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
  const payload = buildFrontlineEmployeeSwitchPayload(state.selectedProcess, actualEmployeeId)
  state.template = undefined
  state.loadingTemplate = true
  state.lastError = undefined
  try {
    const result = await ProFeedbackApi.switchFrontlineActualEmployee(payload)
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

export const switchFrontlinePqcActualEmployee = async (
  state: FrontlineDeviceEmployeeState,
  actualEmployeeId: number
): Promise<FrontlineSwitchActualEmployeeRespVO> => {
  const payload = buildFrontlinePqcEmployeeSwitchPayload(
    state.selectedActiveOrder,
    state.selectedProcess,
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
