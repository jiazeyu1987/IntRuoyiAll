import request from '@/config/axios'

export type InspectionLevel = 'S1' | 'S2' | 'S3' | 'S4' | 'I' | 'II' | 'III'

export interface SamplingCell {
  sampleCode: string
  nominalSampleSize: number
}

export interface SamplingRow {
  minQuantity: number
  maxQuantity: number | null
  samples: Record<InspectionLevel, SamplingCell>
}

export interface SamplingResult extends SamplingCell {
  standard: string
  totalQuantity: number
  inspectionLevel: InspectionLevel
  minQuantity: number
  maxQuantity: number | null
}

export const getSamplingTable = (): Promise<SamplingRow[]> =>
  request.get({ url: '/mes/qc/sampling/table' })

export const lookupSampling = (
  totalQuantity: number,
  inspectionLevel: InspectionLevel
): Promise<SamplingResult> =>
  request.get({ url: '/mes/qc/sampling/lookup', params: { totalQuantity, inspectionLevel } })
