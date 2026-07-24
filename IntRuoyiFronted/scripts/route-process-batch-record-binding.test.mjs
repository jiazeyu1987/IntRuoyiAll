import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'

const root = path.resolve(import.meta.dirname, '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('RouteProcessList exposes batch record binding column and action', () => {
  const source = readText('src/views/mes/pro/route/RouteProcessList.vue')

  assert.match(source, /label="默认批记录"/, '组成工序列表应显示默认批记录列')
  assert.match(source, /绑定批记录/, '组成工序列表应提供绑定批记录入口')
  assert.match(source, /batchRecordReportId/, '路线工序页面应读写 batchRecordReportId 字段')
  assert.ok(
    source.indexOf('label="默认批记录"') < source.indexOf('label="工作站"'),
    '默认批记录列应排在工作站列左侧，避免首屏不可见'
  )
})

test('route process API type carries batch record binding fields', () => {
  const source = readText('src/api/mes/pro/route/process/index.ts')

  assert.match(source, /batchRecordReportId\?: string/, '路线工序接口类型应包含 batchRecordReportId')
  assert.match(source, /batchRecordReportCode\?: string/, '路线工序接口类型应包含 batchRecordReportCode')
  assert.match(source, /batchRecordReportName\?: string/, '路线工序接口类型应包含 batchRecordReportName')
})
