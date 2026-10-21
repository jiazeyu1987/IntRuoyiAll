import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'

export const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'

// Keep the existing explicit MES/DCC grants; a menu never grants a capability.
export const hasExplicitConfigPermission = (permission: string) =>
  useUserStore().permissions.has(permission)

// Infra retains its current administrator and wildcard policy without widening MES/DCC.
export const hasInfraConfigPermission = (permission: string) => {
  const user = useUserStore()
  return (
    user.getRoles.includes('super_admin') ||
    user.getUser.username === 'admin' ||
    user.permissions.has('*:*:*') ||
    user.permissions.has(permission) ||
    checkPermi([permission])
  )
}

export const hasErpConfigCapability = (permissions: string[]) =>
  hasExplicitConfigPermission(GOLDEN_FINGER_PERMISSION) &&
  permissions.every((permission) => checkPermi([permission]))
