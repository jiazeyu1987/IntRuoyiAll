const fs = require('fs')
const path = require('path')
const Module = require('module')

const workspaceRoot = process.env.INT_RUOYI_ROOT || 'E:\\IntRuoyi'
const taskDir = path.join(workspaceRoot, 'doc', 'tasks', '20260802-dcc-traceability-ux-fixes')
const sourceScriptPath = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260802-dcc-original-release-e2e-current',
  'dcc-original-release-e2e-current.cjs'
)
const runId = process.env.DCC_E2E_RUN_ID || new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)
const resultPath =
  process.env.DCC_E2E_RESULT_PATH || path.join(taskDir, `dcc-original-release-wrong-password-${runId}.json`)

process.env.DCC_E2E_RUN_ID = runId
process.env.DCC_E2E_RESULT_PATH = resultPath
process.env.DCC_E2E_FILE_NUMBER = process.env.DCC_E2E_FILE_NUMBER || `CODX-DCC-TRACE-DIAG-${runId}`
process.env.DCC_E2E_FILE_NAME = process.env.DCC_E2E_FILE_NAME || `Codex DCC 签核追溯诊断 ${runId}`

const wrongPasswordPatch = `
  if (process.env.DCC_E2E_VERIFY_WRONG_PASSWORD !== 'false' && !globalThis.__dccWrongPasswordDiagnosticDone) {
    globalThis.__dccWrongPasswordDiagnosticDone = true
    const diagnosticPassword = String(password || '') + '-diagnostic'
    await dialog.locator('input[type="password"]:visible').fill(diagnosticPassword)
    const wrongPasswordResponse = await waitApi(
      page,
      \`wrong password diagnostic \${versionNo} \${approver.username}\`,
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/') &&
        response.url().includes('/approve-task') &&
        response.request().method() === 'POST',
      async () => dialog.getByRole('button', { name: /确认签名/ }).click(),
      90000,
      { skipCodeCheck: true }
    )
    if (!wrongPasswordResponse || wrongPasswordResponse.code === 0) {
      throw new Error(\`Wrong password diagnostic unexpectedly succeeded for \${approver.username}\`)
    }
    await page
      .waitForFunction(
        () =>
          document.body.innerText.includes('签名失败原因') &&
          document.body.innerText.includes('当前密码错误') &&
          document.body.innerText.includes('处理建议') &&
          document.body.innerText.includes('责任入口'),
        undefined,
        { timeout: 30000 }
      )
      .catch(() => undefined)
    const diagnosticText = await dialog.innerText({ timeout: 10000 }).catch(() => '')
    const uiTokensVisible = ['签名失败原因', '当前密码错误', '处理建议', '责任入口'].every((token) =>
      diagnosticText.includes(token)
    )
    if (!uiTokensVisible) {
      throw new Error(\`Wrong password diagnostic copy not visible for \${approver.username}: \${diagnosticText.slice(0, 300)}\`)
    }
    evidence.phases.push({
      phase: 'wrong-password-diagnostic',
      username: approver.username,
      role: approver.role,
      userId: approver.userId,
      responseCode: wrongPasswordResponse.code,
      responseMessage: wrongPasswordResponse.msg || wrongPasswordResponse.message || '',
      uiTokensVisible,
      result: 'PASS'
    })
  }
`

const compileOriginalScript = () => {
  const original = fs.readFileSync(sourceScriptPath, 'utf8')
  const approveNeedle = /  await dialog\.locator\('input\[type="password"\]:visible'\)\.fill\(password\)\r?\n  await waitApi\(/
  if (!approveNeedle.test(original)) {
    throw new Error('Original DCC release script approval password anchor not found')
  }
  const withDiagnostic = original.replace(approveNeedle, () =>
    `${wrongPasswordPatch}\n  await dialog.locator('input[type="password"]:visible').fill(password)\n  await waitApi(`
  )
  const mainNeedle =
    /main\(\)\.catch\(\(error\) => \{\r?\n  console\.error\(`\[dcc-original-release-e2e-current\] \$\{error && error\.message \? error\.message : String\(error\)\}`\)\r?\n  process\.exit\(1\)\r?\n\}\)\r?\n?$/
  if (!mainNeedle.test(withDiagnostic)) {
    throw new Error('Original DCC release script main invocation anchor not found')
  }
  const moduleSource = withDiagnostic.replace(mainNeedle, 'module.exports = { main }\n')
  const compiledModule = new Module(sourceScriptPath, module)
  compiledModule.filename = sourceScriptPath
  compiledModule.paths = Module._nodeModulePaths(path.dirname(sourceScriptPath))
  compiledModule._compile(moduleSource, sourceScriptPath)
  return compiledModule.exports.main
}

const main = async () => {
  if (!process.env.DCC_E2E_PASSWORD) {
    throw new Error('Missing DCC_E2E_PASSWORD environment variable')
  }
  const originalMain = compileOriginalScript()
  await originalMain()
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'))
  const diagnostic = (result.phases || []).find((phase) => phase.phase === 'wrong-password-diagnostic')
  if (!diagnostic || diagnostic.result !== 'PASS') {
    throw new Error('Wrong password diagnostic phase missing from DCC original release result')
  }
  if (result.status !== 'PASS') {
    throw new Error(`DCC original release result did not pass: ${result.status}`)
  }
  console.log(
    JSON.stringify(
      {
        status: 'PASS',
        resultPath,
        fileNumber: result.fileNumber,
        controlledFileId: result.v1ControlledFileId,
        wrongPasswordDiagnostic: {
          username: diagnostic.username,
          role: diagnostic.role,
          responseCode: diagnostic.responseCode,
          uiTokensVisible: diagnostic.uiTokensVisible
        }
      },
      null,
      2
    )
  )
}

main().catch((error) => {
  console.error(`[dcc-original-release-with-wrong-password-e2e] ${error && error.message ? error.message : String(error)}`)
  process.exit(1)
})
