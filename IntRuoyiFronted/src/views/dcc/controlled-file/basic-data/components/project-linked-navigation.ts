import type { Router } from 'vue-router'
import { checkPermi } from '@/utils/permission'

/** Both the tenant menu route and the query permission are required for a cross-module link. */
export const canOpenProjectLinkedPage = (router: Router, path: string, permission: string): boolean => {
  if (!checkPermi([permission])) return false
  const target = router.resolve(path).matched.at(-1)
  return !!target && target.path !== '/:path(.*)*' && target.path !== '/404'
    && target.path !== '/403' && !['404Page', 'NoFound', 'NoAccess'].includes(String(target.name))
}
