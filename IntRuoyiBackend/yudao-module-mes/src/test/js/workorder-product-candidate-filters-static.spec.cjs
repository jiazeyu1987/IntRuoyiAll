const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')

const read = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')

const pageReqSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/vo/MesProWorkOrderPageReqVO.java'
)
const serviceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/MesProWorkOrderServiceImpl.java'
)
const mapperSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/workorder/MesProWorkOrderMapper.java'
)
const testSource = read(
  'src/test/java/cn/iocoder/yudao/module/mes/service/pro/workorder/MesProWorkOrderServiceImplTest.java'
)

assert(
  pageReqSource.includes('private Long productNameFilterId;') &&
    pageReqSource.includes('private Long productCodeFilterId;') &&
    pageReqSource.includes('private String productNameKeyword;') &&
    pageReqSource.includes('private String productCodeKeyword;'),
  'Work order page request VO must expose product name/code candidate filter IDs and raw keywords.'
)

assert(
  serviceSource.includes('resolveProductSearchIds(MesProWorkOrderPageReqVO pageReqVO)') &&
    serviceSource.includes('pageReqVO.getProductNameFilterId()') &&
    serviceSource.includes('pageReqVO.getProductCodeFilterId()') &&
    serviceSource.includes('pageReqVO.getProductNameKeyword()') &&
    serviceSource.includes('pageReqVO.getProductCodeKeyword()'),
  'Work order service must include product name/code candidate IDs and raw keywords when resolving product filters.'
)

assert(
  serviceSource.includes('if (!allSame)') &&
    serviceSource.includes('return List.of(-1L);') &&
    serviceSource.includes('resolveKeywordProductIds(MesProWorkOrderPageReqVO pageReqVO)'),
  'Conflicting product filters must not silently downgrade to one selected product.'
)

assert(
  mapperSource.includes('resolveSingleProductId(reqVO)') &&
    mapperSource.includes('reqVO.getProductNameFilterId()') &&
    mapperSource.includes('reqVO.getProductCodeFilterId()'),
  'Work order mapper must use candidate filter IDs when no expanded product ID collection is present.'
)

assert(
  testSource.includes('getWorkOrderPage_shouldUseProductNameAndCodeCandidateFilterIds') &&
    testSource.includes('getWorkOrderPage_shouldUseProductNameKeywordWhenCandidateNotSelected') &&
    testSource.includes('getWorkOrderPage_shouldReturnEmptyWhenProductCandidateFiltersConflict'),
  'Backend tests must cover matching candidate IDs, raw keyword search, and conflicting product filters.'
)

console.log('PASS: backend work order product candidate filters static contract')
