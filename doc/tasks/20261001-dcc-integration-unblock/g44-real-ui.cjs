// Root-only persistent Playwright page operations; no API calls, injected router, mock or token bootstrap.
const fs = require('node:fs')
const path = require('node:path')
const readline = require('node:readline')
const { execFileSync } = require('node:child_process')
const { createRequire } = require('node:module')
const repo = 'C:/IntRuoyi/20261001-dcc-integration'
const load = createRequire(path.join(repo, 'IntRuoyiFronted/package.json'))
const { chromium } = load('playwright')
const output = 'C:/IntRuoyiBackups/20261003-dcc-integration/g44-real-ui-r4'
let browser, page, authenticated = false, step = 0, chain = Promise.resolve()
const observed = []
const pageErrors = []
function requireAllowed(value) {
  if (!value || typeof value !== 'object' || /password|secret|token|cookie|storageState/i.test(JSON.stringify(Object.keys(value)))) throw new Error('unsafe-command')
}
async function settle() {
  if (await page.locator('.el-message-box:visible').count()) return
  const masks = page.locator('.el-loading-mask:visible')
  while (await masks.count()) await masks.last().waitFor({ state: 'hidden', timeout: 45000 })
}
function locator(c) {
  let base = c.scope ? page.locator(c.scope) : page
  let selected = c.role ? base.getByRole(c.role, { name: c.name, exact: c.exact !== false })
    : c.placeholder ? base.getByPlaceholder(c.placeholder, { exact: c.exact !== false })
    : c.text ? base.getByText(c.text, { exact: c.exact !== false }) : base.locator(c.selector)
  if (Number.isInteger(c.nth)) selected = selected.nth(c.nth)
  return selected
}
async function snapshot() {
  if (!authenticated) throw new Error('login-required')
  await settle()
  const text = await page.locator('body').innerText()
  const controls = []
  const dialogsVisible = page.locator('.el-dialog:visible')
  const base = await dialogsVisible.count() ? dialogsVisible.last() : page
  const all = base.locator('input:visible,textarea:visible,button:visible,[role=combobox]:visible')
  for (let i = 0; i < Math.min(await all.count(), 80); i++) {
    const el = all.nth(i)
    const type = await el.getAttribute('type')
    controls.push({ tag: await el.getAttribute('role'), type, placeholder: await el.getAttribute('placeholder'), name: await el.getAttribute('aria-label'), text: type === 'password' ? '[redacted]' : (await el.innerText()).slice(0, 120) })
  }
  const capture = path.join(output, String(++step).padStart(3, '0') + '.png')
  await page.screenshot({ path: capture, fullPage: false, mask: [page.locator('input[type=password]')] })
  const dialogs = await page.locator('.el-dialog:visible,.el-message-box:visible').allInnerTexts()
  const options = await page.locator('.el-select-dropdown:visible,.el-tree-select__popper:visible').allInnerTexts()
  const result = { status: 'ACTUAL_VISIBLE_UI_SNAPSHOT', url: new URL(page.url()).pathname, text, dialogs, options, controls, screenshot: capture, naturalResponses: observed.splice(0), pageErrors: pageErrors.splice(0) }
  fs.writeFileSync(path.join(output, String(step).padStart(3, '0') + '.json'), JSON.stringify(result, null, 2), 'utf8')
  console.log(JSON.stringify({ ...result, text: dialogs.length || options.length ? undefined : text.slice(0, 7000), controls: controls.slice(-22), naturalResponses: result.naturalResponses.filter(x => x.pathname.includes('/dcc/')).slice(-15) }))
}
async function login() {
  const instructions = fs.readFileSync(path.join(repo, 'AGENTS.md'), 'utf8')
  const username = instructions.match(/^用户名\s+(\S+)\s*$/m)?.[1]
  const credential = instructions.match(/^密码\s+(\S+)\s*$/m)?.[1]
  if (!username || !credential) throw new Error('user-provided-identity-unavailable')
  await page.goto('http://127.0.0.1:' + global.frontendPort + '/login', { waitUntil: 'domcontentloaded' })
  const form = page.locator('.login-form:visible')
  await form.waitFor({ state: 'visible' })
  const tenant = form.locator('.el-select:visible')
  await tenant.click(); await tenant.locator('input').fill('芋道源码'); await tenant.locator('input').press('Enter')
  await form.getByPlaceholder(/用户名|账号/).fill(username)
  await form.locator('input[type=password]').fill(credential)
  const remember = form.getByRole('checkbox', { name: /记住/ })
  if (await remember.count()) await remember.uncheck()
  await form.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(u => !u.pathname.includes('/login'), { timeout: 45000 })
  authenticated = true
  console.log(JSON.stringify({ status: 'ACTUAL_USER_PROVIDED_FRONTEND_LOGIN_COMPLETE', noCredentialCapture: true }))
  await snapshot()
}
async function command(c) {
  requireAllowed(c)
  if (c.action === 'login') return login()
  if (c.action === 'snapshot') return snapshot()
  if (c.action === 'close') { await browser.close(); console.log('{"status":"ACTUAL_UI_SESSION_CLOSED"}'); process.exit(0) }
  if (!authenticated) throw new Error('login-required')
  const el = locator(c)
  if (c.action === 'clickPrompt') {
    if (typeof c.reason !== 'string' || !c.reason.startsWith('20261001-dcc-integration-g44')) throw new Error('task-reason-required')
    const handled = page.waitForEvent('dialog', { timeout: 15000 }).then(async dialog => {
      if (dialog.type() !== 'prompt' || !dialog.message().includes('意见')) { await dialog.dismiss(); throw new Error('unexpected-prompt') }
      await dialog.accept(c.reason)
    })
    await Promise.all([el.click({ timeout: 15000 }), handled])
  }
  else if (c.action === 'click') await el.click({ timeout: 15000 })
  else if (c.action === 'fill') { if (await el.getAttribute('type') === 'password') throw new Error('credential-command-forbidden'); await el.fill(c.value) }
  else if (c.action === 'press') await el.press(c.key)
  else if (c.action === 'check') await el.setChecked(c.checked)
  else if (c.action === 'select') await el.selectOption(c.value)
  else if (c.action === 'upload') await el.setInputFiles(c.file)
  else throw new Error('unsupported-action')
  await snapshot()
}
;(async () => {
  const common = execFileSync('git', ['rev-parse', '--git-common-dir'], { cwd: repo, encoding: 'utf8' }).trim()
  const registry = JSON.parse(fs.readFileSync(path.resolve(repo, common, 'intrruoyi-runtime/worktree-ports.json'), 'utf8').replace(/^\uFEFF/, ''))
  const rows = Array.isArray(registry) ? registry : registry.entries || registry.worktrees
  const matches = rows.filter(x => x.active === true && path.resolve(x.path).toLowerCase() === path.resolve(repo).toLowerCase())
  if (matches.length !== 1 || matches[0].profile !== 'int_qms' || matches[0].slot !== 6 || matches[0].frontendPort !== 8067 || matches[0].backendPort !== 48067) throw new Error('registered-runtime-mismatch')
  global.frontendPort = matches[0].frontendPort
  if (fs.existsSync(output)) throw new Error('existing-evidence-directory')
  fs.mkdirSync(output)
  browser = await chromium.launch({ headless: true })
  page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, timezoneId: 'Asia/Shanghai' })
  page.on('pageerror', error => { pageErrors.push({ type: error.name, safeMessage: /^(文档目录表单组件加载失败|.* is not (a function|iterable)|Cannot read properties of (undefined|null).*)$/.test(error.message) ? error.message.slice(0, 300) : 'frontend-exception-message-withheld' }) })
  page.on('response', response => { const u = new URL(response.url()); if (u.pathname.startsWith('/admin-api/')) observed.push({ method: response.request().method(), pathname: u.pathname, status: response.status() }) })
  console.log('{"status":"REGISTERED_REAL_UI_SESSION_READY"}')
  readline.createInterface({ input: process.stdin }).on('line', line => {
    chain = chain.then(() => command(JSON.parse(line))).catch(error => console.log(JSON.stringify({ status: 'ACTUAL_UI_COMMAND_FAILED', errorType: error.name, safeMessage: ['unsafe-command', 'login-required', 'credential-command-forbidden', 'unsupported-action'].includes(error.message) ? error.message : 'inspect-observed-visible-state' })))
  })
})().catch(async error => { console.log(JSON.stringify({ status: 'ACTUAL_UI_START_FAILED', errorType: error.name })); if (browser) await browser.close(); process.exit(1) })
