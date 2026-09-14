const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const panel = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')
const activeOrderMapper = fs
  .readFileSync(
    path.join(
      workspaceRoot,
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')
const pqcContextService = fs
  .readFileSync(
    path.join(
      workspaceRoot,
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const extractMethod = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `missing method: ${signature}`)
  const openBrace = source.indexOf('{', start)
  assert.ok(openBrace > start, `missing method body: ${signature}`)
  let depth = 0
  for (let index = openBrace; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  assert.fail(`unterminated method: ${signature}`)
}

const selectAllActiveOrders = extractMethod(
  activeOrderMapper,
  'default List<MesProcessPoolActiveOrderDO> selectActiveList()'
)
assert.match(
  selectAllActiveOrders,
  /eq\(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE"\)/,
  'PQC unified order source must query ACTIVE orders.'
)
assert.doesNotMatch(
  selectAllActiveOrders,
  /getLeaderUserId|leaderUserId|loginUserId/,
  'PQC unified order source must not be limited to one production leader or the login user.'
)

const listActiveOrders = extractMethod(
  pqcContextService,
  'public List<MesFrontlineActiveOrderCandidate> listActiveOrders()'
)
assert.match(
  listActiveOrders,
  /activeOrderMapper\.selectActiveList\(\)/,
  'PQC context must use the all-production-leader ACTIVE order collection.'
)
assert.doesNotMatch(
  listActiveOrders,
  /selectActiveListByLeader|leaderUserId|loginUserId/,
  'PQC context must not narrow the collection to one production leader.'
)

const pickerStart = panel.indexOf('v-if="activePicker && isPqcMode"')
const pickerEnd = panel.indexOf('</section>', pickerStart)
assert.ok(pickerStart >= 0 && pickerEnd > pickerStart, 'PQC picker must exist.')
const picker = panel.slice(pickerStart, pickerEnd)

for (const token of [
  'v-if="activePicker === \'order\'"',
  'v-model="activeOrderKeyword"',
  'type="search"',
  'data-pqc-order-search-input',
  'aria-label="输入订单号筛选活跃订单"',
  '@keydown.enter="handleActiveOrderSearchEnter"',
  'v-if="activePicker === \'order\' && pickerOptions.length === 0"',
  '{{ activeOrderPickerEmptyText }}'
]) {
  assert.ok(picker.includes(token), `PQC order picker must include: ${token}`)
}

assert.match(
  panel,
  /const activeOrderPickerEmptyText = computed\([\s\S]*FRONTLINE_PQC_NO_PENDING_ORDER_TEXT[\s\S]*'未找到匹配的待检工单'/,
  'PQC order picker must distinguish no active orders from a search with no matching active order.'
)

assert.match(
  panel,
  /const filteredActiveOrderOptions = computed\([\s\S]*deviceState\.activeOrderOptions[\s\S]*workOrderCode[\s\S]*includes\(keyword\)/,
  'Order-number input must filter the already loaded unified ACTIVE order collection.'
)
assert.match(
  panel,
  /if \(activePicker\.value === 'order'\) \{[\s\S]*filteredActiveOrderOptions\.value\.map/,
  'Order cards must render the filtered unified ACTIVE order collection.'
)
assert.match(
  panel,
  /const handleActiveOrderSearchEnter = async \(\) => \{[\s\S]*exactMatch[\s\S]*filteredActiveOrderOptions\.value\.length === 1[\s\S]*handleSelectActiveOrder/,
  'Enter must select only an exact order-number match or the unique filtered result.'
)
assert.match(
  panel,
  /if \(picker === 'order'\) \{[\s\S]*activeOrderKeyword\.value = ''[\s\S]*activeOrderSearchInputRef\.value\?\.focus\(\)/,
  'Opening the order picker must reset and focus the order-number input.'
)

console.log('PASS: frontline PQC uses all production leaders active orders with order-number quick selection')
