const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const homeView = read('src/views/erp/home/index.vue')
const configApi = read('src/api/erp/config/index.ts')

assert(
  homeView.includes("checkRole(['super_admin'])"),
  'ERP 首页写权限开关必须仅 super_admin 可见'
)
assert(
  homeView.includes('v-if="isSuperAdmin"'),
  'ERP 首页写权限开关容器必须绑定管理员可见条件'
)
assert(
  homeView.includes('ErpKingdeeConfigApi.getExternalWritePermission()'),
  'ERP 首页必须读取后端写权限状态'
)
assert(
  homeView.includes('ErpKingdeeConfigApi.updateExternalWritePermission'),
  'ERP 首页必须通过后端接口保存写权限状态'
)
assert(
  !configApi.includes('/erp/kingdee-config/external-write-permission'),
  'ERP 写权限开关不能调用 /erp/** 接口，否则 ERP 模块禁用时会被后端兜底拦截'
)
assert(
  configApi.includes('/infra/external-write-permission/erp'),
  'ERP 写权限开关必须调用始终可用的基础配置接口 /infra/external-write-permission/erp'
)
assert(
  homeView.includes('@click="toggleExternalWritePermission"'),
  'ERP 写权限开关必须绑定点击切换处理，点击打开状态应提交关闭，点击关闭状态应提交打开'
)
assert(
  /const\s+toggleExternalWritePermission\s*=\s*async\s*\(\)\s*=>[\s\S]*?const\s+previousEnabled\s*=\s*externalWriteEnabled\.value[\s\S]*?const\s+nextEnabled\s*=\s*!previousEnabled[\s\S]*?externalWriteEnabled\.value\s*=\s*nextEnabled[\s\S]*?updateExternalWritePermission\(\{\s*enabled:\s*nextEnabled\s*\}\)/.test(
    homeView
  ),
  'ERP 写权限点击处理必须基于当前状态取反并提交 enabled=取反后的状态'
)
assert(
  /catch\s*\([\s\S]*?\)\s*\{[\s\S]*?externalWriteEnabled\.value\s*=\s*previousEnabled[\s\S]*?message\.error/.test(
    homeView
  ),
  'ERP 写权限保存失败时必须恢复点击前状态并显示失败提示'
)
assert(
  homeView.includes(":type=\"externalWriteEnabled ? 'success' : 'danger'\""),
  'ERP 写权限状态标签必须允许写入为绿色 success，禁止写入为红色 danger'
)
assert(
  !homeView.includes(":type=\"externalWriteEnabled ? 'danger' : 'success'\""),
  'ERP 写权限状态标签不能允许写入为红色、禁止写入为绿色'
)
assert(
  /if\s*\(\s*nextEnabled\s*\)\s*\{[\s\S]*?message\.success\('ERP 写权限已打开'\)[\s\S]*?\}\s*else\s*\{[\s\S]*?message\.error\('ERP 写权限已关闭'\)/.test(
    homeView
  ),
  'ERP 写权限切换提示必须打开为绿色 success toast，关闭为红色 error toast'
)
console.log('ERP external write switch static checks passed')
