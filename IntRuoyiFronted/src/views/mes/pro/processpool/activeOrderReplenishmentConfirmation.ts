export const applyWithNoReplenishmentConfirmation = async <T>(
  apply: (confirmed?: boolean) => Promise<T>,
  confirm: () => Promise<unknown>,
  errorMessage: (error: unknown) => string
): Promise<T | undefined> => {
  try {
    return await apply()
  } catch (error) {
    if (!errorMessage(error).includes('NO_REPLENISHMENT_CONFIRMATION_REQUIRED')) throw error
  }
  try {
    await confirm()
  } catch (action) {
    if (action === 'cancel' || action === 'close') return undefined
    throw action
  }
  return apply(true)
}
