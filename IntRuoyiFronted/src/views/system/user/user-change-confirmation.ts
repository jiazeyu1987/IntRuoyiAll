export const sameSelection = (before: readonly (string | number)[], after: readonly (string | number)[]) =>
  before.length === after.length && before.every((id) => after.includes(id))
export const confirmUserChange = async (
  confirm: (content: string, title?: string) => Promise<unknown>,
  content: string
): Promise<boolean> => {
  try {
    await confirm(content, '确认用户变更')
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') return false
    throw error
  }
}
