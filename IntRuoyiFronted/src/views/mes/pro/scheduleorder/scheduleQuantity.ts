// 保留需求精度；整件排程的向上取整只用于说明剩余排程数量。
export const formatQuantity = (value?: number | null) => {
  if (value == null) return '—'
  const quantity = Number(value)
  return Number.isFinite(quantity) ? String(quantity) : '—'
}

export const wholePieceScheduleHint = (remainingQuantity?: number | null) => {
  if (remainingQuantity == null) return ''
  const quantity = Number(remainingQuantity)
  return Number.isFinite(quantity) && quantity > 0 && !Number.isInteger(quantity)
    ? `剩余按 ${Math.ceil(quantity)} 个排程`
    : ''
}
