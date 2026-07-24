const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const pagePath = path.resolve(frontendRoot, 'src/views/mes/pro/feedback/index.vue')
const reqVoPath = path.resolve(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/importrecord/MesProFeedbackImportRecordPageReqVO.java'
)
const mapperPath = path.resolve(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/feedback/MesProFeedbackImportRecordMapper.java'
)

for (const filePath of [pagePath, reqVoPath, mapperPath]) {
  assert(fs.existsSync(filePath), `模拟报工当前批次过滤相关文件必须存在：${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const reqVoSource = fs.readFileSync(reqVoPath, 'utf8')
const mapperSource = fs.readFileSync(mapperPath, 'utf8')

for (const fragment of [
  'currentImportRecordIds',
  'buildImportRecordPageParams',
  'currentImportRecordIds.value = [...result.importRecordIds]',
  'importRecordIds: currentImportRecordIds.value',
  'currentImportRecordIds.value = []',
  'attributionStatus: undefined as string | undefined'
]) {
  assert(pageSource.includes(fragment), `待归属列表必须按本次导入记录批次过滤：${fragment}`)
}

assert(
  /handleImportSuccess[\s\S]*currentImportRecordIds\.value = \[\.\.\.result\.importRecordIds\][\s\S]*getImportRecordList/.test(
    pageSource
  ),
  '导入成功后必须先记录本批 importRecordIds，再刷新待归属列表。'
)
assert(
  /getImportRecordList[\s\S]*ProFeedbackApi\.getImportRecordPage\(buildImportRecordPageParams\(\)\)/.test(
    pageSource
  ),
  '待归属分页请求必须统一使用带本批 importRecordIds 的查询参数。'
)
assert(
  /resetImportQuery[\s\S]*currentImportRecordIds\.value = \[\]/.test(pageSource),
  '用户点击重置时必须清空本批过滤，才允许查看历史待归属。'
)
assert(reqVoSource.includes('private List<Long> importRecordIds;'), '后端分页 Request VO 必须接收本批导入记录 ID 列表。')
assert(
  mapperSource.includes('.inIfPresent(MesProFeedbackImportRecordDO::getId, reqVO.getImportRecordIds())'),
  '后端待归属分页必须按 importRecordIds 做 IN 过滤。'
)

console.log('PASS: MES feedback import current batch static contract')
