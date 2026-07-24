const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(root, '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readRepo = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeApi = read('src/api/mes/pro/route/index.ts')
const validationRespVO = readRepo(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/flow/MesProRouteProcessFlowValidationRespVO.java'
)
const flowService = readRepo(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProcessFlowServiceImpl.java'
)

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const extractConstFunction = (content, functionName) => {
  const marker = `const ${functionName} =`
  const start = content.indexOf(marker)
  if (start === -1) {
    throw new Error(`${functionName} function missing`)
  }
  const braceStart = content.indexOf('{', start)
  if (braceStart === -1) {
    throw new Error(`${functionName} function body missing`)
  }
  let depth = 0
  for (let index = braceStart; index < content.length; index += 1) {
    const char = content[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return content.slice(start, index + 1)
    }
  }
  throw new Error(`${functionName} function body is not closed`)
}

const triggerFlowAutoLayoutHandler = extractConstFunction(routeFormContent, 'triggerFlowAutoLayout')
const runPendingFlowAutoLayoutHandler = extractConstFunction(
  routeFormContent,
  'runPendingFlowAutoLayout'
)
const handleRouteTabChangeHandler = extractConstFunction(routeFormContent, 'handleRouteTabChange')
const saveFromParentHandler = extractConstFunction(graphComponent, 'saveFromParent')

assertIncludes(
  routeFormContent,
  "const pendingFlowAutoLayoutKey = ref('')",
  'route form must track the pending entry auto-layout context'
)
assertIncludes(
  routeFormContent,
  "const completedFlowAutoLayoutEntryKey = ref('')",
  'route form must remember the context that already consumed entry auto-layout'
)
assertIncludes(
  routeFormContent,
  'const buildFlowAutoLayoutEntryKey = () =>',
  'route form must key entry auto-layout by route and candidate context'
)
assertIncludes(
  routeFormContent,
  'const shouldQueueFlowAutoLayout = () =>',
  'route form must guard repeated flow tab entry auto-layout'
)
assertIncludes(
  triggerFlowAutoLayoutHandler,
  'if (!shouldQueueFlowAutoLayout()) return',
  'flow auto-layout trigger must be gated before queuing'
)
assertIncludes(
  triggerFlowAutoLayoutHandler,
  'pendingFlowAutoLayoutKey.value = buildFlowAutoLayoutEntryKey()',
  'queued auto-layout must remember the context it belongs to'
)
assertIncludes(
  runPendingFlowAutoLayoutHandler,
  'const pendingKey = pendingFlowAutoLayoutKey.value',
  'pending runner must consume the original queued context'
)
assertIncludes(
  runPendingFlowAutoLayoutHandler,
  'completedFlowAutoLayoutEntryKey.value = pendingKey',
  'pending runner must mark entry auto-layout complete after it actually runs'
)
assertIncludes(
  handleRouteTabChangeHandler,
  'triggerFlowAutoLayout()',
  'flow tab change may request entry auto-layout through the guarded trigger'
)

assertIncludes(
  routeApi,
  'routeProcessIdMap?: Record<string, number>',
  'flow validation response must expose persisted route process id remap'
)
assertIncludes(
  validationRespVO,
  'private Map<Long, Long> routeProcessIdMap = new HashMap<>();',
  'backend save response must return persisted draft route process id map'
)
assertIncludes(
  flowService,
  'validation.setRouteProcessIdMap(persistedRouteProcessIdMap);',
  'backend save must attach id map to active route flow save response'
)
assertIncludes(
  graphComponent,
  'const applyPersistedRouteProcessIdMap = (routeProcessIdMap?: Record<string, number>) =>',
  'graph must remap draft route process ids locally after save'
)
assertIncludes(
  graphComponent,
  'const markGraphSaveClean = () =>',
  'graph must clear local dirty state after successful save without reloading'
)
assertIncludes(
  saveFromParentHandler,
  'applyPersistedRouteProcessIdMap(result.routeProcessIdMap)',
  'save must apply persisted ids before saving selected process attributes'
)
assertIncludes(
  saveFromParentHandler,
  'markGraphSaveClean()',
  'save must clear local graph draft state after all save steps pass'
)
assertNotIncludes(
  saveFromParentHandler,
  'await loadGraph()',
  'save must not reload the whole graph after successful save'
)

console.log('mes-route-flow-entry-only-refresh-static PASS')
