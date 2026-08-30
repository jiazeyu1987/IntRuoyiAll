const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(frontendRoot, '..', 'IntRuoyiBackend')

const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) =>
  fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const userApiSource = readFrontend('src/api/system/user/index.ts')
const userPageSource = readFrontend('src/views/system/user/index.vue')
const lifecycleFormPath = path.join(
  frontendRoot,
  'src/views/system/user/UserLifecycleDeactivateForm.vue'
)
const userControllerSource = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/user/UserController.java'
)

assert.ok(
  userApiSource.includes('UserLifecycleDeactivateReqVO') &&
    userApiSource.includes('recordUserLifecycleDeactivation') &&
    userApiSource.includes("/system/user/lifecycle-deactivation"),
  'System user API must expose lifecycle deactivation registration.'
)

assert.ok(
  userPageSource.includes('UserLifecycleDeactivateForm') &&
    userPageSource.includes('@click="openLifecycleDeactivate(scope.row)"') &&
    userPageSource.includes('离职/转岗') &&
    userPageSource.includes('lifecycleDocumentNo') &&
    userPageSource.includes('lifecycleEffectiveTime') &&
    userPageSource.includes('lifecycleDeactivatedTime'),
  'System user page must expose lifecycle columns and a row action for resignation/transfer deactivation.'
)

assert.ok(fs.existsSync(lifecycleFormPath), 'Lifecycle deactivation form component must exist.')
const lifecycleFormSource = fs.readFileSync(lifecycleFormPath, 'utf8')

assert.ok(
  lifecycleFormSource.includes('documentType') &&
    lifecycleFormSource.includes('RESIGNATION') &&
    lifecycleFormSource.includes('TRANSFER') &&
    lifecycleFormSource.includes('documentNo') &&
    lifecycleFormSource.includes('documentTime') &&
    lifecycleFormSource.includes('effectiveTime'),
  'Lifecycle form must collect document type, number, document time, and effective time.'
)

assert.ok(
  lifecycleFormSource.includes('recordUserLifecycleDeactivation') &&
    lifecycleFormSource.includes("emit('success')"),
  'Lifecycle form must call the registration API and refresh the user list on success.'
)

assert.ok(
  userControllerSource.includes('@PutMapping("/lifecycle-deactivation")'),
  'Backend controller must provide the lifecycle deactivation endpoint used by the frontend.'
)

console.log('PASS: system user lifecycle deactivation static contract')
