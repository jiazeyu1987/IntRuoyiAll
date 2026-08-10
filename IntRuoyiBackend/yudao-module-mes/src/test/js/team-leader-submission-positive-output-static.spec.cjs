const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const pageReq = readUtf8(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelinePageReqVO.java'
)
const workbenchService = readUtf8(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImpl.java'
)
const mapperXml = readUtf8(
  'src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)

assert.match(
  pageReq,
  /private\s+Boolean\s+requirePositiveOutputQuantity;/,
  'timeline page request must expose the internal positive-output-quantity filter'
)
assert.match(
  workbenchService,
  /LEADER_TYPE_PRODUCTION[\s\S]*reqVO\.setRequirePositiveOutputQuantity\(Boolean\.TRUE\)/,
  'production leader submission pages must require a positive output quantity'
)
assert.match(
  mapperXml,
  /<if test="reqVO\.requirePositiveOutputQuantity == true">[\s\S]*JSON_EXTRACT\(\s*pool_event\.raw_payload, '\$\.outputQuantity'\)[\s\S]*DECIMAL\(24, 6\)[\s\S]*<!\[CDATA\[>\]\]> 0[\s\S]*<\/if>/,
  'timeline count and page queries must exclude production events without a positive output quantity'
)

console.log('team-leader-submission-positive-output-static PASS')
