import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('showroom admin version browser history artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/history/index.ts',
    'src/views/showroom-admin/history/contracts.ts',
    'src/views/showroom-admin/history/VersionDiffDrawer.vue',
    'src/views/showroom-admin/history/CompanyHistoryWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('history workbench acts as a cross-target version browser with explicit contract gaps', () => {
  const source = readText('src/views/showroom-admin/history/CompanyHistoryWorkbench.vue')

  for (const token of [
    'ShowroomAdminApi.getCompanyHistory',
    'ShowroomAdminApi.getProductPage',
    'ShowroomAdminApi.getProductHistory',
    'ShowroomAdminApi.getHallPage',
    "url: '/showroom/narration/get'",
    'ShowroomFrontstageApi.getDisplayProduct',
    'ShowroomFrontstageApi.getDisplayHome',
    'COMPANY',
    'PRODUCT',
    'NARRATION',
    'PREVIEW_ASSET'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  for (const copyToken of [
    '目标类型',
    '目标对象',
    '讲解语言',
    '公司内容',
    '产品内容',
    '讲解资产',
    '预览资产',
    '后端未提供讲解历史列表接口',
    '后端未提供预览资产历史列表接口',
    '当前只支持读取最新讲解快照',
    '当前只支持读取 live 预览资产快照'
  ]) {
    assert.match(source, new RegExp(copyToken))
  }

  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /fallback/i)
})
