const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeApi = read('src/api/mes/pro/route/index.ts')
const wordImportPage = read('src/views/mes/pro/batchrecordformlist/index.vue')

assert.match(routeApi, /export type ProRouteVersionLifecycleStatus[\s\S]*'DRAFT'[\s\S]*'PENDING_APPROVAL'[\s\S]*'READY_TO_PUBLISH'[\s\S]*'ACTIVE'[\s\S]*'SUPERSEDED'[\s\S]*'REJECTED'[\s\S]*'CANCELLED'/,
  '前端必须声明完整路线版本生命周期状态。')
assert.match(routeApi, /export interface ProRouteVersionVO[\s\S]*lifecycleStatus:\s*ProRouteVersionLifecycleStatus/,
  '前端必须声明路线版本 VO，并暴露 lifecycleStatus。')
assert.match(routeApi, /PRO_ROUTE_VERSION_BASE_URL\s*=\s*'\/mes\/pro\/route-version'/,
  '前端必须集中声明 route-version API 基础路径。')
assert.match(routeApi, /getRouteVersionList:[\s\S]*\/list-by-route/,
  '前端必须提供路线版本列表 API。')
assert.match(routeApi, /createRouteCandidateVersion:[\s\S]*\/create-candidate/,
  '前端必须提供创建候选版本 API。')
assert.match(routeApi, /getRouteVersionBlockers:[\s\S]*\/blockers/,
  '前端必须提供发布 blocker 查询 API。')
assert.match(routeApi, /submitRouteCandidateVersion:[\s\S]*\/submit\?id=/,
  '前端必须提供候选提交 API。')
assert.match(routeApi, /cancelRouteCandidateVersion:[\s\S]*\/cancel\?id=/,
  '前端必须提供候选取消 API。')
assert.match(routeApi, /submitAndPublishRouteCandidateVersion:[\s\S]*\/submit-publish/,
  '前端必须提供提交发布审批 API。')
assert.doesNotMatch(routeApi, /signaturePassword/,
  '工艺路线提交发布 API 不得承载提交者电子签名密码。')
assert.match(routeApi, /submitAndPublishRouteCandidateVersion:[\s\S]*\/submit-publish\?id=\$\{data\.id\}/,
  '工艺路线提交发布 API 必须只用候选版本 ID 调用。')

assert.match(wordImportPage, /生成路线候选版本，待审批\/发布后生效/,
  'Word 重建产线必须提示生成路线候选版本，而不是直接提示 active 生效。')
assert.match(wordImportPage, /生成候选版本/,
  'Word 重建产线确认按钮必须表达候选版本语义。')

console.log('mes-pro-route-version-workflow-static PASS')
