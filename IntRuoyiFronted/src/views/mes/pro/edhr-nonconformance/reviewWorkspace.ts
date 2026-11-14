export interface ReviewWorkspaceMaterial {
  url: string
  fileName: string
}

export interface ReviewUploadAttempt {
  key: number
  fileName: string
  status: 'uploading' | 'failed'
  error?: string
}

export interface ReviewValidationErrors {
  materials: string
  opinion: string
  signature: string
}
