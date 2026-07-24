const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const servicePath = path.join(repoRoot, 'src/config/axios/service.ts')

const source = fs.readFileSync(servicePath, 'utf8')

const moduleDisabledMatcher =
  /const\s+isModuleDisabledReminder\s*=\s*\(message:\s*string\)\s*=>[\s\S]*?已禁用[\s\S]*?参考/

if (!moduleDisabledMatcher.test(source)) {
  throw new Error('请求拦截器必须识别“模块已禁用/参考开启”这类提示消息')
}

const reminderWarningBranch =
  /if\s*\(\s*isModuleDisabledReminder\(msg\)\s*\)\s*\{\s*ElMessage\.warning\(msg\)\s*\}\s*else\s*\{\s*ElMessage\.error/

if (!reminderWarningBranch.test(source)) {
  throw new Error('模块已禁用提醒必须使用 warning 提示，不能使用红叉 error')
}

const reminderErrorBranch =
  /if\s*\(\s*isModuleDisabledReminder\(msg\)\s*\)\s*\{\s*ElMessage\.error/

if (reminderErrorBranch.test(source)) {
  throw new Error('模块已禁用提醒不允许走 ElMessage.error')
}

console.log('PASS: module disabled reminders use warning style instead of error style')
