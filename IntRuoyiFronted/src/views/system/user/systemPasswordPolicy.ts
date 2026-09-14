export const SYSTEM_PASSWORD_MIN_LENGTH = 8
export const SYSTEM_PASSWORD_MESSAGE = '密码至少 8 位且必须包含大写字母、小写字母、数字和特殊字符'

const UPPERCASE_PATTERN = /[A-Z]/
const LOWERCASE_PATTERN = /[a-z]/
const DIGIT_PATTERN = /\d/
const SPECIAL_CHAR_PATTERN = /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>\/?`~]/

export const isSystemPasswordStrong = (value: string | undefined | null) => {
  const password = value || ''
  return (
    password.length >= SYSTEM_PASSWORD_MIN_LENGTH &&
    UPPERCASE_PATTERN.test(password) &&
    LOWERCASE_PATTERN.test(password) &&
    DIGIT_PATTERN.test(password) &&
    SPECIAL_CHAR_PATTERN.test(password)
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
