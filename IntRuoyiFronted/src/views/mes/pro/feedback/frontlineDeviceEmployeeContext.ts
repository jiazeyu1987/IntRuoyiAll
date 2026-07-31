import { ProFeedbackApi } from '@/api/mes/pro/feedback'
import type {
  FrontlineDeviceRouteProcessVO,
  FrontlineEmployeeCandidateVO,
  FrontlineSwitchActualEmployeeReqVO,
  FrontlineSwitchActualEmployeeRespVO,
  FrontlineTemplateVO
} from '@/api/mes/pro/feedback'

export interface FrontlineDeviceEmployeeState {
  processOptions: FrontlineDeviceRouteProcessVO[]
  employeeOptions: FrontlineEmployeeCandidateVO[]
  selectedProcess?: FrontlineDeviceRouteProcessVO
  selectedEmployee?: FrontlineEmployeeCandidateVO
  template?: FrontlineTemplateVO
  loadingProcesses: boolean
  loadingEmployees: boolean
  loadingTemplate: boolean
  lastError?: string
}

export const createFrontlineDeviceEmployeeState = (): FrontlineDeviceEmployeeState => ({
  processOptions: [],
  employeeOptions: [],
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

const resolveFrontlineErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }
  return String(error)
}
