const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const backendRoot = path.resolve(repoRoot, '..', 'ruoyi-vue-pro')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function readBackend(relativePath) {
  return fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
}

const api = read('src/api/srm/nas-locator/index.ts')
const page = read('src/views/srm/nas-locator/index.vue')
const sql = [
  readBackend('sql/mysql/20260628_srm_t6_nas_locator.sql'),
  readBackend('sql/mysql/20260701_srm_t6_nas_locator_blacklist_config.sql')
].join('\n')

for (const snippet of [
  '/srm/nas-locator/status',
  '/srm/nas-locator/page',
  '/srm/nas-locator/refresh',
  '/srm/nas-locator/blacklist',
  '/srm/nas-locator/download',
  'SrmNasLocatorBlacklistRespVO',
  'SrmNasLocatorBlacklistSaveReqVO',
  'SrmNasLocatorStatusRespVO',
  'SrmNasLocatorPageReqVO',
  'SrmNasLocatorFileRespVO',
  'runningShare',
  'runningPath',
  'runningDirectoryCount',
  'runningFileCount',
  'runningShareIndex',
  'runningShareTotal',
  'decodeContentDispositionFileName',
  'downloadByData'
]) {
  assert(api.includes(snippet), `missing API endpoint or type: ${snippet}`)
}

for (const snippet of [
  '共享范围',
  '质量体系文件',
  '生产部',
  '索引根路径',
  '最近成功刷新',
  '目录数 / 文件数',
  '最新任务状态',
  '运行进度',
  '当前共享',
  '当前目录',
  '已扫描目录 / 文件',
  '关键词',
  '*MO13*.pdf',
  '*.pyc',
  '搜索',
  '刷新',
  '详情',
  '黑名单',
  '黑名单已保存，刷新索引后生效',
  '文件名',
  'NAS目录',
  '完整相对路径',
  '修改时间',
  '大小',
  '下载',
  'el-dialog',
  'el-progress',
  'nas-locator-status-dialog',
  'nas-locator-blacklist-dialog',
  'nas-locator-toolbar-button',
  "v-hasPermi=\"['srm:nas-locator:refresh']\"",
  "v-hasPermi=\"['srm:nas-locator:config']\"",
  "v-hasPermi=\"['srm:nas-locator:download']\"",
  "message.success('NAS 索引刷新成功')",
  "message.success('黑名单已保存，刷新索引后生效')",
  "message.error(resolveErrorMessage(error, 'NAS 文件搜索失败，请检查索引状态后重试。'))",
  "message.error(resolveErrorMessage(error, 'NAS 文件下载失败，请稍后重试。'))",
  "formatDate(new Date(value), 'YYYY-MM-DD HH:mm')",
  'formatFileSize(value)'
]) {
  assert(page.includes(snippet), `missing page contract: ${snippet}`)
}

for (const snippet of [
  '@submit.prevent="handleQuery"',
  'native-type="submit"',
  '<el-button\n          type="primary"\n          native-type="button"',
  '<Icon icon="ep:info-filled"',
  '<Icon icon="ep:remove-filled"',
  'const statusDialogVisible = ref(false)',
  'const blacklistDialogVisible = ref(false)',
  'const openStatusDialog = () => {',
  'const openBlacklistDialog = async () => {',
  'statusDialogVisible.value = true',
  'blacklistDialogVisible.value = true',
  'class="nas-locator-toolbar-button"',
  'class="nas-locator-status-dialog"',
  'class="nas-locator-blacklist-dialog"'
]) {
  assert(page.includes(snippet), `missing enter-search contract: ${snippet}`)
}

const refreshButtonIndex = page.indexOf('@click="handleRefresh"')
const detailButtonIndex = page.indexOf('@click="openStatusDialog"')
const blacklistButtonIndex = page.indexOf('@click="openBlacklistDialog"')
assert(refreshButtonIndex >= 0, 'missing refresh button contract')
assert(detailButtonIndex > refreshButtonIndex, 'missing refresh-right-side detail button contract')
assert(blacklistButtonIndex > detailButtonIndex, 'missing blacklist button contract')

assert(
  /\.nas-locator-toolbar-button\s*\{[\s\S]*?min-width:\s*90px;[\s\S]*?\}/m.test(page),
  'missing shared toolbar button sizing contract'
)

for (const snippet of [
  'srm_nas_locator_refresh_task',
  'srm_nas_locator_entry',
  "SELECT 991100, 'NAS定位'",
  "SELECT 991101, 'NAS定位查询'",
  "SELECT 991102, 'NAS定位刷新'",
  "SELECT 991103, 'NAS定位下载'",
  "SELECT 991104, 'NAS定位黑名单'",
  "`component` = 'srm/nas-locator/index'",
  "`component_name` = 'SrmNasLocator'",
  "'srm:nas-locator:query'",
  "'srm:nas-locator:refresh'",
  "'srm:nas-locator:config'",
  "'srm:nas-locator:download'",
  'uk_srm_nas_locator_entry_task_type_path_hash',
  'idx_srm_nas_locator_refresh_task_tenant_status',
  'idx_srm_nas_locator_refresh_task_tenant_finish',
  'idx_srm_nas_locator_entry_tenant_task_type',
  'idx_srm_nas_locator_entry_tenant_name',
  'JSON_VALID',
  'Invalid system_tenant_package.menu_ids JSON',
  'Missing SRM nas-locator route menu for get-permission-info'
]) {
  assert(sql.includes(snippet), `missing SQL contract: ${snippet}`)
}

assert(!/catch\s*\{\s*\}/.test(`${api}\n${page}`), 'frontend must not contain empty catch blocks')
assert(!/mock|fallback|默认成功|空列表/i.test(`${api}\n${page}`), 'frontend must not introduce mock or downgrade semantics')
assert(!/window\.setInterval|startStatusPolling|stopStatusPolling|pollTimer/.test(page), 'page must not contain frontend auto-refresh polling')
assert(!/INSERT IGNORE|ON DUPLICATE KEY UPDATE|DROP TABLE|TRUNCATE TABLE|INSERT INTO `ERP_|UPDATE `ERP_|DELETE FROM `ERP_|INSERT INTO `K3_|UPDATE `K3_|DELETE FROM `K3_|INSERT INTO `FINANCE_|UPDATE `FINANCE_|DELETE FROM `FINANCE_/i.test(sql), 'SQL must stay fail-fast and avoid ERP/K3/finance writes')

console.log('PASS: SRM T6 NAS locator static contract')
