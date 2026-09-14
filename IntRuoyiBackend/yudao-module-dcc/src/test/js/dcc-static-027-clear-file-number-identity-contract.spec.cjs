const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const service = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileMetadataUpdateServiceImpl.java')
const masterMapper = read('src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMasterMapper.java')
const controllerTest = read('src/test/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileMetadataUpdateControllerTest.java')

assert.match(
  controllerTest,
  /fileNumber is optional because not every controlled file has a number/,
  'metadata update boundary must continue to allow blank file numbers'
)

const updateMetadata = extract(
  service,
  'public void updateMetadata',
  'private boolean hasDocControlRole',
  'metadata update method'
)

assert.doesNotMatch(
  updateMetadata,
  /controlledFileMasterMapper\.updateById\s*\(/,
  'metadata update must not clear nullable Master identity fields through MyBatis Plus updateById'
)
assert.match(
  updateMetadata,
  /controlledFileMasterMapper\.updateMetadataIdentity\s*\(/,
  'metadata update must use explicit SQL for Master identity projection updates'
)

assert.match(
  masterMapper,
  /int\s+updateMetadataIdentity\s*\(/,
  'Master mapper must expose an explicit metadata identity update method'
)
assert.match(
  masterMapper,
  /normalized_file_number\s*=\s*#\{normalizedFileNumber\}/,
  'explicit Master update must write normalized_file_number even when the value is null'
)
assert.match(
  masterMapper,
  /file_number\s*=\s*#\{fileNumber\}/,
  'explicit Master update must write file_number when a file number is cleared to blank'
)
assert.match(
  masterMapper,
  /dcc_project_code_id\s*=\s*#\{dccProjectCodeId\}/,
  'explicit Master update must keep project identity in sync with metadata updates'
)
assert.match(
  masterMapper,
  /file_type_taxonomy_leaf_id\s*=\s*#\{fileTypeTaxonomyLeafId\}/,
  'explicit Master update must keep taxonomy identity in sync with metadata updates'
)

console.log('DCC-STATIC-027 clear file number identity contract passed')
