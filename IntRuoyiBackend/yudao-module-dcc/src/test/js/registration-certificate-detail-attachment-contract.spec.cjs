const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const detailModel = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateDetail.java'
)
const queryRecord = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryRecord.java'
)
const queryMapper = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java'
)
const queryService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/query/DccRegistrationCertificateQueryServiceImpl.java'
)

assert.match(
  detailModel,
  /private\s+String\s+registrationFileName\s*;/,
  'detail response must expose the original name of the formal registration attachment'
)
assert.match(
  queryRecord,
  /private\s+String\s+registrationFileName\s*;/,
  'query record must carry the formal registration attachment original name'
)
assert.match(
  queryMapper,
  /SELECT\s+f\.original_name[\s\S]{0,420}f\.owner_type\s*=\s*'VERSION'[\s\S]{0,220}f\.file_kind\s*=\s*'REGISTRATION_CERTIFICATE'[\s\S]{0,180}f\.status\s*=\s*'BOUND'[\s\S]{0,180}AS\s+registration_file_name/i,
  'detail query must select the original name from the same formal version attachment contract'
)
assert.match(
  queryService,
  /\.registrationFileName\(row\.getRegistrationFileName\(\)\)/,
  'detail mapper must expose the queried formal attachment original name'
)
