import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const listPage = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')

const extractDefaultColumnsBlock = () => {
  const marker = 'const edhrBatchExecutionDefaultColumns'
  const start = listPage.indexOf(marker)
  assert.notEqual(start, -1, '批次执行列表必须声明默认列配置')
  const end = listPage.indexOf('const {', start)
  assert.notEqual(end, -1, '默认列配置后应继续声明 useUserTableColumns')
  return listPage.slice(start, end)
}

const extractTableTemplateBlock = () => {
  const start = listPage.indexOf('<el-table')
  assert.notEqual(start, -1, '批次执行列表必须渲染 el-table')
  const end = listPage.indexOf('</el-table>', start)
  assert.notEqual(end, -1, '批次执行列表 el-table 必须闭合')
  return listPage.slice(start, end)
}

test('BDD: 批次执行列表不显示黄框标出的运行态列', () => {
  const tableBlock = extractTableTemplateBlock()

  for (const forbidden of [
    'label="当前工序"',
    'prop="currentProcess"',
    "isEdhrBatchExecutionColumnVisible('currentProcess')",
    'label="当前填写人"',
    'prop="currentFillers"',
    "isEdhrBatchExecutionColumnVisible('currentFillers')",
    'label="完成进度"',
    'prop="progress"',
    "isEdhrBatchExecutionColumnVisible('progress')"
  ]) {
    assert.equal(tableBlock.includes(forbidden), false, `列表表格不应再包含 ${forbidden}`)
  }
})

test('BDD: 显示字段配置不再暴露已隐藏运行态列', () => {
  const defaultColumnsBlock = extractDefaultColumnsBlock()

  for (const forbidden of [
    "key: 'currentProcess'",
    "label: '当前工序'",
    "key: 'currentFillers'",
    "label: '当前填写人'",
    "key: 'progress'",
    "label: '完成进度'"
  ]) {
    assert.equal(defaultColumnsBlock.includes(forbidden), false, `默认显示字段不应再包含 ${forbidden}`)
  }
})

test('BDD: 批次执行列表保留核心打开和追踪列', () => {
  const defaultColumnsBlock = extractDefaultColumnsBlock()
  const tableBlock = extractTableTemplateBlock()

  for (const required of [
    "key: 'batchExecutionCode'",
    "key: 'workOrderCode'",
    "key: 'product'",
    "key: 'route'",
    "key: 'status'",
    "key: 'updateTime'",
    "key: 'operation'"
  ]) {
    assert.match(defaultColumnsBlock, new RegExp(required.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.match(tableBlock, /openDetail\(row\)/, '列表仍需保留打开批次详情入口')
})
