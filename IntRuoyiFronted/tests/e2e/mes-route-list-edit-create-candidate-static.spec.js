const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const getFunctionBody = (functionName) => {
  const marker = `const ${functionName} = async`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `必须定义 ${functionName}。`)
  const nextFunction = source.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextFunction, -1, `必须能截取 ${functionName} 函数体。`)
  return source.slice(start, nextFunction)
}

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?:table-key="ROUTE_LIST_TABLE_KEY"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '工艺路线列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const actionsLabelIndex = template.indexOf('label="操作"')
assert.notEqual(actionsLabelIndex, -1, '必须保留操作列。')
const actionsColumnStart = template.lastIndexOf('<el-table-column', actionsLabelIndex)
const actionsColumnEnd = template.indexOf('</el-table-column>', actionsLabelIndex)
const actionsColumn = template.slice(actionsColumnStart, actionsColumnEnd + '</el-table-column>'.length)

assert.match(
  actionsColumn,
  /@click="handleEditRouteProductionConfig\(scope\.row\)"[\s\S]*>\s*编辑\s*<\/el-button>/,
  '列表“编辑”按钮必须绑定统一候选版本入口。'
)
assert.match(
  actionsColumn,
  /@click="openRouteVersionWorkspace\(scope\.row\)"[\s\S]*>\s*版本\s*<\/el-button>/,
  '列表“版本”按钮必须继续绑定版本工作区。'
)

const editHandler = getFunctionBody('handleEditRouteProductionConfig')
assert.match(
  editHandler,
  /ensureSameSourceDraftCandidateForProductionConfig\(\{[\s\S]*routeId:\s*row\.id[\s\S]*actionName:\s*'进入候选版本编辑'[\s\S]*changeReason:\s*'列表编辑创建候选版本'/,
  '点击“编辑”必须通过统一候选入口复用同源 DRAFT，不能散落直建候选。'
)
assert.match(
  editHandler,
  /const\s+candidateResult\s*=\s*await\s+ensureSameSourceDraftCandidateForProductionConfig\(/,
  '列表“编辑”必须保留候选入口返回的创建来源，不能只接收候选版本本体。'
)
assert.match(
  editHandler,
  /const\s+draftExitQuery[\s\S]*candidateResult\.created[\s\S]*routeDraftOrigin:\s*'list-edit'[\s\S]*discardOnUnsavedExit:\s*'1'[\s\S]*:\s*\{\}/,
  '只有本次列表编辑新建的候选版本，才允许携带未保存退出丢弃标记。'
)
assert.match(
  editHandler,
  /query:\s*buildRouteCandidateEditQuery\(candidateResult\.candidate,\s*\{[\s\S]*tab:\s*'flow'[\s\S]*\.\.\.draftExitQuery/,
  '进入编辑页必须携带候选版本上下文；既有 DRAFT 草稿不得被标记为本次直建草稿。'
)
assert.doesNotMatch(
  editHandler,
  /ProRouteApi\.createRouteCandidateVersion\(\{/,
  '列表“编辑”不得直接调用 createRouteCandidateVersion，必须集中到 routeCandidateEntry.ts。'
)
assert.match(
  editHandler,
  /isRouteMultipleDraftCandidateError\([\s\S]*openRouteVersionWorkspace\(row/,
  '历史多个草稿冲突必须打开版本工作区处理，不能自动猜测使用哪个草稿。'
)

assert.doesNotMatch(
  editHandler,
  /pendingRouteVersionStatus\s*===\s*'PENDING_APPROVAL'[\s\S]*ProRouteApi\.createRouteCandidateVersion|pendingRouteVersionStatus\s*===\s*'READY_TO_PUBLISH'[\s\S]*ProRouteApi\.createRouteCandidateVersion/,
  'PENDING_APPROVAL / READY_TO_PUBLISH 待处理候选不得在列表编辑中创建替代草稿。'
)

const candidateEntry = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/routeCandidateEntry.ts'),
  'utf8'
)
assert.match(
  candidateEntry,
  /export\s+type\s+EnsureSameSourceDraftCandidateResult[\s\S]*candidate:\s*ProRouteVersionVO[\s\S]*created:\s*boolean/,
  '统一候选入口必须返回 candidate + created，供列表退出保护区分本次新建和既有 DRAFT。'
)
assert.match(
  candidateEntry,
  /return\s+\{\s*candidate:\s*requireDraftCandidateVersion\(existingDraftCandidate,\s*actionName\),\s*created:\s*false\s*\}/,
  '复用既有 DRAFT 时必须返回 created=false，避免“不保存草稿”取消历史候选版本。'
)
assert.match(
  candidateEntry,
  /return\s+\{\s*candidate:\s*draftCandidate,\s*created:\s*true\s*\}/,
  '本次调用真正新建 DRAFT 时必须返回 created=true，保留直建草稿退出可取消语义。'
)
assert.match(
  candidateEntry,
  /OPEN_ROUTE_VERSION_STATUSES\s*=\s*\[[\s\S]*DRAFT_ROUTE_VERSION_STATUS[\s\S]*PENDING_APPROVAL_ROUTE_VERSION_STATUS[\s\S]*READY_TO_PUBLISH_ROUTE_VERSION_STATUS[\s\S]*\]/,
  '统一候选入口必须只把打开中的 DRAFT/PENDING_APPROVAL/READY_TO_PUBLISH 视为可阻塞候选。'
)
assert.doesNotMatch(
  candidateEntry,
  /OPEN_ROUTE_VERSION_STATUSES[\s\S]*CANCELLED|CANCELLED[\s\S]*OPEN_ROUTE_VERSION_STATUSES/,
  '删除草稿后的 CANCELLED 版本不得被复用或阻塞再次点击“编辑”创建新草稿。'
)
assert.match(
  candidateEntry,
  /ProRouteApi\.createRouteCandidateVersion\(\{[\s\S]*sourceRouteVersionId:\s*routeInfo\.activeRouteVersionId/,
  '删除草稿后再次点击“编辑”创建的新草稿必须基于当前 ACTIVE 版本。'
)

const versionWorkspace = getFunctionBody('openRouteVersionWorkspace')
assert.match(
  versionWorkspace,
  /ProRouteApi\.getRouteVersionList|loadRouteVersions/,
  '版本工作区必须继续负责加载候选版本列表。'
)

console.log('PASS: mes route list edit uses single open candidate entry')
