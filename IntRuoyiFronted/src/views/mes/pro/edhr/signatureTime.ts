export interface EdhrSignatureTimeForm {
  selectedSignedAt: string | null
  selectedTimeZone: string | null
  selectedTimeReason: string | null
}

export interface EdhrSignatureTimePayload {
  selectedSignedAt: string
  selectedTimeZone: string
  selectedTimeReason: string
}

export const resolveBrowserTimeZone = () => {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || ''
}

export const createSignatureTimeForm = (): EdhrSignatureTimeForm => ({
  selectedSignedAt: '',
  selectedTimeZone: resolveBrowserTimeZone(),
  selectedTimeReason: ''
})

export const normalizeSelectedSignedAt = (value: string) => {
  const trimmed = String(value || '').trim()
  return trimmed.replace(
    /^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})$/,
    '$1T$2'
  )
}

export const buildSignatureTimePayload = (
  form: EdhrSignatureTimeForm
): EdhrSignatureTimePayload | undefined => {
  const selectedSignedAt = String(form.selectedSignedAt || '').trim()
  const selectedTimeZone = String(form.selectedTimeZone || '').trim()
  const selectedTimeReason = String(form.selectedTimeReason || '').trim()
  if (!selectedSignedAt && !selectedTimeReason) {
    return undefined
  }
  if (selectedTimeReason && !selectedSignedAt) {
    throw new Error('请选择签名时间。')
  }
  if (!selectedTimeZone) {
    throw new Error('签名时区不能为空。')
  }
  if (selectedSignedAt && !selectedTimeReason) {
    throw new Error('签名时间原因不能为空。')
  }
  return {
    selectedSignedAt: normalizeSelectedSignedAt(selectedSignedAt),
    selectedTimeZone,
    selectedTimeReason
  }
}
