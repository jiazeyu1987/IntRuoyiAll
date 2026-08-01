const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

function createCodexTestPlaywrightHarness(options = {}) {
  const scriptDeadlineMs = Math.min(
    240000,
    Math.max(30000, Number(process.env.CODEX_TEST_BROWSER_FLOW_TIMEOUT_MS || 240000))
  )
  const deadlineAt = Date.now() + scriptDeadlineMs
  const baseUrl = String(options.baseUrl || process.env.CODEX_TEST_FRONTEND_BASE_URL || 'http://127.0.0.1:8081').replace(/\/$/, '')
  const targetPath = options.targetPath || '/'
  const tempRoot = options.tempRoot || process.env.CODEX_TEST_WORKDIR || process.cwd()
  const frontendRoot = options.frontendRoot || process.env.CODEX_TEST_FRONTEND_ROOT || process.cwd()
  const browserExecutable = options.browserExecutable || process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || ''
  const checkpointCount = Number(options.checkpointCount || (options.checkpoints || []).length || 1)
  const summaryPrefix = options.summaryPrefix || options.caseName || 'Codex Playwright E2E'
  const fixedRouteCode = options.fixedRouteCode || ''
  const fixedRouteName = options.fixedRouteName || ''
  const fixedRouteDescription = options.fixedRouteDescription || ''

  let browserInstance = null
  let browserContext = null
  let browserPage = null
  let currentPhase = 'initializing browser'
  let outputPrinted = false
  const checkpointResults = []

  class EvidenceError extends Error {
    constructor(status, actualText, mismatchDescription = '') {
      super(actualText)
      this.name = 'EvidenceError'
      this.status = status
      this.actualText = actualText
      this.mismatchDescription = mismatchDescription
    }
  }

  function normalizeText(inputText) {
    return String(inputText || '').replace(/\s+/g, '').trim()
  }

  function safePageUrl() {
    try {
      return browserPage ? browserPage.url() : 'page-unavailable'
    } catch {
      return 'url-unavailable'
    }
  }

  function remainingTime(maximumMs) {
    return Math.max(250, Math.min(maximumMs, deadlineAt - Date.now() - 750))
  }

  function requireRemainingTime(phaseLabel) {
    if (Date.now() >= deadlineAt - 1000) {
      throw new EvidenceError(
        'BLOCKED',
        `Browser-script deadline is imminent during ${phaseLabel}; url=${safePageUrl()}`
      )
    }
  }

  function setPhase(phaseLabel) {
    currentPhase = phaseLabel
  }

  async function visibleBodyText(maxLength = 3500) {
    if (!browserPage) {
      return 'page-unavailable'
    }
    const bodyText = await browserPage.locator('body').innerText({ timeout: 1500 }).catch(() => 'body-text-unavailable')
    return String(bodyText || '').replace(/\s+/g, ' ').trim().slice(0, maxLength)
  }

  async function captureScreenshot(checkpointSort) {
    if (!browserPage) {
      return ''
    }
    const screenshotFile = path.join(
      tempRoot,
      `codex-test-checkpoint-${checkpointSort}-${Date.now()}.png`
    )
    const saved = await browserPage.screenshot({
      path: screenshotFile,
      fullPage: true,
      timeout: Math.min(8000, remainingTime(8000))
    }).then(() => true).catch(() => false)
    return saved ? screenshotFile : ''
  }

  function checkpointExists(checkpointSort) {
    return checkpointResults.some((entry) => entry.checkpointSort === checkpointSort)
  }

  function recordCheckpoint(checkpointSort, status, actualText, mismatchDescription = '', screenshotPath = '') {
    const checkpointEntry = {
      checkpointSort,
      status,
      actualText
    }
    if (status === 'FAIL') {
      checkpointEntry.mismatchDescription = mismatchDescription || 'Observed behavior did not match the checkpoint requirement.'
    }
    if (screenshotPath) {
      checkpointEntry.screenshotPath = screenshotPath
    }
    const existingIndex = checkpointResults.findIndex((entry) => entry.checkpointSort === checkpointSort)
    if (existingIndex >= 0) {
      checkpointResults[existingIndex] = checkpointEntry
    } else {
      checkpointResults.push(checkpointEntry)
    }
  }

  async function recordCheckpointError(checkpointSort, errorValue) {
    const errorStatus = errorValue instanceof EvidenceError ? errorValue.status : 'BLOCKED'
    const errorActual = errorValue instanceof EvidenceError
      ? errorValue.actualText
      : `${errorValue && errorValue.message ? errorValue.message : String(errorValue)}; url=${safePageUrl()}`
    const errorMismatch = errorValue instanceof EvidenceError ? errorValue.mismatchDescription : ''
    const errorScreenshot = await captureScreenshot(checkpointSort)
    recordCheckpoint(checkpointSort, errorStatus, errorActual, errorMismatch, errorScreenshot)
  }

  function fillUnfinishedCheckpoints(reasonText) {
    for (let checkpointSort = 1; checkpointSort <= checkpointCount; checkpointSort += 1) {
      if (!checkpointExists(checkpointSort)) {
        recordCheckpoint(
          checkpointSort,
          'BLOCKED',
          `${reasonText}; checkpoint was not completed. url=${safePageUrl()}`
        )
      }
    }
  }

  function buildOutput() {
    checkpointResults.sort((leftEntry, rightEntry) => leftEntry.checkpointSort - rightEntry.checkpointSort)
    const passedCount = checkpointResults.filter((entry) => entry.status === 'PASS').length
    const failedCount = checkpointResults.filter((entry) => entry.status === 'FAIL').length
    const blockedCount = checkpointResults.filter((entry) => entry.status === 'BLOCKED').length
    return {
      checkpointResults,
      summary: `${summaryPrefix}: ${passedCount} PASS, ${failedCount} FAIL, ${blockedCount} BLOCKED.`
    }
  }

  async function closeBrowserQuietly() {
    if (!browserInstance) {
      return
    }
    const closingBrowser = browserInstance
    browserInstance = null
    await Promise.race([
      closingBrowser.close().catch(() => {}),
      new Promise((resolveCloseWait) => setTimeout(resolveCloseWait, 5000))
    ])
  }

  async function printOutputAndExit() {
    if (outputPrinted) {
      return
    }
    outputPrinted = true
    await closeBrowserQuietly()
    process.stdout.write(`${JSON.stringify(buildOutput())}\n`)
    process.exit(0)
  }

  async function firstVisible(locatorValue) {
    const locatorCount = await locatorValue.count().catch(() => 0)
    for (let locatorIndex = 0; locatorIndex < locatorCount; locatorIndex += 1) {
      const locatorCandidate = locatorValue.nth(locatorIndex)
      if (await locatorCandidate.isVisible().catch(() => false)) {
        return locatorCandidate
      }
    }
    return null
  }

  async function pollForValue(producer, timeoutMs, intervalMs = 300) {
    const pollEnd = Math.min(Date.now() + timeoutMs, deadlineAt - 750)
    while (Date.now() < pollEnd) {
      const producedValue = await producer().catch(() => null)
      if (producedValue) {
        return producedValue
      }
      await new Promise((resolvePollDelay) => setTimeout(resolvePollDelay, intervalMs))
    }
    return null
  }

  async function waitForNoLoading(timeoutMs = 20000) {
    const loadingCleared = await pollForValue(async () => {
      const loadingMasks = browserPage.locator('.el-loading-mask')
      const loadingCount = await loadingMasks.count().catch(() => 0)
      for (let loadingIndex = 0; loadingIndex < loadingCount; loadingIndex += 1) {
        if (await loadingMasks.nth(loadingIndex).isVisible().catch(() => false)) {
          return null
        }
      }
      return true
    }, Math.min(timeoutMs, remainingTime(timeoutMs)), 250)
    if (!loadingCleared) {
      throw new EvidenceError(
        'BLOCKED',
        `Page remained under an Element Plus loading mask; url=${safePageUrl()}`
      )
    }
  }

  function parseEnvContents(fileContents) {
    const parsedValues = {}
    for (const rawLine of String(fileContents || '').split(/\r?\n/)) {
      const trimmedLine = rawLine.trim()
      if (!trimmedLine || trimmedLine.startsWith('#')) {
        continue
      }
      const equalsIndex = trimmedLine.indexOf('=')
      if (equalsIndex < 1) {
        continue
      }
      const envKey = trimmedLine.slice(0, equalsIndex).trim()
      let envValue = trimmedLine.slice(equalsIndex + 1).trim()
      if (
        (envValue.startsWith('"') && envValue.endsWith('"'))
        || (envValue.startsWith("'") && envValue.endsWith("'"))
      ) {
        envValue = envValue.slice(1, -1)
      }
      parsedValues[envKey] = envValue
    }
    return parsedValues
  }

  function readLocalLoginDefaults() {
    const envCandidates = [
      path.join(frontendRoot, '.env'),
      path.join(frontendRoot, '.env.development'),
      path.join(frontendRoot, '.env.local'),
      path.join(frontendRoot, '.env.development.local')
    ]
    const mergedDefaults = {}
    for (const envFile of envCandidates) {
      if (!fs.existsSync(envFile)) {
        continue
      }
      const envText = fs.readFileSync(envFile, 'utf8')
      Object.assign(mergedDefaults, parseEnvContents(envText))
    }
    return {
      tenant: mergedDefaults.VITE_APP_DEFAULT_LOGIN_TENANT || '',
      username: mergedDefaults.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
      password: mergedDefaults.VITE_APP_DEFAULT_LOGIN_PASSWORD || ''
    }
  }

  async function chooseLoginTenant(loginForm, tenantValue) {
    const tenantSelect = await firstVisible(loginForm.locator('.el-select'))
    if (!tenantSelect) {
      return
    }
    const tenantInput = await firstVisible(tenantSelect.locator('input[role="combobox"], input'))
    if (!tenantInput) {
      throw new EvidenceError(
        'BLOCKED',
        'A tenant selector is visible in the login form, but its combobox input is unavailable.'
      )
    }
    await tenantSelect.click({ timeout: remainingTime(8000) }).catch(async () => {
      await tenantInput.click({ timeout: remainingTime(5000) })
    })
    const tenantOption = await pollForValue(async () => {
      const optionLocators = browserPage.locator('.el-select-dropdown__item, [role="option"]')
      const optionCount = await optionLocators.count().catch(() => 0)
      let targetOneOption = null
      for (let optionIndex = 0; optionIndex < optionCount; optionIndex += 1) {
        const optionCandidate = optionLocators.nth(optionIndex)
        if (!await optionCandidate.isVisible().catch(() => false)) {
          continue
        }
        const optionText = normalizeText(await optionCandidate.innerText().catch(() => ''))
        if (tenantValue && optionText === normalizeText(tenantValue)) {
          return optionCandidate
        }
        if (optionText === '1' || optionText.includes('租户1')) {
          targetOneOption = optionCandidate
        }
      }
      return targetOneOption
    }, 5000, 250)
    if (tenantOption) {
      await tenantOption.click({ timeout: remainingTime(5000) })
      return
    }
    const currentTenantText = normalizeText(await tenantSelect.innerText().catch(() => ''))
    const currentTenantValue = await tenantInput.inputValue().catch(() => '')
    if (!currentTenantText && !currentTenantValue) {
      throw new EvidenceError(
        'BLOCKED',
        'The login tenant selector had no selectable target-tenant option and no current tenant value.'
      )
    }
    await browserPage.keyboard.press('Escape').catch(() => {})
  }

  async function performScopedLogin() {
    currentPhase = 'authenticating through the local login form'
    const loginForm = await firstVisible(browserPage.locator('.login-form'))
    if (!loginForm) {
      throw new EvidenceError('BLOCKED', `Login form did not render at ${safePageUrl()}.`)
    }

    const usernamePrimary = await firstVisible(
      loginForm.locator('input[placeholder="请输入用户名"]')
    )
    const usernameInput = usernamePrimary || await firstVisible(
      loginForm.locator('input.el-input__inner:not([type="password"]):not([role="combobox"])')
    )
    const passwordInput = await firstVisible(
      loginForm.locator('input[type="password"], input[placeholder="请输入密码"]')
    )
    if (!usernameInput || !passwordInput) {
      throw new EvidenceError(
        'BLOCKED',
        'The scoped login form is missing a visible username or password input.'
      )
    }

    const visibleUsernameDefault = await usernameInput.inputValue().catch(() => '')
    const visiblePasswordDefault = await passwordInput.inputValue().catch(() => '')
    const localLoginDefaults = readLocalLoginDefaults()
    const loginUsername = localLoginDefaults.username || visibleUsernameDefault
    const loginPassword = localLoginDefaults.password || visiblePasswordDefault
    const loginTenant = localLoginDefaults.tenant || '1'
    if (!loginUsername || !loginPassword) {
      throw new EvidenceError(
        'BLOCKED',
        'Local default login username or password is missing, so the login form was not submitted.'
      )
    }

    await chooseLoginTenant(loginForm, loginTenant)
    await usernameInput.fill(loginUsername)
    await passwordInput.fill(loginPassword)
    if (!await usernameInput.inputValue().catch(() => '')) {
      throw new EvidenceError('BLOCKED', 'The scoped username input remained empty after filling.')
    }
    if (!await passwordInput.inputValue().catch(() => '')) {
      throw new EvidenceError('BLOCKED', 'The scoped password input remained empty after filling.')
    }

    const loginResponsePromise = browserPage.waitForResponse(
      (responseValue) => responseValue.request().method() === 'POST'
        && responseValue.url().includes('/admin-api/system/auth/login'),
      { timeout: remainingTime(30000) }
    ).catch(() => null)
    const permissionResponsePromise = browserPage.waitForResponse(
      (responseValue) => responseValue.url().includes('/admin-api/system/auth/get-permission-info'),
      { timeout: remainingTime(60000) }
    ).catch(() => null)
    const loginButton = await firstVisible(
      loginForm.getByRole('button', { name: /登\s*录/ })
    ) || await firstVisible(
      loginForm.locator('button, .el-button').filter({ hasText: /登\s*录/ })
    )
    if (!loginButton) {
      throw new EvidenceError('BLOCKED', 'The scoped login form has no visible 登录 button.')
    }
    await loginButton.click({ timeout: remainingTime(10000) })
    const loginResponse = await loginResponsePromise
    if (!loginResponse) {
      throw new EvidenceError(
        'BLOCKED',
        `No /admin-api/system/auth/login response was observed after clicking 登录; url=${safePageUrl()}`
      )
    }
    const loginPayload = await loginResponse.json().catch(() => null)
    const loginBusinessCode = loginPayload && Object.prototype.hasOwnProperty.call(loginPayload, 'code')
      ? Number(loginPayload.code)
      : null
    if (!loginResponse.ok() || loginBusinessCode !== 0) {
      const loginMessage = loginPayload && (loginPayload.msg || loginPayload.message)
        ? String(loginPayload.msg || loginPayload.message)
        : `HTTP ${loginResponse.status()}`
      throw new EvidenceError(
        'BLOCKED',
        `Local login was rejected: ${loginMessage}; businessCode=${loginBusinessCode}; url=${safePageUrl()}`
      )
    }
    const permissionResponse = await permissionResponsePromise
    if (!permissionResponse || !permissionResponse.ok()) {
      throw new EvidenceError(
        'BLOCKED',
        `Login succeeded, but /admin-api/system/auth/get-permission-info was not observed successfully; url=${safePageUrl()}`
      )
    }
    const leftLogin = await pollForValue(async () => {
      const currentUrl = safePageUrl()
      return currentUrl.includes('/login') ? null : true
    }, 30000, 300)
    if (!leftLogin) {
      throw new EvidenceError(
        'BLOCKED',
        `Login returned business code 0, but the page remained on /login; url=${safePageUrl()}`
      )
    }
  }

  async function controlsVisible(controlSelectors = []) {
    const selectors = controlSelectors.length > 0
      ? controlSelectors
      : ['.table-quick-filter, .unified-list-template__quick-filter', '.el-table, .unified-list-template__table-shell']
    for (const selector of selectors) {
      const visibleControl = await firstVisible(browserPage.locator(selector))
      if (!visibleControl) {
        return false
      }
    }
    return true
  }

  async function ensureHistoryPageReady(pathValue = targetPath, controlSelectors = []) {
    currentPhase = `opening ${pathValue}`
    let lastNavigationError = ''
    for (let navigationAttempt = 1; navigationAttempt <= 2; navigationAttempt += 1) {
      requireRemainingTime(currentPhase)
      await browserPage.goto(`${baseUrl}${pathValue}`, {
        waitUntil: 'domcontentloaded',
        timeout: remainingTime(30000)
      }).catch((navigationError) => {
        lastNavigationError = navigationError.message
      })
      const readyState = await pollForValue(async () => {
        if (await controlsVisible(controlSelectors)) {
          return 'controls'
        }
        const loginVisible = await firstVisible(browserPage.locator('.login-form'))
        if (loginVisible || safePageUrl().includes('/login')) {
          return 'login'
        }
        return null
      }, 60000, 350)
      if (readyState === 'controls') {
        return
      }
      if (readyState === 'login') {
        await performScopedLogin()
        continue
      }
    }
    const pageText = await visibleBodyText()
    throw new EvidenceError(
      'BLOCKED',
      `Target page controls did not render after the login-or-controls loop. navigationError=${lastNavigationError || 'none'}; url=${safePageUrl()}; visibleText=${pageText}`
    )
  }

  async function scanResolvedAction(scopeLocator, actionRegex, rowBox = null, targetRowText = '') {
    const rawCandidates = scopeLocator.locator('button, .el-button, [role="button"], a, span')
    const rawCount = await rawCandidates.count().catch(() => 0)
    let disabledEvidence = ''
    for (let rawIndex = 0; rawIndex < rawCount; rawIndex += 1) {
      const rawCandidate = rawCandidates.nth(rawIndex)
      if (!await rawCandidate.isVisible().catch(() => false)) {
        continue
      }
      const rawCandidateText = normalizeText(
        await rawCandidate.innerText().catch(async () => rawCandidate.textContent().catch(() => ''))
      )
      if (!actionRegex.test(rawCandidateText)) {
        continue
      }
      const resolvedHandleValue = await rawCandidate.evaluateHandle(
        (nodeValue) => nodeValue.closest('button,.el-button,[role="button"],a') || nodeValue
      ).catch(() => null)
      const resolvedElement = resolvedHandleValue ? resolvedHandleValue.asElement() : null
      if (!resolvedElement) {
        if (resolvedHandleValue) {
          await resolvedHandleValue.dispose().catch(() => {})
        }
        continue
      }
      const resolvedState = await resolvedElement.evaluate((elementValue) => {
        const elementStyle = window.getComputedStyle(elementValue)
        const elementRect = elementValue.getBoundingClientRect()
        const ancestorRow = elementValue.closest('tr,.el-table__row')
        return {
          tag: elementValue.tagName,
          className: String(elementValue.className || ''),
          disabled: Boolean(elementValue.disabled),
          ariaDisabled: elementValue.getAttribute('aria-disabled') || '',
          loading: elementValue.classList.contains('is-loading'),
          visible: elementRect.width > 0
            && elementRect.height > 0
            && elementStyle.visibility !== 'hidden'
            && elementStyle.display !== 'none',
          rowText: ancestorRow ? String(ancestorRow.innerText || '') : '',
          centerY: elementRect.top + (elementRect.height / 2)
        }
      }).catch(() => null)
      if (!resolvedState || !resolvedState.visible) {
        await resolvedElement.dispose().catch(() => {})
        continue
      }
      if (rowBox) {
        const targetCenterY = rowBox.y + (rowBox.height / 2)
        const sameTargetRow = normalizeText(resolvedState.rowText).includes(normalizeText(targetRowText))
        const sameVisualRow = Math.abs(resolvedState.centerY - targetCenterY) <= Math.max(18, rowBox.height / 2)
        if (!sameTargetRow && !sameVisualRow) {
          await resolvedElement.dispose().catch(() => {})
          continue
        }
      }
      if (resolvedState.disabled || resolvedState.ariaDisabled === 'true' || resolvedState.loading) {
        disabledEvidence = `${resolvedState.tag}.${resolvedState.className}; disabled=${resolvedState.disabled}; aria-disabled=${resolvedState.ariaDisabled}; loading=${resolvedState.loading}`
        await resolvedElement.dispose().catch(() => {})
        continue
      }
      return { element: resolvedElement, text: rawCandidateText, disabledEvidence }
    }
    return { element: null, text: '', disabledEvidence }
  }

  async function clickVisibleTextAction(scopeLocator, actionRegex, options = {}) {
    const actionDeadline = Math.min(Date.now() + (options.waitEnabledMs || 3000), deadlineAt - 1000)
    let latestDisabledEvidence = ''
    while (Date.now() < actionDeadline) {
      const resolvedAction = await scanResolvedAction(
        scopeLocator,
        actionRegex,
        options.rowBox || null,
        options.targetRowText || ''
      )
      latestDisabledEvidence = resolvedAction.disabledEvidence || latestDisabledEvidence
      if (resolvedAction.element) {
        try {
          await resolvedAction.element.scrollIntoViewIfNeeded().catch(() => {})
          await resolvedAction.element.click({ timeout: remainingTime(options.clickTimeoutMs || 8000) })
          await resolvedAction.element.dispose().catch(() => {})
          return { clicked: true, text: resolvedAction.text, disabledEvidence: latestDisabledEvidence }
        } catch (clickError) {
          await resolvedAction.element.dispose().catch(() => {})
          return {
            clicked: false,
            text: resolvedAction.text,
            disabledEvidence: latestDisabledEvidence,
            clickError: clickError.message
          }
        }
      }
      await new Promise((resolveActionDelay) => setTimeout(resolveActionDelay, 300))
    }
    return { clicked: false, text: '', disabledEvidence: latestDisabledEvidence, clickError: '' }
  }

  async function closeSpecificOverlayFailSoft(overlayLocator) {
    if (!overlayLocator || !await overlayLocator.isVisible().catch(() => false)) {
      return true
    }
    const closeAction = await clickVisibleTextAction(
      overlayLocator,
      /^(关闭|返回|取消)$/,
      { waitEnabledMs: 1200, clickTimeoutMs: 3500 }
    ).catch(() => ({ clicked: false }))
    if (!closeAction.clicked && await overlayLocator.isVisible().catch(() => false)) {
      const headerClose = await firstVisible(
        overlayLocator.locator('.el-dialog__headerbtn, .el-drawer__close-btn')
      )
      if (headerClose) {
        await headerClose.click({ timeout: remainingTime(3500) }).catch(() => {})
      }
    }
    if (await overlayLocator.isVisible().catch(() => false)) {
      await browserPage.keyboard.press('Escape').catch(() => {})
    }
    const overlayClosed = await pollForValue(async () => (
      await overlayLocator.isVisible().catch(() => false) ? null : true
    ), 5000, 200)
    return Boolean(overlayClosed)
  }

  async function closeAllBusinessOverlaysBeforeQuery() {
    for (let closeAttempt = 0; closeAttempt < 3; closeAttempt += 1) {
      const visibleOverlay = await firstVisible(
        browserPage.locator('.el-dialog, .el-drawer, .el-overlay-dialog')
      )
      if (!visibleOverlay) {
        return
      }
      await closeSpecificOverlayFailSoft(visibleOverlay)
    }
    const remainingOverlay = await firstVisible(
      browserPage.locator('.el-dialog, .el-drawer, .el-overlay-dialog')
    )
    if (remainingOverlay) {
      const remainingText = (await remainingOverlay.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      throw new EvidenceError(
        'BLOCKED',
        `A visible Element Plus overlay remained before the list query: ${remainingText.slice(0, 1200)}`
      )
    }
  }

  async function getQuickFilterRoot() {
    const quickRoot = await firstVisible(
      browserPage.locator('.table-quick-filter, .unified-list-template__quick-filter')
    )
    if (!quickRoot) {
      throw new EvidenceError(
        'BLOCKED',
        `No visible list quick filter was found; url=${safePageUrl()}`
      )
    }
    return quickRoot
  }

  async function getQuickFilterFieldInfo(quickRoot) {
    const fieldArea = await firstVisible(quickRoot.locator('.table-quick-filter__field'))
    if (!fieldArea) {
      return { fieldArea: null, fieldKind: 'code', labelText: '路线编码(assumed)' }
    }
    const fieldInnerText = await fieldArea.innerText().catch(() => '')
    const fieldInput = await firstVisible(fieldArea.locator('input'))
    const fieldInputValue = fieldInput ? await fieldInput.inputValue().catch(() => '') : ''
    const combinedFieldText = normalizeText(`${fieldInnerText} ${fieldInputValue}`)
    if (combinedFieldText.includes('路线名称')) {
      return { fieldArea, fieldKind: 'name', labelText: '路线名称' }
    }
    if (combinedFieldText.includes('路线编码')) {
      return { fieldArea, fieldKind: 'code', labelText: '路线编码' }
    }
    return { fieldArea, fieldKind: 'code', labelText: combinedFieldText || '路线编码(assumed)' }
  }

  async function trySwitchQuickFilterField(quickRoot, desiredKind) {
    const currentFieldInfo = await getQuickFilterFieldInfo(quickRoot)
    if (!currentFieldInfo.fieldArea || currentFieldInfo.fieldKind === desiredKind) {
      return await getQuickFilterFieldInfo(quickRoot)
    }
    const desiredText = desiredKind === 'name' ? '路线名称' : '路线编码'
    await currentFieldInfo.fieldArea.click({ timeout: remainingTime(5000) }).catch(async () => {
      const fieldInput = await firstVisible(currentFieldInfo.fieldArea.locator('input'))
      if (fieldInput) {
        await fieldInput.click({ timeout: remainingTime(5000) })
      }
    })
    const option = await pollForValue(async () => {
      const optionsList = browserPage.locator('.el-select-dropdown__item, [role="option"]')
      const optionCount = await optionsList.count().catch(() => 0)
      for (let optionIndex = 0; optionIndex < optionCount; optionIndex += 1) {
        const optionCandidate = optionsList.nth(optionIndex)
        if (!await optionCandidate.isVisible().catch(() => false)) {
          continue
        }
        const optionText = normalizeText(await optionCandidate.innerText().catch(() => ''))
        if (optionText.includes(desiredText)) {
          return optionCandidate
        }
      }
      return null
    }, 3000, 250)
    if (!option) {
      await browserPage.keyboard.press('Escape').catch(() => {})
      return await getQuickFilterFieldInfo(quickRoot)
    }
    await option.click({ timeout: remainingTime(5000) })
    return await getQuickFilterFieldInfo(quickRoot)
  }

  async function getQuickFilterValueInput(quickRoot) {
    const valueArea = await firstVisible(quickRoot.locator('.table-quick-filter__value'))
    const scopedRoot = valueArea || quickRoot
    const input = await firstVisible(scopedRoot.locator('input.el-input__inner, input'))
    if (!input) {
      throw new EvidenceError('BLOCKED', 'The quick-filter value input was not visible.')
    }
    return input
  }

  async function getRouteTableShell() {
    const tableShell = await firstVisible(
      browserPage.locator('.unified-list-template__table-shell, .el-table')
    )
    if (!tableShell) {
      throw new EvidenceError('BLOCKED', `The route table was not visible; url=${safePageUrl()}`)
    }
    return tableShell
  }

  async function collectVisibleRouteRows() {
    const tableShell = await getRouteTableShell()
    const rowLocators = tableShell.locator('.el-table__body-wrapper tbody tr.el-table__row, tbody tr.el-table__row, .el-table__body-wrapper tbody tr, tbody tr')
    const rowCount = await rowLocators.count().catch(() => 0)
    const rows = []
    for (let rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
      const rowCandidate = rowLocators.nth(rowIndex)
      if (!await rowCandidate.isVisible().catch(() => false)) {
        continue
      }
      const rowText = (await rowCandidate.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      if (!rowText) {
        continue
      }
      rows.push({ row: rowCandidate, locator: rowCandidate, text: rowText, index: rowIndex })
    }
    return rows
  }

  async function clickQuickFilterQuery(quickRoot) {
    const queryAction = await firstVisible(
      quickRoot.locator('button, .el-button').filter({ hasText: /查询|搜索/ })
    ) || await firstVisible(
      browserPage.getByRole('button', { name: /查询|搜索/ })
    )
    if (!queryAction) {
      throw new EvidenceError('BLOCKED', 'The quick-filter 查询/搜索 button was not visible.')
    }
    await queryAction.click({ timeout: remainingTime(8000) })
  }

  async function queryRoute(preferredKind = 'code', routeCode = fixedRouteCode, routeName = fixedRouteName) {
    await closeAllBusinessOverlaysBeforeQuery()
    const quickRoot = await getQuickFilterRoot()
    const fieldInfo = await trySwitchQuickFilterField(quickRoot, preferredKind)
    const submittedValue = fieldInfo.fieldKind === 'name' ? routeName : routeCode
    const valueInput = await getQuickFilterValueInput(quickRoot)
    await valueInput.fill(submittedValue)
    const queryResponsePromise = browserPage.waitForResponse(
      (responseValue) => responseValue.request().method() === 'GET'
        && /\/admin-api\/mes\/pro\/route\/page/.test(responseValue.url()),
      { timeout: Math.min(20000, remainingTime(20000)) }
    ).catch(() => null)
    await clickQuickFilterQuery(quickRoot)
    await Promise.race([
      queryResponsePromise,
      new Promise((resolveWait) => setTimeout(resolveWait, 1200))
    ])
    await waitForNoLoading(20000)
    const rows = await collectVisibleRouteRows()
    const matches = rows.filter((entry) => entry.text.includes(routeCode) || entry.text.includes(routeName))
    return {
      fieldKind: fieldInfo.fieldKind,
      fieldLabel: fieldInfo.labelText,
      submittedValue,
      rows,
      matches
    }
  }

  async function queryFixedRoute(preferredKind = 'code') {
    return await queryRoute(preferredKind, fixedRouteCode, fixedRouteName)
  }

  async function waitForMessageBox() {
    return await pollForValue(async () => firstVisible(
      browserPage.locator('.el-message-box, .el-overlay-message-box')
    ), 5000, 200)
  }

  async function confirmVisibleMessageBox(messageBoxLocator) {
    const messageText = (await messageBoxLocator.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
    const primaryButton = await firstVisible(
      messageBoxLocator.locator('.el-message-box__btns button, button, .el-button').filter({ hasText: /确定|确认|删除|启用|停用/ })
    )
    if (!primaryButton) {
      throw new EvidenceError(
        'BLOCKED',
        `Element Plus confirmation box has no visible primary action. messageBox=${messageText.slice(0, 1200)}`
      )
    }
    try {
      await primaryButton.click({ timeout: remainingTime(8000) })
    } catch (clickError) {
      const retryButton = await firstVisible(
        messageBoxLocator.locator('.el-message-box__btns button, button, .el-button').filter({ hasText: /确定|确认|删除|启用|停用/ })
      )
      if (!retryButton) {
        throw new EvidenceError(
          'BLOCKED',
          `The message-box primary action could not be re-scoped after click interception. messageBox=${messageText.slice(0, 1200)}`
        )
      }
      await retryButton.click({ timeout: remainingTime(8000) })
    }
    const boxHidden = await pollForValue(async () => (
      await messageBoxLocator.isVisible().catch(() => false) ? null : true
    ), 15000, 250)
    if (!boxHidden) {
      const remainingMessageText = (await messageBoxLocator.innerText().catch(() => messageText))
        .replace(/\s+/g, ' ')
        .trim()
      throw new EvidenceError(
        'BLOCKED',
        `Element Plus confirmation box remained visible after its primary action: ${remainingMessageText.slice(0, 1200)}`
      )
    }
    return messageText
  }

  function resolveRowLocator(rowEntry) {
    if (rowEntry && typeof rowEntry.locator === 'function') {
      return rowEntry
    }
    if (rowEntry && rowEntry.row && typeof rowEntry.row.locator === 'function') {
      return rowEntry.row
    }
    if (rowEntry && rowEntry.locator && typeof rowEntry.locator.locator === 'function') {
      return rowEntry.locator
    }
    return null
  }

  async function clickRouteRowAction(rowEntry, actionRegex, actionLabel) {
    const rowLocator = resolveRowLocator(rowEntry)
    if (!rowLocator || typeof rowLocator.locator !== 'function') {
      throw new EvidenceError('BLOCKED', `The ${actionLabel} row wrapper did not contain a Playwright Locator.`)
    }
    const rowBox = await rowLocator.boundingBox().catch(() => null)
    const scopedAction = await clickVisibleTextAction(
      rowLocator,
      actionRegex,
      { waitEnabledMs: 4000, clickTimeoutMs: 8000 }
    )
    if (scopedAction.clicked) {
      return scopedAction
    }
    if (rowBox) {
      const fixedColumnAction = await clickVisibleTextAction(
        browserPage,
        actionRegex,
        {
          rowBox,
          targetRowText: rowEntry.text || '',
          waitEnabledMs: 5000,
          clickTimeoutMs: 8000
        }
      )
      if (fixedColumnAction.clicked) {
        return fixedColumnAction
      }
      const fixedEvidence = fixedColumnAction.disabledEvidence || fixedColumnAction.clickError || ''
      throw new EvidenceError(
        'BLOCKED',
        `${actionLabel} action was visible only as an unavailable row action. row=${rowEntry.text}; actionState=${fixedEvidence || 'no enabled resolved action element'}`
      )
    }
    throw new EvidenceError(
      'BLOCKED',
      `${actionLabel} action could not be resolved in the target row. row=${rowEntry.text}; actionState=${scopedAction.disabledEvidence || scopedAction.clickError || 'missing'}`
    )
  }

  async function deleteMatchedRoute(rowEntry, contextLabel) {
    currentPhase = `${contextLabel}: deleting the fixed route`
    const deleteResponsePromise = browserPage.waitForResponse(
      (responseValue) => responseValue.request().method() !== 'GET'
        && /\/admin-api\/mes\/pro\/route\/(delete|remove)/.test(responseValue.url()),
      { timeout: Math.min(20000, remainingTime(20000)) }
    ).catch(() => null)
    await clickRouteRowAction(rowEntry, /^删除$/, '删除')
    const deleteMessageBox = await waitForMessageBox()
    let confirmationText = 'no confirmation dialog observed'
    if (deleteMessageBox) {
      confirmationText = await confirmVisibleMessageBox(deleteMessageBox)
    }
    const deleteResponse = await deleteResponsePromise
    let deleteResponseEvidence = 'delete response endpoint not captured'
    if (deleteResponse) {
      const deletePayload = await deleteResponse.json().catch(() => null)
      const deleteBusinessCode = deletePayload && Object.prototype.hasOwnProperty.call(deletePayload, 'code')
        ? Number(deletePayload.code)
        : null
      deleteResponseEvidence = `HTTP ${deleteResponse.status()}, businessCode=${deleteBusinessCode}`
      if (!deleteResponse.ok() || (deleteBusinessCode !== null && deleteBusinessCode !== 0)) {
        const deleteMessage = deletePayload && (deletePayload.msg || deletePayload.message)
          ? String(deletePayload.msg || deletePayload.message)
          : deleteResponseEvidence
        throw new EvidenceError(
          'FAIL',
          `Route delete request failed: ${deleteMessage}`,
          'The row delete action did not complete successfully.'
        )
      }
    }
    return {
      confirmationText,
      deleteResponseEvidence,
      rowText: rowEntry.text
    }
  }

  async function ensureFixedRouteAbsent(contextLabel, routeCode = fixedRouteCode, routeName = fixedRouteName) {
    const deletions = []
    for (let deleteRound = 1; deleteRound <= 3; deleteRound += 1) {
      const queryOutcome = await queryRoute('code', routeCode, routeName)
      if (queryOutcome.matches.length === 0) {
        return {
          deletions,
          fieldLabel: queryOutcome.fieldLabel,
          submittedValue: queryOutcome.submittedValue,
          rows: queryOutcome.rows
        }
      }
      const targetRow = queryOutcome.matches[0]
      deletions.push(await deleteMatchedRoute(targetRow, `${contextLabel} round ${deleteRound}`))
      const absenceCheck = await queryRoute('code', routeCode, routeName)
      if (absenceCheck.matches.length === 0) {
        return {
          deletions,
          fieldLabel: absenceCheck.fieldLabel,
          submittedValue: absenceCheck.submittedValue,
          rows: absenceCheck.rows
        }
      }
    }
    const finalQuery = await queryRoute('code', routeCode, routeName)
    throw new EvidenceError(
      'FAIL',
      `Reset/cleanup still found route rows after delete attempts. matches=${finalQuery.matches.map((entry) => entry.text).join(' | ')}`,
      'The fixed test route could not be removed from the visible route list.'
    )
  }

  async function clickAddRouteButton() {
    const addAction = await firstVisible(
      browserPage.getByRole('button', { name: /新增工艺路线|新增/ })
    ) || await firstVisible(
      browserPage.locator('button, .el-button').filter({ hasText: /新增工艺路线|新增/ })
    )
    if (!addAction) {
      throw new EvidenceError('BLOCKED', 'The 新增工艺路线/新增 button was not visible.')
    }
    await addAction.click({ timeout: remainingTime(8000) })
  }

  async function findVisibleDialog(titleRegex) {
    const dialogs = browserPage.locator('.el-dialog, .el-drawer')
    const dialogCount = await dialogs.count().catch(() => 0)
    for (let dialogIndex = 0; dialogIndex < dialogCount; dialogIndex += 1) {
      const dialogCandidate = dialogs.nth(dialogIndex)
      if (!await dialogCandidate.isVisible().catch(() => false)) {
        continue
      }
      const dialogText = await dialogCandidate.innerText().catch(() => '')
      if (titleRegex.test(dialogText)) {
        return dialogCandidate
      }
    }
    return null
  }

  async function findExactFormItem(dialogLocator, labelRegex, rejectRegex = null) {
    const formItems = dialogLocator.locator('.el-form-item')
    const itemCount = await formItems.count().catch(() => 0)
    for (let itemIndex = 0; itemIndex < itemCount; itemIndex += 1) {
      const itemCandidate = formItems.nth(itemIndex)
      if (!await itemCandidate.isVisible().catch(() => false)) {
        continue
      }
      const labelText = normalizeText(await itemCandidate.locator('.el-form-item__label').first().innerText().catch(() => ''))
      const itemText = normalizeText(await itemCandidate.innerText().catch(() => ''))
      if (!labelRegex.test(labelText) && !labelRegex.test(itemText)) {
        continue
      }
      if (rejectRegex && (rejectRegex.test(labelText) || rejectRegex.test(itemText))) {
        continue
      }
      return itemCandidate
    }
    return null
  }

  async function fillDialogFormItem(dialogLocator, labelRegex, valueText, rejectRegex = null) {
    const formItem = await findExactFormItem(dialogLocator, labelRegex, rejectRegex)
    if (!formItem) {
      throw new EvidenceError(
        'BLOCKED',
        `form item not found for ${labelRegex}; phase=${currentPhase}`
      )
    }
    const valueInput = await firstVisible(
      formItem.locator('input.el-input__inner, input, textarea')
    )
    if (!valueInput) {
      throw new EvidenceError(
        'BLOCKED',
        `form item ${labelRegex} has no visible input; phase=${currentPhase}`
      )
    }
    await valueInput.fill(valueText)
    return valueInput
  }

  async function readRouteFormValue(dialogLocator, labelRegex, rejectRegex = null) {
    const itemCandidate = await findExactFormItem(dialogLocator, labelRegex, rejectRegex)
    if (!itemCandidate) {
      return ''
    }
    const valueInput = await firstVisible(
      itemCandidate.locator('input.el-input__inner, input, textarea')
    )
    if (!valueInput) {
      return ''
    }
    return valueInput.evaluate(
      (inputElement) => inputElement.value || inputElement.getAttribute('value') || ''
    ).catch(() => '')
  }

  async function waitForRouteFormValues(dialogLocator, expectedCode = fixedRouteCode, expectedName = fixedRouteName, timeoutMs = 30000) {
    return await pollForValue(async () => {
      const codeValue = await readRouteFormValue(dialogLocator, /^(编码|路线编码)$/)
      const nameValue = await readRouteFormValue(
        dialogLocator,
        /^(名称|路线名称)$/,
        /编码|基础信息/
      )
      if (codeValue === expectedCode && nameValue === expectedName) {
        return { codeValue, nameValue }
      }
      return null
    }, Math.min(timeoutMs, remainingTime(timeoutMs)), 500)
  }

  async function clickDialogBusinessAction(dialogLocator) {
    const businessActionRegex = /^(保存|确定|提交|确认复制|复制|确认发布|发布|启用|停用|确认删除|删除)$/
    const footerScope = dialogLocator.locator('.el-dialog__footer, .el-drawer__footer')
    const footerAction = await clickVisibleTextAction(
      footerScope,
      businessActionRegex,
      { waitEnabledMs: 2500, clickTimeoutMs: 8000 }
    )
    if (footerAction.clicked) {
      return footerAction.text
    }
    const dialogAction = await clickVisibleTextAction(
      dialogLocator,
      businessActionRegex,
      { waitEnabledMs: 2500, clickTimeoutMs: 8000 }
    )
    if (dialogAction.clicked) {
      return dialogAction.text
    }
    const pageAction = await clickVisibleTextAction(
      browserPage,
      /^(保存|确定|提交)$/,
      { waitEnabledMs: 1800, clickTimeoutMs: 8000 }
    )
    if (pageAction.clicked) {
      return pageAction.text
    }
    throw new EvidenceError(
      'BLOCKED',
      `No visible enabled dialog save action was found. footerState=${footerAction.disabledEvidence || footerAction.clickError || 'missing'}; dialogState=${dialogAction.disabledEvidence || dialogAction.clickError || 'missing'}`
    )
  }

  async function waitForSuccessToast(timeoutMs = 15000) {
    return await pollForValue(async () => {
      const messageCandidates = browserPage.locator('.el-message, .el-notification')
      const messageCount = await messageCandidates.count().catch(() => 0)
      for (let messageIndex = 0; messageIndex < messageCount; messageIndex += 1) {
        const messageCandidate = messageCandidates.nth(messageIndex)
        if (!await messageCandidate.isVisible().catch(() => false)) {
          continue
        }
        const messageText = (await messageCandidate.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
        if (/新增成功|保存成功|操作成功/.test(messageText)) {
          return messageText
        }
      }
      return null
    }, Math.min(timeoutMs, remainingTime(timeoutMs)), 200)
  }

  async function createFixedRoute() {
    await closeAllBusinessOverlaysBeforeQuery()
    await clickAddRouteButton()
    const createDialog = await pollForValue(
      async () => findVisibleDialog(/新增工艺路线/),
      15000,
      250
    )
    if (!createDialog) {
      throw new EvidenceError(
        'BLOCKED',
        `Clicking 新增 did not open the 新增工艺路线 dialog; url=${safePageUrl()}`
      )
    }
    currentPhase = 'waiting for RouteFormContent and filling the fixed route'
    const formReady = await pollForValue(async () => {
      const contentVisible = await firstVisible(createDialog.locator('.route-form-content'))
      const codeItem = await findExactFormItem(createDialog, /^(编码|路线编码)$/)
      const nameItem = await findExactFormItem(
        createDialog,
        /^(名称|路线名称)$/,
        /编码|基础信息/
      )
      return contentVisible || (codeItem && nameItem) ? true : null
    }, 30000, 300)
    if (!formReady) {
      const latestDialogText = (await createDialog.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      throw new EvidenceError(
        'BLOCKED',
        `新增工艺路线 dialog shell opened, but RouteFormContent/编码/名称 fields did not render within 30 seconds. dialogText=${latestDialogText.slice(0, 1800)}`
      )
    }
    const codeInput = await fillDialogFormItem(
      createDialog,
      /^(编码|路线编码)$/,
      fixedRouteCode
    )
    const nameInput = await fillDialogFormItem(
      createDialog,
      /^(名称|路线名称)$/,
      fixedRouteName,
      /编码|基础信息/
    )
    const descriptionItem = await findExactFormItem(
      createDialog,
      /^(说明|备注|路线说明)$/
    )
    if (descriptionItem && fixedRouteDescription) {
      const descriptionInput = await firstVisible(
        descriptionItem.locator('input.el-input__inner, input, textarea')
      )
      if (descriptionInput) {
        await descriptionInput.fill(fixedRouteDescription)
      }
    }
    const verifiedCode = await codeInput.evaluate(
      (inputElement) => inputElement.value || inputElement.getAttribute('value') || ''
    ).catch(() => '')
    const verifiedName = await nameInput.evaluate(
      (inputElement) => inputElement.value || inputElement.getAttribute('value') || ''
    ).catch(() => '')
    if (verifiedCode !== fixedRouteCode || verifiedName !== fixedRouteName) {
      throw new EvidenceError(
        'BLOCKED',
        `Dialog-scoped values were not stable before save: code=${verifiedCode}; name=${verifiedName}`
      )
    }

    currentPhase = 'saving the fixed route'
    const saveResponsePromise = browserPage.waitForResponse(
      (responseValue) => responseValue.request().method() !== 'GET'
        && /\/admin-api\/mes\/pro\/route\/(create|save)/.test(responseValue.url()),
      { timeout: Math.min(20000, remainingTime(20000)) }
    ).then(async (responseValue) => {
      const responsePayload = await responseValue.json().catch(() => null)
      const responseCode = responsePayload && Object.prototype.hasOwnProperty.call(responsePayload, 'code')
        ? Number(responsePayload.code)
        : null
      return {
        ok: responseValue.ok(),
        httpStatus: responseValue.status(),
        businessCode: responseCode,
        message: responsePayload && (responsePayload.msg || responsePayload.message)
          ? String(responsePayload.msg || responsePayload.message)
          : ''
      }
    }).catch(() => null)
    const clickedSaveText = await clickDialogBusinessAction(createDialog)
    const successToastPromise = waitForSuccessToast(15000)
    const saveEvidence = await Promise.race([
      saveResponsePromise.then((responseData) => (responseData ? { type: 'response', data: responseData } : null)),
      successToastPromise.then((toastText) => (toastText ? { type: 'toast', data: toastText } : null)),
      new Promise((resolveSaveTimeout) => setTimeout(() => resolveSaveTimeout(null), remainingTime(16000)))
    ])
    const capturedSaveResponse = await Promise.race([
      saveResponsePromise,
      new Promise((resolveResponseWait) => setTimeout(() => resolveResponseWait(null), 2500))
    ])
    const capturedSuccessToast = saveEvidence && saveEvidence.type === 'toast'
      ? saveEvidence.data
      : await waitForSuccessToast(1800)
    if (capturedSaveResponse) {
      if (
        !capturedSaveResponse.ok
        || (capturedSaveResponse.businessCode !== null && capturedSaveResponse.businessCode !== 0)
      ) {
        throw new EvidenceError(
          'FAIL',
          `新增保存 request failed after clicking ${clickedSaveText}: ${capturedSaveResponse.message || `HTTP ${capturedSaveResponse.httpStatus}`}; businessCode=${capturedSaveResponse.businessCode}`,
          'The create/save business request did not return success.'
        )
      }
    }
    if (!capturedSuccessToast && !capturedSaveResponse) {
      const createDialogText = (await createDialog.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      throw new EvidenceError(
        'BLOCKED',
        `No save-success toast or create/save response was observed after clicking ${clickedSaveText}. dialogText=${createDialogText.slice(0, 1800)}`
      )
    }
    const closeSucceeded = await closeSpecificOverlayFailSoft(createDialog)
    return {
      saveAction: clickedSaveText,
      toast: capturedSuccessToast || '',
      response: capturedSaveResponse,
      postSaveDialogClosed: closeSucceeded
    }
  }

  async function findClickableCodeEntry(rowEntry, routeCode = fixedRouteCode) {
    const rowLocator = resolveRowLocator(rowEntry)
    if (!rowLocator || typeof rowLocator.locator !== 'function') {
      return null
    }
    const rowBox = await rowLocator.boundingBox().catch(() => null)
    const codeCandidates = rowLocator.locator(
      'button.el-button.is-link, .el-button.is-link, a, [role="link"], button, span'
    )
    const codeCount = await codeCandidates.count().catch(() => 0)
    for (let codeIndex = 0; codeIndex < codeCount; codeIndex += 1) {
      const codeCandidate = codeCandidates.nth(codeIndex)
      if (!await codeCandidate.isVisible().catch(() => false)) {
        continue
      }
      const codeText = normalizeText(await codeCandidate.innerText().catch(() => ''))
      if (codeText === normalizeText(routeCode) && await codeCandidate.isEnabled().catch(() => true)) {
        return codeCandidate
      }
    }
    const pageCandidates = browserPage.locator(
      'button.el-button.is-link, .el-button.is-link, a, [role="link"], button, span'
    )
    const pageCandidateCount = await pageCandidates.count().catch(() => 0)
    for (let codeIndex = 0; codeIndex < pageCandidateCount; codeIndex += 1) {
      const codeCandidate = pageCandidates.nth(codeIndex)
      if (!await codeCandidate.isVisible().catch(() => false)) {
        continue
      }
      const codeText = normalizeText(await codeCandidate.innerText().catch(() => ''))
      if (codeText !== normalizeText(routeCode)) {
        continue
      }
      const resolvedHandle = await codeCandidate.evaluateHandle(
        (nodeValue) => nodeValue.closest('button.el-button.is-link,.el-button.is-link,a,[role="link"],button') || nodeValue
      ).catch(() => null)
      const resolvedElement = resolvedHandle ? resolvedHandle.asElement() : null
      if (!resolvedElement) {
        if (resolvedHandle) {
          await resolvedHandle.dispose().catch(() => {})
        }
        continue
      }
      const resolvedState = await resolvedElement.evaluate((elementValue) => {
        const elementStyle = window.getComputedStyle(elementValue)
        const elementRect = elementValue.getBoundingClientRect()
        return {
          disabled: Boolean(elementValue.disabled),
          ariaDisabled: elementValue.getAttribute('aria-disabled') || '',
          visible: elementRect.width > 0
            && elementRect.height > 0
            && elementStyle.visibility !== 'hidden'
            && elementStyle.display !== 'none',
          centerY: elementRect.top + (elementRect.height / 2)
        }
      }).catch(() => null)
      if (!resolvedState || !resolvedState.visible || resolvedState.disabled || resolvedState.ariaDisabled === 'true') {
        await resolvedElement.dispose().catch(() => {})
        continue
      }
      if (rowBox) {
        const targetCenterY = rowBox.y + (rowBox.height / 2)
        const sameVisualRow = Math.abs(resolvedState.centerY - targetCenterY) <= Math.max(18, rowBox.height / 2)
        if (!sameVisualRow) {
          await resolvedElement.dispose().catch(() => {})
          continue
        }
      }
      return resolvedElement
    }
    return null
  }

  async function openRouteDetailFromList(routeCode = fixedRouteCode, routeName = fixedRouteName) {
    for (let detailAttempt = 1; detailAttempt <= 2; detailAttempt += 1) {
      currentPhase = `opening route detail, attempt ${detailAttempt}`
      const codeQuery = await queryRoute('code', routeCode, routeName)
      if (codeQuery.matches.length !== 1) {
        throw new EvidenceError(
          'FAIL',
          `Expected one fixed route before opening detail, but visible matching rows=${codeQuery.matches.length}; rows=${codeQuery.rows.map((entry) => entry.text).join(' | ')}`,
          'The saved fixed route was not uniquely visible in the route list.'
        )
      }
      const targetRow = codeQuery.matches[0]
      const routeCodeEntry = await findClickableCodeEntry(targetRow, routeCode)
      if (!routeCodeEntry) {
        throw new EvidenceError(
          'BLOCKED',
          `The visible route row has no clickable 路线编码 link for ${routeCode}. row=${targetRow.text}`
        )
      }
      const detailResponsePromise = browserPage.waitForResponse(
        (responseValue) => responseValue.request().method() === 'GET'
          && /\/admin-api\/mes\/pro\/route\/get\?/.test(responseValue.url()),
        { timeout: Math.min(30000, remainingTime(30000)) }
      ).catch(() => null)
      await routeCodeEntry.click({ timeout: remainingTime(10000) })
      const detailDialog = await pollForValue(
        async () => findVisibleDialog(/工艺路线详情/),
        12000,
        250
      )
      if (!detailDialog) {
        if (detailAttempt === 1) {
          await new Promise((resolveDetailRetry) => setTimeout(resolveDetailRetry, 500))
          continue
        }
        throw new EvidenceError(
          'BLOCKED',
          `Clicking the route-code link did not open 工艺路线详情; row=${targetRow.text}`
        )
      }
      await Promise.race([
        detailResponsePromise,
        new Promise((resolveDetailResponseWait) => setTimeout(resolveDetailResponseWait, 2500))
      ])
      const detailValues = await waitForRouteFormValues(detailDialog, routeCode, routeName, 30000)
      if (detailValues) {
        return {
          dialog: detailDialog,
          codeValue: detailValues.codeValue,
          nameValue: detailValues.nameValue,
          rowText: targetRow.text,
          queryField: codeQuery.fieldLabel
        }
      }
      const blankCode = await readRouteFormValue(detailDialog, /^(编码|路线编码)$/)
      const blankName = await readRouteFormValue(
        detailDialog,
        /^(名称|路线名称)$/,
        /编码|基础信息/
      )
      await closeSpecificOverlayFailSoft(detailDialog)
      if (detailAttempt === 2) {
        throw new EvidenceError(
          'FAIL',
          `工艺路线详情 form values did not match after two route-code-link attempts: code=${blankCode || '<empty>'}; name=${blankName || '<empty>'}; expectedCode=${routeCode}; expectedName=${routeName}`,
          'The opened detail form did not show the fixed route code and name.'
        )
      }
    }
    throw new EvidenceError('BLOCKED', 'The fixed-route detail flow ended without a detail dialog.')
  }

  async function findVisibleTab(dialogLocator, exactTabText) {
    const tabCandidates = dialogLocator.locator('.el-tabs__item, [role="tab"]')
    const tabCount = await tabCandidates.count().catch(() => 0)
    for (let tabIndex = 0; tabIndex < tabCount; tabIndex += 1) {
      const tabCandidate = tabCandidates.nth(tabIndex)
      if (!await tabCandidate.isVisible().catch(() => false)) {
        continue
      }
      const tabText = normalizeText(await tabCandidate.innerText().catch(() => ''))
      if (tabText === normalizeText(exactTabText)) {
        return tabCandidate
      }
    }
    return null
  }

  async function verifyDetailTabs(detailDialog) {
    currentPhase = 'verifying 基础信息、流转关系图、关联产品 tabs'
    const basicTab = await findVisibleTab(detailDialog, '基础信息')
    const flowTab = await findVisibleTab(detailDialog, '流转关系图')
    const productTab = await findVisibleTab(detailDialog, '关联产品')
    const missingTabs = []
    if (!basicTab) {
      missingTabs.push('基础信息')
    }
    if (!flowTab) {
      missingTabs.push('流转关系图')
    }
    if (!productTab) {
      missingTabs.push('关联产品')
    }
    if (missingTabs.length > 0) {
      const detailText = (await detailDialog.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      throw new EvidenceError(
        'FAIL',
        `工艺路线详情 is missing visible tabs: ${missingTabs.join('、')}. dialogText=${detailText.slice(0, 1800)}`,
        'The detail page did not display all required 基础信息、流转关系图、关联产品 tabs.'
      )
    }

    await flowTab.click({ timeout: remainingTime(8000) })
    const flowActive = await pollForValue(async () => {
      const activeClass = await flowTab.getAttribute('class').catch(() => '')
      const ariaSelected = await flowTab.getAttribute('aria-selected').catch(() => '')
      return String(activeClass).includes('is-active') || ariaSelected === 'true' ? true : null
    }, 5000, 200)
    if (!flowActive) {
      throw new EvidenceError(
        'FAIL',
        'The visible 流转关系图 tab could not be activated.',
        'The route-flow tab was present but did not open.'
      )
    }
    const graphContainer = await pollForValue(async () => firstVisible(
      detailDialog.locator(
        '.route-flow-graph-designer, .route-flow-graph-designer__canvas, .route-flow-graph-designer__node, [data-flow-node="route-process"]'
      )
    ), 10000, 300)
    const flowEmptyState = await firstVisible(detailDialog.locator('.el-empty'))
    if (!graphContainer && flowEmptyState) {
      const emptyText = (await flowEmptyState.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      throw new EvidenceError(
        'FAIL',
        `流转关系图 opened with an actual empty state and no graph container: ${emptyText}`,
        'The flow tab had no graph designer, route-process node, connector canvas, or visible graph content.'
      )
    }

    await productTab.click({ timeout: remainingTime(8000) })
    const productActive = await pollForValue(async () => {
      const activeClass = await productTab.getAttribute('class').catch(() => '')
      const ariaSelected = await productTab.getAttribute('aria-selected').catch(() => '')
      return String(activeClass).includes('is-active') || ariaSelected === 'true' ? true : null
    }, 5000, 200)
    if (!productActive) {
      throw new EvidenceError(
        'FAIL',
        'The visible 关联产品 tab could not be activated.',
        'The related-products tab was present but did not open.'
      )
    }

    await basicTab.click({ timeout: remainingTime(8000) })
    const basicActive = await pollForValue(async () => {
      const activeClass = await basicTab.getAttribute('class').catch(() => '')
      const ariaSelected = await basicTab.getAttribute('aria-selected').catch(() => '')
      return String(activeClass).includes('is-active') || ariaSelected === 'true' ? true : null
    }, 5000, 200)
    if (!basicActive) {
      throw new EvidenceError(
        'FAIL',
        'The visible 基础信息 tab could not be reactivated after tab verification.',
        'The basic-information tab did not remain usable.'
      )
    }
    return {
      tabs: ['基础信息', '流转关系图', '关联产品'],
      graphContainerVisible: Boolean(graphContainer),
      relatedProductsOpened: true
    }
  }

  async function verifyUniqueFixedRouteAfterSave() {
    currentPhase = 'verifying the saved fixed route in the list'
    const codeQuery = await queryFixedRoute('code')
    if (codeQuery.matches.length !== 1) {
      throw new EvidenceError(
        'FAIL',
        `After save, querying ${codeQuery.fieldLabel}=${codeQuery.submittedValue} returned ${codeQuery.matches.length} visible fixed-route rows. rows=${codeQuery.rows.map((entry) => entry.text).join(' | ')}`,
        'The newly saved route was not uniquely searchable in the list.'
      )
    }
    const exactCodeMatches = codeQuery.matches.filter((entry) => entry.text.includes(fixedRouteCode))
    const exactNameMatches = codeQuery.matches.filter((entry) => entry.text.includes(fixedRouteName))
    if (exactCodeMatches.length !== 1 || exactNameMatches.length !== 1) {
      throw new EvidenceError(
        'FAIL',
        `The unique visible row did not contain both fixed values. row=${codeQuery.matches[0].text}`,
        'The saved list row did not show the required fixed route code and name.'
      )
    }

    const nameQuery = await queryFixedRoute('name')
    if (nameQuery.fieldKind === 'name' && nameQuery.matches.length !== 1) {
      throw new EvidenceError(
        'FAIL',
        `After switching to 路线名称 and submitting ${fixedRouteName}, visible fixed-route rows=${nameQuery.matches.length}. rows=${nameQuery.rows.map((entry) => entry.text).join(' | ')}`,
        'The saved route was not uniquely searchable by the fixed route name.'
      )
    }
    return {
      codeSearchField: codeQuery.fieldLabel,
      nameSearchField: nameQuery.fieldLabel,
      nameSearchUsed: nameQuery.fieldKind === 'name',
      rowText: codeQuery.matches[0].text
    }
  }

  async function runBrowserFlow(scenarioFlow) {
    currentPhase = 'launching Chrome'
    const launchOptions = { headless: true }
    if (browserExecutable) {
      launchOptions.executablePath = browserExecutable
    }
    browserInstance = await chromium.launch(launchOptions)
    browserContext = await browserInstance.newContext({
      viewport: { width: 1440, height: 1000 },
      locale: 'zh-CN'
    })
    browserPage = await browserContext.newPage()
    browserPage.setDefaultTimeout(10000)
    browserPage.setDefaultNavigationTimeout(30000)
    await scenarioFlow(api)
  }

  async function handleDeadline() {
    const deadlineUrl = safePageUrl()
    const deadlineText = await visibleBodyText(3000)
    fillUnfinishedCheckpoints(
      `Browser-script deadline reached during ${currentPhase}; url=${deadlineUrl}; visibleText=${deadlineText}`
    )
    await printOutputAndExit()
  }

  async function startExecution(scenarioFlow) {
    const flowPromise = runBrowserFlow(scenarioFlow)
      .then(() => ({ kind: 'flow' }))
      .catch((flowError) => ({ kind: 'error', error: flowError }))
    const deadlinePromise = new Promise((resolveDeadline) => {
      setTimeout(() => resolveDeadline({ kind: 'deadline' }), scriptDeadlineMs)
    })
    const completedRace = await Promise.race([flowPromise, deadlinePromise])
    if (completedRace.kind === 'deadline') {
      await handleDeadline()
      return
    }
    if (completedRace.kind === 'error') {
      const unexpectedText = completedRace.error && completedRace.error.message
        ? completedRace.error.message
        : String(completedRace.error)
      const unfinishedSort = Array.from({ length: checkpointCount }, (_, index) => index + 1)
        .find((sortValue) => !checkpointExists(sortValue)) || checkpointCount
      const unexpectedScreenshot = await captureScreenshot(unfinishedSort)
      recordCheckpoint(
        unfinishedSort,
        'BLOCKED',
        `Unexpected browser-script error during ${currentPhase}: ${unexpectedText}; url=${safePageUrl()}`,
        '',
        unexpectedScreenshot
      )
      fillUnfinishedCheckpoints(`Unexpected browser-script error during ${currentPhase}`)
    }
    await printOutputAndExit()
  }

  const api = {
    EvidenceError,
    get browser() {
      return browserInstance
    },
    get context() {
      return browserContext
    },
    get page() {
      return browserPage
    },
    get currentPhase() {
      return currentPhase
    },
    setPhase,
    normalizeText,
    safePageUrl,
    remainingTime,
    requireRemainingTime,
    visibleBodyText,
    captureScreenshot,
    checkpointExists,
    recordCheckpoint,
    recordCheckpointError,
    fillUnfinishedCheckpoints,
    buildOutput,
    closeBrowserQuietly,
    printOutputAndExit,
    firstVisible,
    pollForValue,
    waitForNoLoading,
    parseEnvContents,
    readLocalLoginDefaults,
    chooseLoginTenant,
    performScopedLogin,
    controlsVisible,
    ensureHistoryPageReady,
    scanResolvedAction,
    clickVisibleTextAction,
    closeSpecificOverlayFailSoft,
    closeAllBusinessOverlaysBeforeQuery,
    getQuickFilterRoot,
    getQuickFilterFieldInfo,
    trySwitchQuickFilterField,
    getQuickFilterValueInput,
    getRouteTableShell,
    collectVisibleRouteRows,
    clickQuickFilterQuery,
    queryRoute,
    queryFixedRoute,
    waitForMessageBox,
    confirmVisibleMessageBox,
    resolveRowLocator,
    clickRouteRowAction,
    deleteMatchedRoute,
    ensureFixedRouteAbsent,
    clickAddRouteButton,
    findVisibleDialog,
    findExactFormItem,
    fillDialogFormItem,
    readRouteFormValue,
    waitForRouteFormValues,
    clickDialogBusinessAction,
    waitForSuccessToast,
    createFixedRoute,
    findClickableCodeEntry,
    openRouteDetailFromList,
    findVisibleTab,
    verifyDetailTabs,
    verifyUniqueFixedRouteAfterSave,
    runBrowserFlow,
    startExecution,
    run: startExecution
  }

  return api
}

module.exports = {
  createCodexTestPlaywrightHarness
}
