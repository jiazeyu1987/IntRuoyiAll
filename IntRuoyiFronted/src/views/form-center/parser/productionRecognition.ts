export interface BatchRecordTotalRecognitionProduct {
  name?: string
  code?: string
}

export interface BatchRecordTotalRecognitionMaterial {
  code?: string | null
  name?: string
  sourceCodeLabel?: string | null
}

export interface BatchRecordTotalRecognitionEquipmentOption {
  code?: string | null
  name?: string
  sourceCodeLabel?: string | null
}

export interface BatchRecordTotalRecognitionParameterUi {
  control?: string
  defaultValue?: unknown
  step?: unknown
  min?: unknown
  max?: unknown
  unit?: string
  displayName?: string
  options?: unknown[]
}

export interface BatchRecordTotalRecognitionParameter {
  name?: string
  referenceValue?: string
  actualValue?: string
  ui?: BatchRecordTotalRecognitionParameterUi
}

export interface BatchRecordTotalRecognitionEquipmentGroup {
  equipmentOptions?: BatchRecordTotalRecognitionEquipmentOption[]
  parameters?: BatchRecordTotalRecognitionParameter[]
  selectionMode?: string
}

export interface BatchRecordTotalRecognitionProcess {
  name: string
  criticalProcess?: boolean
  inputs?: BatchRecordTotalRecognitionMaterial[]
  outputs?: BatchRecordTotalRecognitionMaterial[]
  equipmentGroups?: BatchRecordTotalRecognitionEquipmentGroup[]
}

export interface BatchRecordTotalRecognitionJson {
  product: BatchRecordTotalRecognitionProduct
  schemaVersion: number
  processes: BatchRecordTotalRecognitionProcess[]
}
