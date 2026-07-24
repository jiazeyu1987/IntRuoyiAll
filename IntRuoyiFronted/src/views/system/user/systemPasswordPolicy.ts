export const SYSTEM_PASSWORD_MIN_LENGTH = 8
export const SYSTEM_PASSWORD_MESSAGE = '密码至少 8 位且必须包含英文和数字'

const LETTER_PATTERN = /[A-Za-z]/
const DIGIT_PATTERN = /\d/

export const isSystemPasswordStrong = (value: string | undefined | null) => {
  const password = value || ''
  return (
    password.length >= SYSTEM_PASSWORD_MIN_LENGTH &&
    LETTER_PATTERN.test(password) &&
    DIGIT_PATTERN.test(password)
  )
}

export const systemPasswordRule = {
  validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
    if (isSystemPasswordStrong(value)) {
      callback()
      return
    }
    callback(new Error(SYSTEM_PASSWORD_MESSAGE))
  },
  trigger: 'blur'
}
