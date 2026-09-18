const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repo = path.resolve(__dirname, '..', '..', '..')
const servicePath = path.join(repo, 'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const controllerPath = path.join(repo, 'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java')

const service = fs.readFileSync(servicePath, 'utf8')
const controller = fs.readFileSync(controllerPath, 'utf8')

function methodBody(source, signature) {
  const start = source.indexOf(signature)
  assert.notEqual(start, -1, `Missing method ${signature}`)
  const bodyStart = source.indexOf('{', start)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1
    if (source[i] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart + 1, i)
    }
  }
  throw new Error(`Unterminated method ${signature}`)
}

const checkout = methodBody(service, 'private DccControlledFileRespVO doCheckoutControlledFile')
const checkin = methodBody(service, 'private DccControlledFileRespVO doCheckinControlledFile')
const mutateGuard = methodBody(service, 'private void assertCanMutateControlledFile')

assert.match(mutateGuard, /projectAccessService\.assertProjectEditorOrOwner\(userId,\s*file\.getDccProjectCodeId\(\)\)/,
  'mutation boundary must re-check formal project OWNER/EDIT access')
assert.match(mutateGuard, /permissionSupport\.hasCategoryPermission\(file\.getCategoryId\(\),\s*userId,\s*DccFileCategoryPermissionActionEnum\.UPLOAD\)/,
  'mutation boundary must re-check category UPLOAD permission')

const checkoutGuard = checkout.indexOf('assertCanMutateControlledFile(userId, file);')
assert.ok(checkoutGuard >= 0, 'checkout must call mutation guard')
assert.ok(checkoutGuard < checkout.indexOf('checkoutMapper.selectActiveByMasterId'),
  'checkout must check mutation permission before reading/creating active lock state')
assert.ok(checkoutGuard < checkout.indexOf('checkoutMapper.insert'),
  'checkout must check mutation permission before creating lock')
assert.ok(checkoutGuard < checkout.indexOf('checkoutByIdAndTenantWhenAvailable'),
  'checkout must check mutation permission before marking file checked out')

const checkinGuard = checkin.indexOf('assertCanMutateControlledFile(userId, file);')
assert.ok(checkinGuard >= 0, 'checkin must call mutation guard')
assert.ok(checkinGuard < checkin.indexOf('checkoutMapper.selectActiveByMasterId'),
  'checkin must check mutation permission before active checkout/replay branches')
assert.ok(checkinGuard < checkin.indexOf('uploadTicketService.resolveForBinding'),
  'checkin must check mutation permission before resolving upload ticket')
assert.ok(checkinGuard < checkin.indexOf('controlledFileMapper.insert'),
  'checkin must check mutation permission before inserting next iteration')

assert.match(controller, /@PreAuthorize\("@ss\.hasPermission\('dcc:controlled-file:query'\)"\)\s+public CommonResult<DccControlledFileRespVO> checkoutControlledFile/,
  'checkout controller may expose query-authenticated entry but service must own write authorization')
assert.match(controller, /@PreAuthorize\("@ss\.hasPermission\('dcc:controlled-file:query'\)"\)\s+public CommonResult<DccControlledFileRespVO> checkinControlledFile/,
  'checkin controller may expose query-authenticated entry but service must own write authorization')

console.log('DCC-STATIC-008 checkout/checkin permission contract PASS')
