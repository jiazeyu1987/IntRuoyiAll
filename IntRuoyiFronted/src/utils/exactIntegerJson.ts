/** Parse embedded evidence JSON without rounding integer identities. */
export const parseExactIntegerJson = (source: string): any => {
  // Validate the original grammar before changing numeric token representation.
  JSON.parse(source)
  let output = ''
  let cursor = 0
  while (cursor < source.length) {
    const start = cursor
    if (source[cursor] === '"') {
      cursor++
      while (cursor < source.length) {
        if (source[cursor] === '\\') {
          cursor += 2
        } else if (source[cursor++] === '"') {
          break
        }
      }
      output += source.slice(start, cursor)
    } else if (source[cursor] === '-' || /[0-9]/.test(source[cursor])) {
      while (cursor < source.length && /[0-9eE+.\-]/.test(source[cursor])) cursor++
      const token = source.slice(start, cursor)
      output += /^-?\d+$/.test(token) && !Number.isSafeInteger(Number(token))
        ? JSON.stringify(token)
        : token
    } else {
      output += source[cursor++]
    }
  }
  return JSON.parse(output)
}
