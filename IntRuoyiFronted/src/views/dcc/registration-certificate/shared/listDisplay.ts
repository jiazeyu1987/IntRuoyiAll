import { formatApplicationCertificateType } from './applicationDisplay'

export const formatListCertificateType = (factsJson?: string | null): string => {
  const type = factsJson ? JSON.parse(factsJson)?.certificateType : undefined
  return type ? formatApplicationCertificateType(type) : '—'
}
