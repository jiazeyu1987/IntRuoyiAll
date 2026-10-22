import request from '@/config/axios'
export interface EmployeeSignatureScope {
  activeOrderId: number; routeId: number; routeProcessId: number; processId: number
  employeeProfileId: number; identityDomain: 'MES_EMPLOYEE_PROFILE'
}
export interface EmployeeSignatureQualification { status: string; changeRequired: boolean; expired: boolean; locked: boolean; identityDomain: string }
export const getEmployeeSignatureQualification = (params: EmployeeSignatureScope) => request.get<EmployeeSignatureQualification>({url:'/mes/pro/feedback/frontline/device-account/employee-profile/signature-qualification',params})
export const changeEmployeeSignaturePassword = (data: EmployeeSignatureScope & {currentPassword: string;newPassword: string}) => request.post<boolean>({url:'/mes/pro/feedback/frontline/device-account/employee-profile/signature-password/change',data})
