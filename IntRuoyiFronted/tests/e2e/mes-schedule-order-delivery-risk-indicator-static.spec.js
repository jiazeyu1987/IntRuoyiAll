const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const dayjs = require('dayjs')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const compileRiskFunction = (functionName, nextFunctionName) => {
  const startToken = `const ${functionName} =`
  const endToken = `\nconst ${nextFunctionName} =`
  const start = pageSource.indexOf(startToken)
  const end = pageSource.indexOf(endToken, start)
  assert(start >= 0 && end > start, `Unable to extract ${functionName} from schedule-order page.`)

  const source = pageSource
    .slice(start, end)
    .replace(
      `const ${functionName} = (row: MesProScheduleOrderVO) =>`,
      `const ${functionName} = (row) =>`
    )
  return new Function('dayjs', `${source}; return ${functionName}`)(dayjs)
}

for (const token of [
  'getStartRiskText',
  'getDeliveryRiskText',
  '晚于最晚开工',
  '逾承诺交期',
  'schedule-order-pool__risk-indicator',
  'ep:warning-filled'
]) {
  assert(pageSource.includes(token), `Schedule-order risk indicator must contain ${token}.`)
}

assert.match(
  pageSource,
  /plannedStartTime[\s\S]*?schedule-order-pool__risk-cell[\s\S]*?getStartRiskText\(row\)[\s\S]*?schedule-order-pool__risk-indicator[\s\S]*?晚于最晚开工/,
  'Planned-start risk must be rendered as visible text, not color only.'
)
assert.match(
  pageSource,
  /plannedEndTime[\s\S]*?schedule-order-pool__risk-cell[\s\S]*?getDeliveryRiskText\(row\)[\s\S]*?schedule-order-pool__risk-indicator[\s\S]*?逾承诺交期/,
  'Promise-date risk must be rendered as visible text, not color only.'
)

assert(
  pageSource.includes("dayjs(row.promiseDate).startOf('day')") &&
    pageSource.includes("dayjs(row.plannedEndTime).startOf('day')"),
  'Delivery lateness must use calendar-day arithmetic for the date-only promise field.'
)
assert(
  pageSource.includes(':aria-label="getStartRiskText(row)"') &&
    pageSource.includes(':aria-label="getDeliveryRiskText(row)"'),
  'Both risk indicators must expose their exact visible reason to assistive technology.'
)

const getStartRiskText = compileRiskFunction('getStartRiskText', 'isStartRisk')
const getDeliveryRiskText = compileRiskFunction('getDeliveryRiskText', 'isDeliveryRisk')

assert.equal(
  getStartRiskText({
    latestStartTime: '2026-07-14 08:00:00',
    plannedStartTime: '2026-07-14 08:30:00'
  }),
  '晚于最晚开工 30 分钟'
)
assert.equal(
  getStartRiskText({
    latestStartTime: '2026-07-14 08:00:00',
    plannedStartTime: '2026-07-14 09:00:00'
  }),
  '晚于最晚开工 1 小时'
)
assert.equal(
  getStartRiskText({
    latestStartTime: '2026-07-14 08:00:00',
    plannedStartTime: '2026-07-15 08:00:00'
  }),
  '晚于最晚开工 1 天'
)
assert.equal(
  getStartRiskText({
    latestStartTime: '2026-07-14 08:00:00',
    plannedStartTime: '2026-07-14 08:00:00'
  }),
  '',
  'An on-time planned start must not be marked as risky.'
)

assert.equal(
  getDeliveryRiskText({ promiseDate: '2026-07-14', plannedEndTime: '2026-07-14 23:59:59' }),
  '',
  'The promise date is a calendar-day deadline, so the same date must remain on time.'
)
assert.equal(
  getDeliveryRiskText({ promiseDate: '2026-07-14', plannedEndTime: '2026-10-28 08:00:00' }),
  '逾承诺交期 106 天'
)
assert.equal(
  getDeliveryRiskText({ promiseDate: '2026-07-14', plannedEndTime: '2026-12-14 08:00:00' }),
  '逾承诺交期 153 天'
)
assert.equal(
  getDeliveryRiskText({ promiseDate: '', plannedEndTime: '2026-10-28 08:00:00' }),
  '',
  'Missing promise data must not be presented as a calculated lateness value.'
)

console.log('PASS: schedule-order delivery-risk indicator static contract')
