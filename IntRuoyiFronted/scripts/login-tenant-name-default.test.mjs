async (page) => {
  const frontendBase = 'http://127.0.0.1:8081'
  const expectedTenantName = '芋道源码'
  const expectedUsername = 'admin'
  const expectedPassword = 'admin123'
  const oldDefaultTenantName = '测试租户'
  const customTestTenantCredentials = {
    tenantName: oldDefaultTenantName,
    username: 'custom-user',
    password: 'custom-pass-123',
    rememberMe: true
  }
  const redScreenshotPath =
    'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/login-tenant-name-default-red.png'
  const greenScreenshotPath =
    'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/login-tenant-name-default-green.png'

  const openPage = async (path) => {
    await page.goto(`${frontendBase}${path}`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForLoadState('networkidle').catch(() => null)
    await page.waitForTimeout(1000)
  }

  const getVisibleCredentials = async () => {
    const form = page.locator('.login-form:visible')
    const textboxes = form.getByRole('textbox')
    const textboxCount = await textboxes.count()
    if (textboxCount < 2) {
      throw new Error(`login_textbox_count_unexpected:${textboxCount}`)
    }
    const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)
    const passwordInput = form.locator('input[type="password"]').first()
    const formText = await form.innerText()
    const inputValues = await form.locator('input').evaluateAll((inputs) =>
      inputs.map((input) => input.value).filter(Boolean)
    )
    const tenantName =
      [expectedTenantName, oldDefaultTenantName, '瑛泰源码'].find((name) =>
        formText.includes(name) || inputValues.includes(name)
      ) || ''
    const username = await usernameInput.inputValue()
    const password = await passwordInput.inputValue()
    return { tenantName, username, password }
  }

  const assertCredentials = async (label, expectedCredentials) => {
    const credentials = await getVisibleCredentials()
    if (credentials.tenantName !== expectedCredentials.tenantName) {
      await page.screenshot({ path: redScreenshotPath, fullPage: true })
      throw new Error(
        `${label}_tenant_mismatch: expected=${expectedCredentials.tenantName}, actual=${credentials.tenantName}`
      )
    }
    if (credentials.username !== expectedCredentials.username) {
      await page.screenshot({ path: redScreenshotPath, fullPage: true })
      throw new Error(
        `${label}_username_mismatch: expected=${expectedCredentials.username}, actual=${credentials.username}`
      )
    }
    if (credentials.password !== expectedCredentials.password) {
      await page.screenshot({ path: redScreenshotPath, fullPage: true })
      throw new Error(
        `${label}_password_mismatch: expected=${expectedCredentials.password}, actual=${credentials.password}`
      )
    }
    return credentials
  }

  const clearBrowserStorage = async () => {
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
  }

  const rememberLoginForm = async (loginForm) => {
    await page.evaluate(async (form) => {
      const authUtil = await import('/src/utils/auth.ts')
      authUtil.setLoginForm(form)
    }, loginForm)
  }

  const expectedDefaultCredentials = {
    tenantName: expectedTenantName,
    username: expectedUsername,
    password: expectedPassword
  }

  await page.goto(`${frontendBase}/login`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  await clearBrowserStorage()

  await openPage('/login')
  const loginDefaults = await assertCredentials('login_default', expectedDefaultCredentials)

  await rememberLoginForm({
    tenantName: oldDefaultTenantName,
    username: 'aoteman',
    password: expectedPassword,
    rememberMe: true
  })

  await openPage('/login')
  const cachedAotemanCredentials = await assertCredentials(
    'old_test_aoteman_cache',
    expectedDefaultCredentials
  )

  await clearBrowserStorage()
  await rememberLoginForm({
    tenantName: oldDefaultTenantName,
    username: 'admin',
    password: expectedPassword,
    rememberMe: true
  })

  await openPage('/login')
  const cachedAdminCredentials = await assertCredentials(
    'old_test_admin_cache',
    expectedDefaultCredentials
  )

  await clearBrowserStorage()
  await rememberLoginForm(customTestTenantCredentials)

  await openPage('/login')
  const customTestTenantDefaults = await assertCredentials(
    'custom_test_tenant_cache',
    customTestTenantCredentials
  )

  await clearBrowserStorage()
  await openPage('/social-login')
  const socialDefaults = await assertCredentials('social_default', expectedDefaultCredentials)

  await page.screenshot({ path: greenScreenshotPath, fullPage: true })

  return {
    defaultTenantName: loginDefaults.tenantName,
    defaultUsername: loginDefaults.username,
    defaultPassword: loginDefaults.password,
    cachedAotemanTenantName: cachedAotemanCredentials.tenantName,
    cachedAdminTenantName: cachedAdminCredentials.tenantName,
    customTestTenantName: customTestTenantDefaults.tenantName,
    socialTenantName: socialDefaults.tenantName
  }
}
