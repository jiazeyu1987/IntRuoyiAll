type SignatureOrderRecord = {
  id?: number
  signedAt?: string
}

export const selectLatestSignature = <T extends SignatureOrderRecord>(
  records: T[],
  signedAtMillis: (record: T) => number
) => {
  records.forEach((record) => {
    if (typeof record.id !== 'number' || !Number.isFinite(record.id)) {
      throw new Error('签名记录缺少有效编号，无法确定最新签名。')
    }
    const signedAt = signedAtMillis(record)
    if (!Number.isFinite(signedAt)) {
      throw new Error('签名记录缺少有效服务器签署时间，无法确定最新签名。')
    }
  })
  return records.reduce<T | undefined>((latest, candidate) => {
    if (!latest) return candidate
    const timeDifference = signedAtMillis(candidate) - signedAtMillis(latest)
    if (timeDifference !== 0) return timeDifference > 0 ? candidate : latest
    return candidate.id! > latest.id! ? candidate : latest
  }, undefined)
}
