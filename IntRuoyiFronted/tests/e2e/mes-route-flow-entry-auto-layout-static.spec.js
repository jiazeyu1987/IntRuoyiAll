const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeFormShell = read('src/views/mes/pro/route/RouteForm.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertMatch = (content, pattern, label) => {
  if (!pattern.test(content)) {
    throw new Error(`${label} missing`)
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

const applyAutoLayoutHandler = extractConstFunction(graphComponent, 'applyAutoLayout')
const handleAutoLayoutHandler = extractConstFunction(graphComponent, 'handleAutoLayout')
const runPendingEntryAutoLayoutHandler = extractConstFunction(
  graphComponent,
  'runPendingEntryAutoLayout'
)
const completeGraphLoadViewportHandler = extractConstFunction(
  graphComponent,
  'completeGraphLoadViewport'
)

assertIncludes(
  routeFormContent,
  '@tab-change="handleRouteTabChange"',
  'route tabs must react to tab entry'
)
assertIncludes(
  routeFormContent,
  'ref="routeFlowGraphDesignerRef"',
  'route form must hold graph designer ref'
)
assertIncludes(
  routeFormContent,
  'const pendingFlowAutoLayout = ref(false)',
  'route form must keep pending graph auto layout request'
)
assertIncludes(
  routeFormContent,
  'pendingFlowAutoLayout.value = true',
  'route form must queue graph auto layout request'
)
assertNotIncludes(
  routeFormContent,
  'routeFlowGraphDesignerRef.value?.autoLayoutOnEntry()',
  'route form must not silently drop auto layout when graph ref is not mounted'
)
assertIncludes(
  routeEditPage,
  '<RouteFormContent',
  'route edit page must enter through shared route form content'
)
assertIncludes(
  routeEditPage,
  "return 'flow'",
  'route edit page must default existing route entry to flow tab'
)
assertIncludes(
  routeEditPage,
  "await content.open('update', routeId.value, initialTab.value)",
  'route edit page must pass resolved initial tab into shared form content'
)
assertIncludes(
  routeFormShell,
  '<RouteFormContent',
  'dialog route form must use shared form content'
)
assertIncludes(
  routeFormShell,
  'ref="contentRef"',
  'dialog route form must keep shared content ref'
)
assertMatch(
  routeFormContent,
  /let shouldTriggerFlowAutoLayout = false[\s\S]*shouldTriggerFlowAutoLayout = activeTab\.value === 'flow'[\s\S]*finally \{[\s\S]*formLoading\.value = false[\s\S]*\}[\s\S]*if \(shouldTriggerFlowAutoLayout\) \{[\s\S]*triggerFlowAutoLayout\(\)/,
  'existing route open must trigger flow auto layout after form loading clears'
)
assertMatch(
  routeFormContent,
  /const handleRouteTabChange = \(tabName: string \| number\) => \{[\s\S]*if \(tabName === 'flow'\)[\s\S]*triggerFlowAutoLayout/,
  'flow tab change handler must only trigger auto layout for flow tab'
)
assertMatch(
  routeFormContent,
  /const runPendingFlowAutoLayout = async \(\) => \{[\s\S]*if \(!pendingFlowAutoLayout\.value\) return[\s\S]*await nextTick\(\)[\s\S]*if \(activeTab\.value !== 'flow'\) \{[\s\S]*pendingFlowAutoLayout\.value = false[\s\S]*return[\s\S]*if \(formLoading\.value \|\| !formData\.value\.id\) return[\s\S]*const designer = routeFlowGraphDesignerRef\.value[\s\S]*if \(!designer\) return[\s\S]*pendingFlowAutoLayout\.value = false[\s\S]*await designer\.autoLayoutOnEntry\(\)/,
  'flow auto layout trigger must keep request pending until graph ref is stable'
)
assertMatch(
  routeFormContent,
  /watch\([\s\S]*routeFlowGraphDesignerRef[\s\S]*runPendingFlowAutoLayout\(\)[\s\S]*flush: 'post'/,
  'route form must retry pending auto layout when graph ref mounts'
)
assertMatch(
  routeFormContent,
  /shouldTriggerFlowAutoLayout = activeTab\.value === 'flow'[\s\S]*if \(shouldTriggerFlowAutoLayout\) \{[\s\S]*triggerFlowAutoLayout/,
  'default flow entry must trigger auto layout after opening an existing route'
)

assertIncludes(
  graphComponent,
  'const autoLayoutEntryPending = ref(false)',
  'graph must track pending entry auto layout'
)
assertIncludes(
  graphComponent,
  'const autoLayoutOnEntry = async () =>',
  'graph must expose entry auto layout method'
)
assertIncludes(
  graphComponent,
  'runPendingEntryAutoLayout()',
  'graph must run pending entry auto layout after graph load'
)
assertIncludes(
  graphComponent,
  'const fitGraphAfterLayout = async (focusRouteProcessId?: number) =>',
  'graph must wait for rendered layout before fitting viewport'
)
assertIncludes(
  applyAutoLayoutHandler,
  'await fitGraphAfterLayout(options.focusRouteProcessId)',
  'shared auto layout must await viewport fitting after nodes render'
)
assertIncludes(
  handleAutoLayoutHandler,
  'await applyAutoLayout()',
  'manual auto layout must use the shared layout path'
)
assertIncludes(
  runPendingEntryAutoLayoutHandler,
  'await handleAutoLayout()',
  'pending entry auto layout must await the same layout path as manual click'
)
assertIncludes(
  completeGraphLoadViewportHandler,
  'const didRunAutoLayout = await runPendingEntryAutoLayout()',
  'graph load viewport finalizer must await pending entry auto layout result'
)
assertNotIncludes(
  graphComponent,
  'void nextTick(handleFitScreen)',
  'entry auto layout must not schedule a one-tick fit that can run before Vue Flow finishes rendering'
)
assertMatch(
  graphComponent,
  /const runPendingEntryAutoLayout = async \(\) => \{[\s\S]*if \(!autoLayoutEntryPending\.value \|\| loading\.value \|\| routeNodes\.value\.length === 0\) return false[\s\S]*await handleAutoLayout\(\)[\s\S]*return true/,
  'pending entry auto layout must reuse handleAutoLayout once nodes are ready'
)
assertMatch(
  graphComponent,
  /defineExpose\(\{[\s\S]*autoLayoutOnEntry[\s\S]*validateBeforeSubmit[\s\S]*saveFromParent[\s\S]*\}\)/,
  'graph must expose autoLayoutOnEntry together with submit hooks'
)

console.log('mes-route-flow-entry-auto-layout-static PASS')
