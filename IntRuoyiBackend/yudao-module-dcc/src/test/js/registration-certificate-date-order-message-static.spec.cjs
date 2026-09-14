const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const errorCodePath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java'
)
const commandServiceTestPath = path.join(
  moduleRoot,
  'src/test/java/cn/iocoder/yudao/module/dcc/registrationcertificate/DccRegistrationCertificateCommandServiceTest.java'
)

const errorCodes = fs.readFileSync(errorCodePath, 'utf8')
const commandServiceTest = fs.readFileSync(commandServiceTestPath, 'utf8')
const expectedMessage = '注册证日期顺序不正确：首次获证日期不能晚于生效日期，生效日期必须早于有效期至'

assert.match(
  errorCodes,
  new RegExp(
    `REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID\\s*=\\s*new ErrorCode\\(1_080_000_232,\\s*"${expectedMessage}"`
  ),
  'date order error should return a Chinese business message'
)

assert.match(
  commandServiceTest,
  new RegExp(`assertEquals\\("${expectedMessage}",\\s*order\\.getMessage\\(\\)\\)`),
  'date order backend test should assert the Chinese message contract'
)

console.log('registration certificate date order message static contract passed')
