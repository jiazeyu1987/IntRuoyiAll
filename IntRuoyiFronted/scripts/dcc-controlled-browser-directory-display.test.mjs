async (page) => {
  const frontendBase = 'http://127.0.0.1:8081'
  const targetPath = '/dcc/controlled-file/browser'
  const loginTenants = ['测试租户']
  const loginUsername = 'aoteman'
  const loginPassword = 'admin123'
  const screenshotPath =
    'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-browser-directory-display-red.png'

  let directoryTreePayload = null

  page.on('response', async (response) => {
    if (!response.url().includes('/admin-api/dcc/directories/tree')) {
      return
    }
    try {
      directoryTreePayload = await response.json()
    } catch {
      directoryTreePayload = { code: -1, msg: 'directory_tree_response_unreadable' }
    }
  })

  const normalizeText = (value) => String(value || '').replace(/\s+/g, ' ').trim()

  const flattenDirectories = (nodes, result = []) => {
    for (const node of Array.isArray(nodes) ? nodes : []) {
      result.push(node)
      flattenDirectories(node?.children, result)
    }
    return result
  }

  const waitQuiet = async (timeout = 15000) => {
    await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
    await page.waitForTimeout(1000)
  }

  const ensureLogin = async () => {
    const attempts = []

    await page.goto(`${frontendBase}/login?redirect=${encodeURIComponent(targetPath)}`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })

    for (const tenantName of loginTenants) {
      await page.goto(`${frontendBase}/login?redirect=${encodeURIComponent(targetPath)}`, {
        waitUntil: 'domcontentloaded',
        timeout: 30000
      })
      await page.waitForSelector('.login-form input', { timeout: 15000 })
      await page.waitForTimeout(500)

      const loginForm = page.locator('.login-form')
      const textboxes = loginForm.getByRole('textbox')
      const textboxCount = await textboxes.count()
      const passwordInput = loginForm.locator('input[type="password"]').first()
      const tenantInput = textboxCount >= 3 ? textboxes.nth(0) : null
      const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)

      if (tenantInput) {
        await tenantInput.fill('')
        await tenantInput.fill(tenantName)
      }
      await usernameInput.fill('')
      await usernameInput.fill(loginUsername)
      await passwordInput.fill('')
      await passwordInput.fill(loginPassword)

      await Promise.all([
        loginForm.locator('.el-button--primary').first().click(),
        page.waitForTimeout(1500)
      ])
      await waitQuiet(8000)

      const success = !page.url().includes('/login')
      attempts.push({ tenantName, success, url: page.url() })
      if (success) {
        return attempts
      }
    }

    throw new Error(`login_failed_for_all_tenants:${JSON.stringify(attempts)}`)
  }

  const loginAttempts = await ensureLogin()
  await page.goto(`${frontendBase}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  await waitQuiet(12000)

  if (!directoryTreePayload || Number(directoryTreePayload.code) !== 0) {
    throw new Error(`directory_tree_response_missing_or_failed:${JSON.stringify(directoryTreePayload)}`)
  }

  const directories = flattenDirectories(directoryTreePayload.data)
  const candidate = directories.find((item) => {
    const name = normalizeText(item?.name)
    const code = normalizeText(item?.code)
    return name && code && name !== code
  })
  if (!candidate) {
    throw new Error('directory_tree_candidate_missing_name_code_pair')
  }

  const treeNodes = page.locator('.browser-directory-node')
  const nodeCount = await treeNodes.count()
  if (nodeCount === 0) {
    throw new Error('browser_directory_nodes_missing')
  }

  let candidateNodeText = ''
  for (let index = 0; index < nodeCount; index += 1) {
    const text = normalizeText(await treeNodes.nth(index).textContent())
    if (text.includes(normalizeText(candidate.name))) {
      candidateNodeText = text
      break
    }
  }

  if (!candidateNodeText) {
    throw new Error(
      `browser_directory_candidate_not_found:name=${normalizeText(candidate.name)} code=${normalizeText(candidate.code)}`
    )
  }

  if (candidateNodeText.includes(normalizeText(candidate.code))) {
    await page.screenshot({ path: screenshotPath, fullPage: true })
    throw new Error(
      `directory_code_still_visible_in_tree:name=${normalizeText(candidate.name)} code=${normalizeText(candidate.code)} text=${candidateNodeText}`
    )
  }

  const candidateNameLocator = page
    .locator('.browser-directory-node__name')
    .filter({ hasText: normalizeText(candidate.name) })
    .first()
  if ((await candidateNameLocator.count()) === 0) {
    throw new Error(`browser_directory_name_locator_missing:${normalizeText(candidate.name)}`)
  }
  await candidateNameLocator.click()
  await waitQuiet(8000)

  const subtitleText = normalizeText(await page.locator('.browser-list-subtitle').first().textContent())
  if (subtitleText.includes(normalizeText(candidate.code))) {
    await page.screenshot({ path: screenshotPath, fullPage: true })
    throw new Error(
      `directory_code_still_visible_in_subtitle:name=${normalizeText(candidate.name)} code=${normalizeText(candidate.code)} subtitle=${subtitleText}`
    )
  }

  await page.screenshot({
    path: 'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-browser-directory-display-green.png',
    fullPage: true
  })

  return {
    finalUrl: page.url(),
    loginAttempts,
    candidate: {
      id: candidate.id,
      name: normalizeText(candidate.name),
      code: normalizeText(candidate.code)
    },
    candidateNodeText,
    subtitleText
  }
}
