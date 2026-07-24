const fs = require('fs')
const path = require('path')

const filePath = path.resolve(__dirname, '../../src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(filePath, 'utf8')

const forbiddenDisabledGuard = ':disabled="scope.row.status !== CommonStatusEnum.DISABLE"'
const forbiddenTooltipCopy = '仅停用状态，才可以操作'

if (source.includes(forbiddenDisabledGuard)) {
  throw new Error(`found forbidden route action disabled guard in ${filePath}`)
}

if (source.includes(forbiddenTooltipCopy)) {
  throw new Error(`found obsolete route action tooltip copy in ${filePath}`)
}

if (!source.includes("v-hasPermi=\"['mes:pro-route:update']\"")) {
  throw new Error('missing update permission gate on route edit action')
}

if (!source.includes("v-hasPermi=\"['mes:pro-route:delete']\"")) {
  throw new Error('missing delete permission gate on route delete action')
}

console.log('PASS: route edit/delete actions are no longer status-disabled')
