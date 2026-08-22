const fs = require('fs')
const path = require('path')

const apiPath = path.join(__dirname, 'teamLeader.ts')
const pagePath = path.join(__dirname, '../../../../views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

function assertIncludes(source, needle, message) {
  if (!source.includes(needle)) throw new Error(message)
}

assertIncludes(api, 'pickListId: string', 'active-order add request must carry string pickListId')
assertIncludes(api, 'pickListBindingId: string', 'active-order response must expose string pickListBindingId')
assertIncludes(api, 'getTeamLeaderActiveOrderPickListOptions', 'pick-list candidate API is missing')
assertIncludes(page, 'activeOrderForm.pickListId', 'active-order dialog must bind pickListId')
assertIncludes(page, 'getTeamLeaderActiveOrderPickListOptions', 'page must load pick-list candidates')
assertIncludes(page, 'pickListId,', 'submit must send pickListId')

console.log('teamLeaderPickListBinding.static.spec PASS')
