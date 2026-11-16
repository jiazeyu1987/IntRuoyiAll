import { erpNumberFormatter } from '@/utils'

// Only list presentation uses zero decimal places; source values and form precision stay intact.
export const formatErpListInteger = (value: number | string | undefined) =>
  erpNumberFormatter(value, 0)

export const erpListIntegerColumnFormatter = (_row: unknown, _column: unknown, value: number | string | undefined) =>
  formatErpListInteger(value)
