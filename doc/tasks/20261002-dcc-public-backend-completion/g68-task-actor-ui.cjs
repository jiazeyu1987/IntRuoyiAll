// Bounded real Playwright UI only. Credentials stay in memory; no APIRequest, mock, router/state injection or auth capture.
const fs = require('node:fs')
const path = require('node:path')
const readline = require('node:readline')
const { createRequire } = require('node:module')
const repo = 'C:/IntRuoyiAll-int_main'
const load = createRequire(path.join(repo, 'IntRuoyiFronted/package.json'))
const { chromium } = load('playwright')
const output = 'C:/IntRuoyiBackups/20261006-dcc-task-actor-config/g68-ui-r1'
let browser, page, authenticated = false, step = 0, chain = Promise.resolve()
const observed = []
function safeCommand(c) {
  if (!c || typeof c !== 'object' || /password|secret|token|cookie|storageState/i.test(JSON.stringify(Object.keys(c)))) throw new Error('UNSAFE_COMMAND')
}
function element(c) {
  const base = c.scope ? page.locator(c.scope) : page
  let el = c.role ? base.getByRole(c.role, { name: c.name, exact: c.exact !== false })
    : c.text ? base.getByText(c.text, { exact: c.exact !== false })
      : c.placeholder ? base.getByPlaceholder(c.placeholder, { exact: c.exact !== false }) : base.locator(c.selector)
  if (Number.isInteger(c.nth)) el = el.nth(c.nth)
  return el
}
async function settle() {
  const masks = page.locator('.el-loading-mask:visible')
  for (let i = 0; i < 8 && await masks.count(); i++) await masks.last().waitFor({ state: 'hidden', timeout: 30000 })
}
async function snapshot(c = {}) {
  if (!authenticated || new URL(page.url()).pathname.includes('/login')) throw new Error('LOGIN_REQUIRED')
  await settle()
  const dialogs = page.locator('.el-dialog:visible,.el-message-box:visible')
  const base = await dialogs.count() ? dialogs.last() : page
  const inputs = []
  const fields = base.locator('input:visible,textarea:visible')
  for (let i = 0; i < Math.min(await fields.count(), 80); i++) {
    const el = fields.nth(i), type = await el.getAttribute('type')
    if (type === 'password') continue
    inputs.push({ index: i, type, placeholder: await el.getAttribute('placeholder'), value: await el.inputValue() })
  }
  const rows = await base.locator('.el-table__body tr:visible').allInnerTexts()
  const links = await page.locator('a:visible').evaluateAll(es => es.map(e => ({ text: e.innerText, path: e.getAttribute('href') })).filter(e => e.path && !/token|secret|password|cookie/i.test(e.path)))
  const result = { step: ++step, url: new URL(page.url()).pathname, text: (await page.locator('body').innerText()).slice(0, 15000), dialogs: await dialogs.allInnerTexts(), options: await page.locator('.el-select-dropdown:visible,.el-tree-select__popper:visible').allInnerTexts(), inputs, rows, links, naturalResponses: observed.splice(0) }
  if (c.capture) {
    if (!/^[a-z0-9-]+$/.test(c.capture)) throw new Error('BAD_CAPTURE_NAME')
    result.screenshot = path.join(output, c.capture + '.png')
    if (fs.existsSync(result.screenshot)) throw new Error('CAPTURE_EXISTS')
    await page.screenshot({ path: result.screenshot, fullPage: false, mask: [page.locator('input[type=password]')] })
  }
  fs.writeFileSync(path.join(output, String(step).padStart(3, '0') + '.json'), JSON.stringify(result, null, 2), { encoding: 'utf8', flag: 'wx' })
  console.log(JSON.stringify(result))
}
async function login() {
  const instructions = fs.readFileSync(path.join(repo, 'AGENTS.md'), 'utf8')
  const username = instructions.match(/^用户名\s+(\S+)\s*$/m)?.[1]
  let credential = instructions.match(/^密码\s+(\S+)\s*$/m)?.[1]
  if (!username || !credential) throw new Error('IDENTITY_UNAVAILABLE')
  await page.goto('http://127.0.0.1:8061/login', { waitUntil: 'domcontentloaded' })
  const form = page.locator('.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenant = form.locator('.el-select:visible')
  await tenant.click(); await tenant.locator('input').fill('芋道源码'); await tenant.locator('input').press('Enter')
  await form.getByPlaceholder(/用户名|账号/).fill(username)
  await form.locator('input[type=password]').fill(credential)
  const remember = form.getByRole('checkbox', { name: /记住/ })
  if (await remember.count()) await remember.uncheck()
  await form.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(u => !u.pathname.includes('/login'), { timeout: 45000 })
  credential = undefined
  authenticated = true
  console.log(JSON.stringify({ status: 'REAL_FRONTEND_LOGIN_COMPLETE', tenant: '芋道源码', user: username, noCredentialCapture: true }))
  await snapshot()
}
async function command(c) {
  safeCommand(c)
  if (c.action === 'login') return login()
  if (c.action === 'close') { await browser.close(); console.log('{"status":"OWN_UI_SESSION_CLOSED"}'); process.exit(0) }
  if (!authenticated) throw new Error('LOGIN_REQUIRED')
  if (c.action === 'goto') {
    if (typeof c.path !== 'string' || !c.path.startsWith('/') || /\/\/|token|secret|password/i.test(c.path)) throw new Error('UNSAFE_ROUTE')
    await page.goto('http://127.0.0.1:8061' + c.path, { waitUntil: 'domcontentloaded' })
  } else if (c.action === 'click') await element(c).click({ timeout: 15000 })
  else if (c.action === 'fill') await element(c).fill(c.value)
  else if (c.action === 'press') await element(c).press(c.key)
  else if (c.action === 'inspect') {
    const el = element(c)
    console.log(JSON.stringify({ status: 'VISIBLE_DOM_INSPECTION', count: await el.count(), html: (await el.evaluateAll(es => es.slice(0, 12).map(e => e.outerHTML))).join('\n').slice(0, 17000) }))
    return
  } else if (c.action !== 'snapshot') throw new Error('UNSUPPORTED_ACTION')
  await snapshot(c)
}
async function main() {
  if (fs.existsSync(output)) throw new Error('OUTPUT_EXISTS')
  fs.mkdirSync(output, { recursive: true })
  browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1600, height: 1050 } })
  page = await context.newPage()
  page.setDefaultTimeout(15000)
  page.on('response', response => {
    const u = new URL(response.url())
    if (/\/admin-api\/dcc\/(project-codes|file-categories)/.test(u.pathname)) observed.push({ path: u.pathname, method: response.request().method(), status: response.status() })
  })
  console.log('{"status":"OWN_PLAYWRIGHT_SESSION_READY","frontendPort":8061,"backendPort":48061,"output":"' + output + '"}')
  const rl = readline.createInterface({ input: process.stdin, terminal: false })
  rl.on('line', line => {
    chain = chain.then(() => command(JSON.parse(line))).catch(error => console.log(JSON.stringify({ status: 'UI_ACTION_FAILED', errorType: error.name, safeMessage: /^(UNSAFE_|LOGIN_REQUIRED|IDENTITY_UNAVAILABLE|OUTPUT_EXISTS|CAPTURE_EXISTS|BAD_CAPTURE_NAME|UNSUPPORTED_ACTION)/.test(error.message) ? error.message : 'inspect-visible-state; no automatic submit retry' })))
  })
}
main().catch(error => { console.log(JSON.stringify({ status: 'UI_START_FAILED', errorType: error.name })); process.exit(1) })
