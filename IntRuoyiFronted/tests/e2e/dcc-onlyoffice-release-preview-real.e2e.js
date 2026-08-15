const assert = require('node:assert/strict')
const { chromium } = require('playwright')

function requireEnvironment(name) {
  const value = String(process.env[name] || '').trim()
  assert.ok(value, `${name} is required for the OnlyOffice release preview gate`)
  return value
}

function requireFileId(name) {
  const value = requireEnvironment(name)
  assert.match(value, /^\d+$/, `${name} must be a numeric controlled-file ID`)
  return value
}

const config = {
  baseUrl: requireEnvironment('DCC_ONLYOFFICE_RELEASE_E2E_BASE_URL').replace(/\/+$/, ''),
  tenant: requireEnvironment('DCC_ONLYOFFICE_RELEASE_E2E_TENANT'),
  username: requireEnvironment('DCC_ONLYOFFICE_RELEASE_E2E_USERNAME'),
  password: requireEnvironment('DCC_ONLYOFFICE_RELEASE_E2E_PASSWORD'),
  timeout: 120000
}

const samples = [
  {
    kind: 'DOCX',
    extension: '.docx',
    fileId: requireFileId('DCC_ONLYOFFICE_RELEASE_E2E_DOCX_FILE_ID')
  },
  {
    kind: 'XLSX',
    extension: '.xlsx',
    fileId: requireFileId('DCC_ONLYOFFICE_RELEASE_E2E_XLSX_FILE_ID')
  },
  {
    kind: 'PPTX',
    extension: '.pptx',
    fileId: requireFileId('DCC_ONLYOFFICE_RELEASE_E2E_PPTX_FILE_ID')
  }
]

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: config.timeout })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: config.timeout })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.equal(response.ok(), true, `login HTTP status ${response.status()}`)
  assert.ok(payload && [0, 200].includes(payload.code), 'login business response must be successful')
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: config.timeout,
    waitUntil: 'commit'
  })
}

function isPreviewMetadataResponse(response, sample) {
  if (response.request().method() !== 'GET') return false
  return new URL(response.url()).pathname.endsWith(
    `/dcc/controlled-files/${sample.fileId}/preview-metadata`
  )
}

function isOnlyOfficeContentResponse(response) {
  if (response.request().method() !== 'GET' || !response.ok()) return false
  const pathname = new URL(response.url()).pathname
  return pathname.includes('/cache/files/') && !pathname.endsWith('/api.js')
}

async function verifySample(page, sample) {
  let onlyOfficeContentResponse = null
  const captureOnlyOfficeContent = (response) => {
    if (!onlyOfficeContentResponse && isOnlyOfficeContentResponse(response)) {
      onlyOfficeContentResponse = response
    }
  }
  page.on('response', captureOnlyOfficeContent)
  const metadataResponsePromise = page.waitForResponse(
    (response) => isPreviewMetadataResponse(response, sample),
    { timeout: config.timeout }
  )
  try {
    await page.goto(
      `${config.baseUrl}/dcc/controlled-file/detail/${sample.fileId}?viewer=1&from=release-gate`,
      { waitUntil: 'commit', timeout: config.timeout }
    )

    const metadataResponse = await metadataResponsePromise
    const metadataPayload = await metadataResponse.json().catch(() => null)
    assert.equal(metadataResponse.ok(), true, `${sample.kind} preview metadata HTTP must succeed`)
    assert.ok(
      metadataPayload && [0, 200].includes(metadataPayload.code),
      `${sample.kind} preview metadata business response must succeed`
    )
    const metadata = metadataPayload.data || {}
    assert.equal(metadata.previewKind, 'OFFICE', `${sample.kind} sample must use OFFICE preview`)
    assert.ok(
      String(metadata.fileName || '').toLowerCase().endsWith(sample.extension),
      `${sample.kind} sample file extension must be ${sample.extension}`
    )
    assert.ok(metadata.onlyofficeBaseUrl, `${sample.kind} OnlyOffice base URL must be present`)
    assert.ok(metadata.onlyofficeDocumentUrl, `${sample.kind} signed document URL must be present`)
    assert.equal(
      String(metadata.previewUnavailableReason || '').trim(),
      '',
      `${sample.kind} preview must not report an unavailable reason`
    )

    await page.getByText('只读预览态', { exact: true }).waitFor({ state: 'visible', timeout: config.timeout })
    await page.getByText('禁止截图/外传', { exact: true }).first().waitFor({
      state: 'visible',
      timeout: config.timeout
    })
    await page.locator('.onlyoffice-viewer-frame iframe').waitFor({
      state: 'attached',
      timeout: config.timeout
    })
    await page.waitForFunction(() => {
      const frame = document.querySelector('.onlyoffice-viewer-frame iframe')
      return frame && String(frame.getAttribute('src') || '').includes('/web-apps/')
    }, null, { timeout: config.timeout })
    await page.waitForTimeout(1500)

    const viewerError = page.locator('.onlyoffice-viewer-shell .el-alert--error:visible').first()
    const viewerErrorCount = await viewerError.count()
    const viewerErrorText = viewerErrorCount > 0 ? await viewerError.innerText() : ''
    assert.equal(
      viewerErrorCount,
      0,
      `${sample.kind} must not display an OnlyOffice error alert: ${viewerErrorText}`
    )
    assert.equal(
      await page.getByText(/错误码\s*-4|下载失败|OnlyOffice 文档加载失败/).count(),
      0,
      `${sample.kind} must not display an OnlyOffice download error`
    )
    assert.ok(
      onlyOfficeContentResponse || page.frames().some((frame) => frame.url().includes('/web-apps/')),
      `${sample.kind} OnlyOffice content frame must load`
    )

    console.log(`PASS: ${sample.kind} controlled preview loaded, fileId=${sample.fileId}`)
  } finally {
    page.off('response', captureOnlyOfficeContent)
  }
}

async function main() {
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const dccWriteRequests = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(config.timeout)
    page.setDefaultNavigationTimeout(config.timeout)
    page.on('request', (request) => {
      if (
        request.url().includes('/admin-api/dcc/') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
      ) {
        dccWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
      }
    })

    await login(page)
    for (const sample of samples) {
      await verifySample(page, sample)
    }
    assert.deepEqual(dccWriteRequests, [], `DCC write request detected: ${dccWriteRequests.join(', ')}`)
    console.log('GREEN: dcc-onlyoffice-release-preview-real -> PASS, DOCX/XLSX/PPTX loaded')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`BLOCKER: dcc-onlyoffice-release-preview-real -> ${error.stack || error.message}`)
  process.exit(1)
})
