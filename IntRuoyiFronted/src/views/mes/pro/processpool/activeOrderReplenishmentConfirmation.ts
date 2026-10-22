export const applyWithNoReplenishmentConfirmation = async <T>(
  apply: (confirmed?: boolean) => Promise<T>,
  confirm: () => Promise<unknown>,
  errorMessage: (error: unknown) => string,
  isCurrent: () => boolean
): Promise<T | undefined> => {
  if (!isCurrent()) return undefined
  try {
    return await apply()
  } catch (error) {
    if (!errorMessage(error).includes('NO_REPLENISHMENT_CONFIRMATION_REQUIRED')) throw error
    if (!isCurrent()) return undefined
  }
  if (!isCurrent()) return undefined
  try {
    await confirm()
  } catch (action) {
    if (action === 'cancel' || action === 'close') return undefined
    throw action
  }
  if (!isCurrent()) return undefined
  return apply(true)
}
