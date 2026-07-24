const fs = require('fs')
const path = require('path')

const routerPath = path.resolve(__dirname, '../../src/router/modules/showroom.ts')
const routerSource = fs.readFileSync(routerPath, 'utf8')

for (const marker of ["path: 'keyword'", "name: 'ShowroomAdminKeyword'", "title: '关键词中英对照'"]) {
  if (!routerSource.includes(marker)) {
    throw new Error(`missing route marker "${marker}" in ${routerPath}`)
  }
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

for (const marker of [
  "<KeywordWorkbench v-else-if=\"activeSection === 'keyword'\" />",
  "import KeywordWorkbench from './keyword/KeywordWorkbench.vue'",
  "{ name: 'keyword', routeName: 'ShowroomAdminKeyword' }"
]) {
  if (!indexSource.includes(marker)) {
    throw new Error(`missing keyword workspace marker "${marker}" in ${indexPath}`)
  }
}

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const marker of [
  'export interface ShowroomKeywordPageReqVO extends PageParam',
  'export interface ShowroomKeywordPageRowRespVO',
  'export interface ShowroomKeywordRespVO',
  'export interface ShowroomKeywordSaveReqVO',
  'getKeywordPage: async (params: ShowroomKeywordPageReqVO)',
  'getKeyword: async (id: number)',
  'createKeyword: async (data: ShowroomKeywordSaveReqVO)',
  'updateKeyword: async (data: ShowroomKeywordSaveReqVO)',
  'deleteKeyword: async (id: number)'
]) {
  if (!apiSource.includes(marker)) {
    throw new Error(`missing keyword API marker "${marker}" in ${apiPath}`)
  }
}

const workbenchPath = path.resolve(__dirname, '../../src/views/showroom-admin/keyword/KeywordWorkbench.vue')
if (!fs.existsSync(workbenchPath)) {
  throw new Error(`missing keyword workbench file ${workbenchPath}`)
}
const workbenchSource = fs.readFileSync(workbenchPath, 'utf8')

for (const marker of [
  '关键词中英对照',
  '新增关键词',
  '中文关键词',
  'English Keyword',
  '更新时间',
  '查询',
  '重置',
  '编辑',
  '删除',
  'await ShowroomAdminApi.getKeywordPage',
  'await ShowroomAdminApi.createKeyword',
  'await ShowroomAdminApi.updateKeyword',
  'await ShowroomAdminApi.deleteKeyword'
]) {
  if (!workbenchSource.includes(marker)) {
    throw new Error(`missing keyword workbench marker "${marker}" in ${workbenchPath}`)
  }
}

if (!/message\.error\(resolved\.message\)[\s\S]*throw resolved/.test(workbenchSource)) {
  throw new Error(`keyword workbench must surface and rethrow real API failures in ${workbenchPath}`)
}

console.log('PASS: showroom keyword admin static wiring is present')
