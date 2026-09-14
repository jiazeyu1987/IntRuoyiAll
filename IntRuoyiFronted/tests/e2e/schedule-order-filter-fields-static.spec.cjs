const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(root, 'IntRuoyiFronted')
const backendRoot = path.join(root, 'IntRuoyiBackend')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const pageSource = read('IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue')
const apiSource = read('IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts')
const pageReqSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/vo/MesProScheduleOrderPageReqVO.java'
)
const serviceSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java'
)
const scheduleMapperSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderMapper.java'
)
const processMapperSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/process/MesProProcessMapper.java'
)

assert.equal(fs.existsSync(frontendRoot), true, '前端源码根目录必须存在。')
assert.equal(fs.existsSync(backendRoot), true, '后端源码根目录必须存在。')

assert.match(
  pageSource,
  /const scheduleOrderStatusOptions = \[[\s\S]*label: '待排产', value: 0[\s\S]*label: '已排产', value: 1[\s\S]*label: '生产中', value: 2[\s\S]*label: '已完成', value: 3[\s\S]*label: '已取消', value: 4[\s\S]*\]/,
  '排产工单页面必须声明正式订单状态选项。'
)

const requiredFilterContracts = [
  {
    label: '物料编码',
    key: 'productCode',
    type: 'text',
    queryParamKey: 'productCode',
    placeholder: '请输入物料编码'
  },
  {
    label: '物料名称',
    key: 'productName',
    type: 'text',
    queryParamKey: 'productName',
    placeholder: '请输入物料名称'
  },
  {
    label: '当前工序',
    key: 'currentProcessKeyword',
    type: 'text',
    queryParamKey: 'currentProcessKeyword',
    placeholder: '请输入工序编码或名称'
  },
  {
    label: '订单状态',
    key: 'status',
    type: 'select',
    queryParamKey: 'status',
    options: 'scheduleOrderStatusOptions'
  }
]

for (const filter of requiredFilterContracts) {
  const quickFilterRegex = new RegExp(
    `key:\\s*'${filter.key}'[\\s\\S]*label:\\s*'${filter.label}'[\\s\\S]*type:\\s*'${filter.type}'`
  )
  assert.match(pageSource, quickFilterRegex, `快速筛选必须包含${filter.label}。`)

  const multiFilterRegex = new RegExp(
    `key:\\s*'${filter.key}'[\\s\\S]*label:\\s*'${filter.label}'[\\s\\S]*type:\\s*'${filter.type}'[\\s\\S]*queryParamKey:\\s*'${filter.queryParamKey}'`
  )
  assert.match(pageSource, multiFilterRegex, `多条件筛选必须包含${filter.label}。`)

  if (filter.placeholder) {
    assert.match(pageSource, new RegExp(`placeholder:\\s*'${filter.placeholder}'`), `${filter.label}必须有明确输入提示。`)
  }
  if (filter.options) {
    assert.match(pageSource, new RegExp(`options:\\s*${filter.options}`), `${filter.label}必须使用正式选项。`)
  }
}

assert.match(
  pageSource,
  /currentProcessKeyword:\s*undefined as string \| undefined/,
  '页面查询参数必须持有 currentProcessKeyword。'
)
assert.match(pageSource, /productCode:\s*undefined as string \| undefined/, '页面查询参数必须持有 productCode。')
assert.match(pageSource, /productName:\s*undefined as string \| undefined/, '页面查询参数必须持有 productName。')
assert.match(pageSource, /status:\s*undefined as number \| undefined/, '页面查询参数必须持有 status。')
assert.match(apiSource, /currentProcessKeyword\?: string/, '前端 API 类型必须声明 currentProcessKeyword。')
assert.match(apiSource, /status\?: number/, '前端 API 类型必须声明 status。')
assert.match(pageReqSource, /private String productCode;/, '后端分页请求必须声明 productCode。')
assert.match(pageReqSource, /private String productName;/, '后端分页请求必须声明 productName。')
assert.match(pageReqSource, /private String currentProcessKeyword;/, '后端分页请求必须声明 currentProcessKeyword。')
assert.match(pageReqSource, /private Integer status;/, '后端分页请求必须保留 status。')

assert.match(
  serviceSource,
  /resolveSchedulePageProductIds\(MesProScheduleOrderPageReqVO pageReqVO,\s*List<Long> quickFilterProductIds\)/,
  '排产工单服务必须汇总正式物料编码和名称筛选。'
)
assert.match(
  serviceSource,
  /itemMapper\.selectListByCodeLike\(pageReqVO\.getProductCode\(\)\)/,
  '排产工单服务必须按物料编码解析正式物料身份。'
)
assert.match(
  serviceSource,
  /itemMapper\.selectListByNameLike\(pageReqVO\.getProductName\(\)\)/,
  '排产工单服务必须按物料名称解析正式物料身份。'
)
assert.match(
  serviceSource,
  /resolveCurrentProcessFilterIds\(MesProScheduleOrderPageReqVO pageReqVO\)[\s\S]*processMapper\.selectListByKeywordLike\(pageReqVO\.getCurrentProcessKeyword\(\)\)/,
  '排产工单服务必须按当前工序关键词解析正式工序身份。'
)
assert.match(
  serviceSource,
  /filterByCurrentProcess\(pageResult\.getList\(\), currentProcessFilterIds\)/,
  '排产工单服务必须用解析后的当前工序身份过滤列表。'
)
assert.match(
  serviceSource,
  /case "productCode", "productName", "productSpecification", "currentProcessKeyword" -> true;/,
  '物料和当前工序快速筛选必须在进入 Mapper 前由服务层正式解析。'
)
assert.match(
  scheduleMapperSource,
  /"status", QuickFilterUtils\.QuickFilterField\.integerSelect\(MesProScheduleOrderDO::getStatus\)/,
  '排产工单 Mapper 必须支持订单状态快速筛选。'
)
assert.match(
  processMapperSource,
  /selectListByKeywordLike\(String keyword\)[\s\S]*String searchText = keyword\.trim\(\);[\s\S]*like\(MesProProcessDO::getCode, searchText\)[\s\S]*or\(\)[\s\S]*like\(MesProProcessDO::getName, searchText\)/,
  '工序 Mapper 必须支持按工序编码或名称模糊解析当前工序。'
)

assert.doesNotMatch(pageSource, /catch\s*\{\s*\}/, '页面不得吞掉筛选相关异常。')

console.log('PASS: schedule order filter fields static contract')
