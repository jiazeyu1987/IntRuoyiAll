export const sameSelection = (before: readonly (string | number)[], after: readonly (string | number)[]) =>
  before.length === after.length && before.every((id) => after.includes(id))
export const confirmUserChange = async <T = string>(
  confirm: (content: T, title?: string) => Promise<unknown>,
  content: T,
  title = '确认用户变更'
): Promise<boolean> => {
  try {
    await confirm(content, title)
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') return false
    throw error
  }
}
