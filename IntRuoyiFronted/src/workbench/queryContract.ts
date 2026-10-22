export type WorkbenchQueryContractVersion = 3 | 4

export const parseWorkbenchQueryContract = (value: string | undefined): WorkbenchQueryContractVersion => {
  const configured = value === undefined ? '3' : value
  if (configured === '3') return 3
  if (configured === '4') return 4
  throw new Error('工作台查询版本配置无效。')
}

// The deployment selects a contract explicitly; a response cannot change the selected engine.
export const workbenchQueryContractVersion = parseWorkbenchQueryContract(import.meta.env.VITE_WORKBENCH_QUERY_CONTRACT)
