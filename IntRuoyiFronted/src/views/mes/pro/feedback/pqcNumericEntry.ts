// Keep source strings (including measured precision); never synthesize measurements.
export const isPqcNumericValue = (value: string, precision?: number) => {
  const text = value.trim()
  if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(text) || !Number.isFinite(Number(text)))
    return false
  // Match the server's BigDecimal scale after removing insignificant trailing zeros.
  const decimals = (text.split('.')[1] ?? '').replace(/0+$/, '')
  return precision === undefined || decimals.length <= precision
}

export function parsePqcNumericColumn(text: string, quantity: number, precision?: number) {
  const rows = text.replace(/\r\n?/g, '\n').split('\n')
  // Excel terminates its copied column with one newline; internal blanks are samples.
  if (rows.at(-1) === '') rows.pop()
  const values = rows.map((row) => row.trim())
  const errors: string[] = []
  if (values.length !== quantity) errors.push(`需要 ${quantity} 行，当前 ${values.length} 行。`)
  rows.forEach((row, index) => {
    if (row.includes('\t')) errors.push(`第 ${index + 1} 行包含多列，请只复制一列测量值。`)
    else if (!row.trim()) errors.push(`第 ${index + 1} 行为空，不能跳过件号。`)
    else if (!isPqcNumericValue(row)) errors.push(`第 ${index + 1} 行不是有效数字，请使用普通小数。`)
    else if (!isPqcNumericValue(row, precision)) errors.push(`第 ${index + 1} 行超出小数精度（最多 ${precision} 位）。`)
  })
  return { values, errors }
}
