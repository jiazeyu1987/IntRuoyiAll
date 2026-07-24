const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(root, '..')

const read = (absolutePath) => fs.readFileSync(absolutePath, 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message)
  }
}

const apiPath = path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts')
const historyPagePath = path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const timelineVoPath = path.join(
  repoRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionReviewTimelineRespVO.java'
)

for (const filePath of [apiPath, historyPagePath, timelineVoPath]) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`缺少文件：${path.relative(repoRoot, filePath)}`)
  }
}

const api = read(apiPath)
const historyPage = read(historyPagePath)
const timelineVo = read(timelineVoPath)

assertIncludes(
  timelineVo,
  'private List<DossierItem> dossierItems;',
  'review-timeline 后端响应必须包含批次卷宗项'
)
assertIncludes(
  api,
  'export interface EdhrBatchExecutionDossierItemRespVO',
  '前端 API 类型必须声明批次卷宗项'
)
assertIncludes(
  api,
  'dossierItems?: EdhrBatchExecutionDossierItemRespVO[]',
  'review-timeline 前端类型必须包含 dossierItems'
)
assertIncludes(
  historyPage,
  'dossierItems',
  '历史批记录页必须消费 review-timeline 的 dossierItems'
)
assertIncludes(
  historyPage,
  'edhr-batch-history__dossier-section',
  '历史批记录页必须展示归档目录区域'
)
assertIncludes(historyPage, '成品检', '历史批记录页必须展示成品检目录项')
assertIncludes(historyPage, 'sourceDocHash', '历史批记录页必须展示成品检来源 hash')
assertIncludes(historyPage, 'sourceDocResult', '历史批记录页必须展示成品检结果')

console.log('eDHR final inspection dossier static checks passed')
