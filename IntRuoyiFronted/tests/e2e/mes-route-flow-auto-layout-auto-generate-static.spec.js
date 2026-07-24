const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

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

const loadGraphHandler = extractConstFunction(component, 'loadGraph')
const completeGraphLoadViewportHandler = extractConstFunction(component, 'completeGraphLoadViewport')
const autoLayoutOnEntryHandler = extractConstFunction(component, 'autoLayoutOnEntry')
const generateLinearDraftHandler = extractConstFunction(component, 'handleGenerateLinearDraft')

assertIncludes(formContent, 'triggerFlowAutoLayout()', 'route form must request graph auto-layout on flow tab entry')
assertIncludes(component, 'completeGraphLoadViewport', 'graph load must finalize viewport after loading')
assertIncludes(loadGraphHandler, 'shouldAdjustViewport = routeNodes.value.length > 0', 'loadGraph must remember successful graph load viewport work')
assertIncludes(loadGraphHandler, 'loading.value = false', 'loadGraph must clear loading before pending auto-layout')
assertIncludes(loadGraphHandler, 'await completeGraphLoadViewport()', 'loadGraph must run pending auto-layout after loading completes')
assertIncludes(completeGraphLoadViewportHandler, 'const didRunAutoLayout = await runPendingEntryAutoLayout()', 'viewport finalize must await pending entry auto-layout')
assertIncludes(completeGraphLoadViewportHandler, 'if (!didRunAutoLayout)', 'viewport finalize must avoid duplicate fit after auto-layout')
assertIncludes(autoLayoutOnEntryHandler, 'autoLayoutEntryPending.value = true', 'entering flow tab must mark pending auto-layout')
assertIncludes(autoLayoutOnEntryHandler, 'await runPendingEntryAutoLayout()', 'entering flow tab must try immediate auto-layout when graph is ready')
assertNotIncludes(component, 'data-flow-action="add-edge-dialog"', 'toolbar add edge dialog button must be removed')
assertNotIncludes(component, '添加连接线', 'toolbar add edge copy must be removed')
assertNotIncludes(component, 'edgeDialogVisible', 'unused add-edge dialog state must be removed')
assertNotIncludes(component, 'handleOpenEdgeDialog', 'unused add-edge dialog opener must be removed')
assertNotIncludes(component, 'handleEdgeAdd', 'unused add-edge dialog submit handler must be removed')
assertNotIncludes(component, '根据序号生成线性关系', 'old linear relation button copy must be removed')
  assertIncludes(component, '自动生成', 'linear relation draft button must be renamed to 自动生成')
assertIncludes(generateLinearDraftHandler, '线性关系已生成草稿，请点击顶部保存后生效。', 'auto generate must keep draft-save behavior')
assertIncludes(component, '@connect="handleConnect"', 'canvas drag connection must remain available')
assertIncludes(component, 'addEdge(Number(connection.source), Number(connection.target))', 'canvas connect handler must still add edges')

console.log('mes-route-flow-auto-layout-auto-generate-static PASS')
