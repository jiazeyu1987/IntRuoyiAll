export interface PdfViewportLike {
  width: number
  height: number
}

export interface PdfRenderTaskLike {
  promise: Promise<void>
}

export interface PdfPageProxyLike {
  getViewport(params: { scale: number }): PdfViewportLike
  render(params: {
    canvasContext: CanvasRenderingContext2D
    viewport: PdfViewportLike
  }): PdfRenderTaskLike
  cleanup?(): void
}

export interface PdfDocumentProxyLike {
  numPages: number
  getPage(pageNumber: number): Promise<PdfPageProxyLike>
  cleanup?(): Promise<void> | void
  destroy?(): Promise<void> | void
}

export interface PdfLoadingTaskLike {
  promise: Promise<PdfDocumentProxyLike>
  destroy?(): Promise<void> | void
}

export const GlobalWorkerOptions: {
  workerSrc: string
}

export function getDocument(params: unknown): PdfLoadingTaskLike
