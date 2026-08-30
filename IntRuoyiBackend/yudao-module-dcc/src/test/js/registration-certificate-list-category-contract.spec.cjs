const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const pageItem = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificatePageItem.java'
)
const oldIndexItem = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateOldIndexItem.java'
)
const queryService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java'
)
const queryMapper = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java'
)
const queryServiceTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/registrationcertificate/DccRegistrationCertificateQueryServiceTest.java'
)

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const pageItemMapping = extract(queryService, 'private DccRegistrationCertificatePageItem pageItem', 'private DccRegistrationCertificateDetail detail', 'page item mapping')
const oldIndexMapping = extract(queryService, 'private static DccRegistrationCertificateOldIndexItem oldIndexItem', 'private static DccRegistrationCertificatePageQuery normalize', 'old index mapping')

assert.match(
  queryMapper,
  /\bv\.classification\b/,
  'registration certificate query mapper must select the formal category from version classification'
)
assert.match(
  pageItem,
  /private String classification;/,
  'current registration-certificate page item must expose category'
)
assert.match(
  oldIndexItem,
  /private String classification;/,
  'old registration-certificate index item must expose category'
)
assert.match(
  pageItemMapping,
  /\.classification\(row\.getClassification\(\)\)/,
  'current registration-certificate page mapping must return the formal category field'
)
assert.match(
  oldIndexMapping,
  /\.classification\(row\.getClassification\(\)\)/,
  'old registration-certificate index mapping must return the formal category field'
)
assert.doesNotMatch(
  pageItemMapping + oldIndexMapping,
  /classification[\s\S]{0,120}(default|fallback|orElse|Optional|""|'')/,
  'registration certificate category list mapping must not invent a fallback category'
)
assert.match(
  queryServiceTest,
  /getClassification\(\)/,
  'JUnit query service coverage must assert the returned category value'
)

console.log('registration certificate list category backend contract: PASS')
