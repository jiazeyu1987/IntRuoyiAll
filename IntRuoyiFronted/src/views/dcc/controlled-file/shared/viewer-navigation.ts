import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { buildControlledFileTraceabilityPath, buildControlledFileViewerPath } from '../view/presentation'

export const openControlledFileViewer = (
  router: Router,
  route: RouteLocationNormalizedLoaded,
  controlledFileId: number | string,
  from: string
) => {
  const normalizedId = String(controlledFileId || '').trim()
  if (!normalizedId) {
    throw new Error('DCC 受控文件预览缺少文件 ID，无法打开受控预览页。')
  }
  return router.push(buildControlledFileViewerPath(normalizedId, from, route.fullPath))
}

export const openControlledFileTraceability = (
  router: Router,
  route: RouteLocationNormalizedLoaded,
  controlledFileId: number | string,
  from: string
) => {
  const normalizedId = String(controlledFileId || '').trim()
  if (!normalizedId) {
    throw new Error('DCC 受控文件追溯缺少文件 ID，无法打开签核追溯页。')
  }
  return router.push(buildControlledFileTraceabilityPath(normalizedId, from, route.fullPath))
}
