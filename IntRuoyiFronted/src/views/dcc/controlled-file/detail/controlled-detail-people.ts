// userNames is already formatted by the existing detail loader; retain distinct name/account pairs.
export const getControlledDetailPersonText = (display?: string): string => {
  const text = display?.trim() || ''
  const pair = text.match(/^(.+?)\s*\(([^()]*)\)$/)
  return pair && pair[1].trim() === pair[2].trim() ? pair[1].trim() : text
}
