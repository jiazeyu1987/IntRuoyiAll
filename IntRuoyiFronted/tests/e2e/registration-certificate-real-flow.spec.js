const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260816-registration-certificate-full-business-delivery'
)
const ARTIFACT_DIR = process.env.REG_CERT_E2E_ARTIFACT_DIR
  ? path.resolve(process.env.REG_CERT_E2E_ARTIFACT_DIR)
  : path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-real-flow-result.json')

const DEFAULT_BASE_URL = 'http://127.0.0.1:8095'

function readDotEnvValue(name) {
  for (const fileName of ['.env.local', '.env']) {
    const filePath = path.join(FRONTEND_ROOT, fileName)
    if (!fs.existsSync(filePath)) continue
    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
    for (const line of lines) {
      const match = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/)
      if (match && match[1] === name) {
        return match[2].replace(/^['"]|['"]$/g, '')
      }
    }
  }
  return ''
}

const config = {
  baseUrl: (
    process.env.REG_CERT_E2E_BASE_URL ||
    process.env.E2E_BASE_URL ||
    DEFAULT_BASE_URL
  ).replace(/\/+$/, ''),
  tenant:
    process.env.REG_CERT_E2E_TENANT ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT') ||
    '芋道源码',
  username:
    process.env.REG_CERT_E2E_USERNAME ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME') ||
    'admin',
  password: process.env.REG_CERT_E2E_PASSWORD || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD'),
  reviewerUsername: process.env.REG_CERT_E2E_REVIEWER_USERNAME || '',
  reviewerPassword:
    process.env.REG_CERT_E2E_REVIEWER_PASSWORD ||
    process.env.REG_CERT_E2E_PASSWORD ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD'),
  runKey: process.env.REG_CERT_E2E_RUN_KEY || '',
  requireWriteFixture: process.env.REG_CERT_E2E_REQUIRE_WRITE_FIXTURE !== 'false',
  requireApprovalFlow: process.env.REG_CERT_E2E_REQUIRE_APPROVAL_FLOW === 'true'
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractPageResult(payload) {
  const data = payload && payload.data
  return {
    list: Array.isArray(data?.list) ? data.list : [],
    total: Number(data?.total || 0)
  }
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page, credentials = { username: config.username, password: config.password }) {
  expect(credentials.password, 'login password must be available without logging it').toBeTruthy()

  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(credentials.username)
  await form.locator('input[type="password"]').first().fill(credentials.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(
    isBusinessOk(loginPayload),
    `login business code ${loginPayload.code}, message=${loginPayload.msg || ''}`
  ).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function registrationPath(response, suffix) {
  if (response.request().method() !== 'GET') return false
  const pathname = new URL(response.url()).pathname
  return pathname.endsWith(`/admin-api/dcc/registration-certificates${suffix}`)
}

test.describe('AC-040 domestic registration certificate real flow', () => {
  test('real menu, read, detail, old-index, config and write-prerequisite gates are observable', async ({
    page,
    browser
  }) => {
    test.setTimeout(240000)

    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      reviewerUsername: config.requireApprovalFlow ? config.reviewerUsername : undefined,
      responses: [],
      writeRequests: [],
      directFileRequests: [],
      failedResponses: [],
      consoleErrors: [],
      pageErrors: []
    }

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') evidence.consoleErrors.push(message.text())
    })
    page.on('request', (request) => {
      const method = request.method()
      const url = request.url()
      if (
        !['GET', 'HEAD', 'OPTIONS'].includes(method) &&
        url.includes('/admin-api/dcc/registration-certificates')
      ) {
        evidence.writeRequests.push({ method, url })
      }
      if (
        /\/admin-api\/infra\/file\//.test(url) ||
        /\/admin-api\/infra\/file\/get\?/.test(url) ||
        /\/admin-api\/dcc\/registration-certificates\/files\/[^/]+\/preview-metadata/.test(url)
      ) {
        evidence.directFileRequests.push({ method, url })
      }
    })
    page.on('response', async (response) => {
      const url = response.url()
      if (response.status() >= 400) {
        evidence.failedResponses.push({
          method: response.request().method(),
          path: new URL(url).pathname,
          status: response.status()
        })
      }
      if (!url.includes('/admin-api/dcc/registration-certificates')) return
      const payload = await readJsonResponse(response)
      evidence.responses.push({
        method: response.request().method(),
        path: new URL(url).pathname,
        status: response.status(),
        code: payload.code,
        message: payload.msg || payload.message || ''
      })
    })

    try {
      const permissionResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/system/auth/get-permission-info') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await login(page)
      const permissionResponse = await permissionResponsePromise
      const permissionPayload = await readJsonResponse(permissionResponse)
      expect(
        isBusinessOk(permissionPayload),
        `permission-info code ${permissionPayload.code}`
      ).toBe(true)
      const permissions = JSON.stringify(permissionPayload.data || {})
      expect(
        permissions,
        'logged-in account must expose registration certificate query permission'
      ).toContain('dcc:registration-certificate:query-current')

      const pageResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/page'),
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })

      await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
        timeout: 60000
      })
      await expect(page.getByRole('button', { name: '上传注册证' })).toBeVisible()

      const pagePayload = await readJsonResponse(await pageResponsePromise)
      expect(
        isBusinessOk(pagePayload),
        `current page code ${pagePayload.code}, message=${pagePayload.msg || ''}`
      ).toBe(true)

      const currentPage = extractPageResult(pagePayload)
      evidence.currentCount = currentPage.total
      expect(
        currentPage.list.length,
        'B-TEST requires at least one approved current registration certificate fixture'
      ).toBeGreaterThan(0)

      const oldIndexResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/old-index/page'),
        {
          timeout: 60000
        }
      )
      await page.getByRole('tab', { name: '老证' }).click()
      await expect(page.locator('[data-testid="registration-certificate-old-index"]')).toBeVisible({
        timeout: 60000
      })
      const oldIndexPayload = await readJsonResponse(await oldIndexResponsePromise)
      expect(
        isBusinessOk(oldIndexPayload),
        `old index code ${oldIndexPayload.code}, message=${oldIndexPayload.msg || ''}`
      ).toBe(true)
      const oldIndexPage = extractPageResult(oldIndexPayload)
      evidence.oldIndexCount = oldIndexPage.total
      expect(
        oldIndexPage.list.length,
        'B-TEST requires at least one approved old certificate fixture'
      ).toBeGreaterThan(0)

      const selected = currentPage.list[0]
      evidence.selectedCertificateId = selected.certificateId
      evidence.selectedVersionId = selected.versionId
      evidence.selectedCertificateNo = selected.certificateNo

      const detailResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, `/${selected.certificateId}`),
        { timeout: 60000 }
      )
      const historyResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, `/${selected.certificateId}/history`),
        { timeout: 60000 }
      )
      await page
        .locator('.el-table:visible')
        .first()
        .getByRole('button', { name: '详情' })
        .first()
        .click()
      await expect(
        page.locator('[data-testid="registration-certificate-detail-page"]')
      ).toBeVisible({ timeout: 60000 })

      const detailPayload = await readJsonResponse(await detailResponsePromise)
      const historyPayload = await readJsonResponse(await historyResponsePromise)
      expect(
        isBusinessOk(detailPayload),
        `detail code ${detailPayload.code}, message=${detailPayload.msg || ''}`
      ).toBe(true)
      expect(
        isBusinessOk(historyPayload),
        `history code ${historyPayload.code}, message=${historyPayload.msg || ''}`
      ).toBe(true)
      expect(
        String(detailPayload.data?.certificateId),
        'detail must load the selected certificate'
      ).toBe(String(selected.certificateId))
      expect(Array.isArray(historyPayload.data), 'history payload must be an array').toBe(true)
      await expect(
        page.locator('[data-testid="registration-certificate-workflow-actions"]')
      ).toBeVisible()
      await expect(
        page.locator('[data-testid="registration-certificate-access-request-action"]')
      ).not.toBeVisible()
      await page.getByRole('tab', { name: '访问申请' }).click()
      const accessPanel = page.locator(
        '[data-testid="registration-certificate-access-request-action"]'
      )
      await expect(accessPanel).toBeVisible()
      const registrationFileId = detailPayload.data?.registrationFileId
      const projectCodeId = detailPayload.data?.projectCodeId
      const detailUrl = page.url()
      expect(
        config.runKey,
        'REG_CERT_E2E_RUN_KEY must be explicit for task-owned write data'
      ).toMatch(/^[A-Za-z0-9][A-Za-z0-9._-]{2,80}$/)
      expect(
        registrationFileId,
        'B-TEST fixture must expose a formal registration business file'
      ).toBeTruthy()

      const submitAccessRequest = async (requestType) => {
        await page.goto(detailUrl, { waitUntil: 'commit' })
        await expect(
          page.locator('[data-testid="registration-certificate-detail-page"]')
        ).toBeVisible({ timeout: 60000 })
        await page.getByRole('tab', { name: '访问申请' }).click()
        const accessPanel = page.locator(
          '[data-testid="registration-certificate-access-request-action"]'
        )
        await expect(accessPanel).toBeVisible({ timeout: 60000 })
        const radioName = requestType === 'DOWNLOAD_FILE' ? '下载文件' : '查看旧证'
        await accessPanel.getByText(radioName, { exact: true }).click()
        if (requestType === 'DOWNLOAD_FILE') {
          expect(
            projectCodeId,
            'download request requires the formal project code returned by detail'
          ).toBeTruthy()
        }
        const accessResponsePromise = page.waitForResponse(
          (response) =>
            response.url().includes('/admin-api/dcc/registration-certificates/access-requests') &&
            response.request().method() === 'POST',
          { timeout: 60000 }
        )
        await accessPanel.getByRole('button', { name: '提交访问申请' }).click()
        const accessResponse = await accessResponsePromise
        const accessPayload = await readJsonResponse(accessResponse)
        expect(
          isBusinessOk(accessPayload),
          `access request code ${accessPayload.code}, message=${accessPayload.msg || ''}`
        ).toBe(true)
        expect(accessPayload.data, 'access request must return a stable request id').toBeTruthy()
        const generatedKey = accessResponse.request().headers()['idempotency-key']
        expect(generatedKey, 'the page must generate Idempotency-Key automatically').toMatch(
          /^DCC-REG-CERT-ACCESS_SUBMIT-/
        )
        await expect(page.locator('.el-alert--success:visible')).toContainText('访问申请已提交', {
          timeout: 60000
        })
        const statusData = await readAccessStatus(accessPayload.data)
        expect(
          statusData?.bpmProcessInstanceId,
          'access request must expose its Native BPM process instance'
        ).toBeTruthy()
        return {
          requestId: accessPayload.data,
          requestType,
          idempotencyKey: generatedKey,
          bpmProcessInstanceId: statusData.bpmProcessInstanceId
        }
      }

      const approveAccessRequest = async (request) => {
        expect(
          config.reviewerUsername,
          'REG_CERT_E2E_REVIEWER_USERNAME must identify the approved Native BPM candidate'
        ).toBeTruthy()
        const reviewerContext = await browser.newContext()
        const reviewerPage = await reviewerContext.newPage()
        try {
          await login(reviewerPage, {
            username: config.reviewerUsername,
            password: config.reviewerPassword
          })
          const approvalKeyword = encodeURIComponent(String(request.bpmProcessInstanceId))
          const taskPageResponsePromise = reviewerPage.waitForResponse(
            (response) =>
              response.url().includes('/admin-api/approval-center/tasks/page') &&
              response.url().includes(`keyword=${approvalKeyword}`) &&
              response.request().method() === 'GET',
            { timeout: 60000 }
          )
          await reviewerPage.goto(
            `${config.baseUrl}/approval-center/todo?keyword=${approvalKeyword}`,
            { waitUntil: 'commit' }
          )
          const taskPagePayload = await readJsonResponse(await taskPageResponsePromise)
          expect(
            isBusinessOk(taskPagePayload),
            `approval task page code ${taskPagePayload.code}`
          ).toBe(true)
          const tasks = Array.isArray(taskPagePayload.data?.list) ? taskPagePayload.data.list : []
          const businessKey = `DCC_REGISTRATION_CERTIFICATE_ACCESS_REQUEST:${request.requestId}`
          const approvalTaskIndex = tasks.findIndex(
            (task) =>
              task.businessKey === businessKey ||
              String(task.sourceTaskId || '') === String(request.requestId) ||
              String(task.processInstanceId || '') === String(request.bpmProcessInstanceId)
          )
          const approvalTask = approvalTaskIndex >= 0 ? tasks[approvalTaskIndex] : undefined
          expect(
            approvalTask,
            `Native BPM task ${businessKey} must be visible to the logged-in reviewer`
          ).toBeTruthy()
          evidence.approvalTasks = evidence.approvalTasks || []
          evidence.approvalTasks.push({
            requestId: request.requestId,
            requestType: request.requestType,
            reviewerUsername: config.reviewerUsername,
            id: approvalTask.id,
            sourceTaskType: approvalTask.sourceTaskType,
            sourceTaskId: approvalTask.sourceTaskId,
            businessKey: approvalTask.businessKey,
            processInstanceId: approvalTask.processInstanceId,
            currentNodeName: approvalTask.currentNodeName,
            availableActions: approvalTask.availableActions
          })
          const taskRow = reviewerPage
            .locator('.approval-center__table .el-table__row')
            .nth(approvalTaskIndex)
          await expect(
            taskRow,
            'Native BPM task row must render in the real approval center table'
          ).toBeVisible({ timeout: 60000 })
          await expect(
            taskRow,
            'Native BPM task row must expose a real review action'
          ).toContainText(/审核|审批/, { timeout: 60000 })
          await taskRow
            .getByRole('button', { name: /审核|审批/ })
            .first()
            .click()
          const reviewDialog = reviewerPage.locator('.approval-center__review-dialog:visible')
          await expect(reviewDialog).toBeVisible({ timeout: 30000 })
          await reviewDialog.locator('input[type="password"]').fill(config.reviewerPassword)
          const reviewResponsePromise = reviewerPage.waitForResponse(
            (response) =>
              response.url().includes('/admin-api/approval-center/tasks/review') &&
              response.request().method() === 'POST',
            { timeout: 60000 }
          )
          await reviewDialog.getByRole('button', { name: '确认审核' }).click()
          const reviewPayload = await readJsonResponse(await reviewResponsePromise)
          expect(
            isBusinessOk(reviewPayload),
            `approval review code ${reviewPayload.code}, message=${reviewPayload.msg || ''}`
          ).toBe(true)
          expect(
            reviewPayload.data,
            'approval review must return a successful backend result'
          ).toBe(true)
          evidence.approvals = evidence.approvals || []
          evidence.approvals.push({
            requestId: request.requestId,
            reviewerUsername: config.reviewerUsername,
            result: 'APPROVE',
            code: reviewPayload.code
          })
        } finally {
          await reviewerContext.close()
        }
      }

      const readAccessStatus = async (requestId) => {
        await page.getByRole('tab', { name: '审批结果' }).click()
        const resultPanel = page.locator(
          '[data-testid="registration-certificate-approval-result-action"]'
        )
        const statusResponsePromise = page.waitForResponse(
          (response) =>
            response
              .url()
              .includes(`/admin-api/dcc/registration-certificates/access-requests/${requestId}`) &&
            response.request().method() === 'GET',
          { timeout: 60000 }
        )
        await resultPanel.getByRole('button', { name: '刷新申请状态' }).click()
        const statusPayload = await readJsonResponse(await statusResponsePromise)
        expect(
          isBusinessOk(statusPayload),
          `access status code ${statusPayload.code}, message=${statusPayload.msg || ''}`
        ).toBe(true)
        expect(
          statusPayload.data?.requestId,
          'status must identify the requested access record'
        ).toBe(requestId)
        return statusPayload.data
      }

      const revokeGrantFromCurrentPage = async (requestId, grantId) => {
        await page
          .locator('[data-testid="registration-certificate-approval-result-action"]')
          .getByRole('textbox', { name: '撤回或撤销原因' })
          .fill(`REGCERT-E2E-${config.runKey}-REVOKE`)
        const revokeResponsePromise = page.waitForResponse(
          (response) =>
            response
              .url()
              .includes(`/admin-api/dcc/registration-certificates/grants/${grantId}/revoke`) &&
            response.request().method() === 'POST',
          { timeout: 60000 }
        )
        await page
          .locator('[data-testid="registration-certificate-approval-result-action"]')
          .getByRole('button', { name: '撤销授权' })
          .click()
        const revokePayload = await readJsonResponse(await revokeResponsePromise)
        expect(
          isBusinessOk(revokePayload),
          `revoke grant code ${revokePayload.code}, message=${revokePayload.msg || ''}`
        ).toBe(true)
        evidence.revocation = {
          actor: config.username,
          grantId,
          requestId,
          code: revokePayload.code
        }
      }

      if (config.requireWriteFixture) {
        const oldRequest = await submitAccessRequest('VIEW_OLD_CERTIFICATE')
        evidence.accessRequests = [oldRequest]
        if (config.requireApprovalFlow) {
          await approveAccessRequest(oldRequest)
          const oldStatus = await readAccessStatus(oldRequest.requestId)
          evidence.oldViewStatus = {
            requestStatus: oldStatus.requestStatus,
            grants: oldStatus.grants
          }
          expect(oldStatus.requestStatus).toBe('APPROVED')
          expect(
            oldStatus.grants.some(
              (grant) => grant.grantType === 'VIEW_OLD_CERTIFICATE' && grant.status === 'ACTIVE'
            )
          ).toBe(true)
          const oldViewGrant = oldStatus.grants.find(
            (grant) => grant.grantType === 'VIEW_OLD_CERTIFICATE' && grant.status === 'ACTIVE'
          )
          expect(
            oldViewGrant,
            'old certificate view approval must create an active grant that can be revoked'
          ).toBeTruthy()
          await revokeGrantFromCurrentPage(oldRequest.requestId, oldViewGrant.grantId)
          const revokedOldStatus = await readAccessStatus(oldRequest.requestId)
          evidence.revokedGrantStatus = revokedOldStatus.grants.find(
            (grant) => String(grant.grantId) === String(oldViewGrant.grantId)
          )
          expect(evidence.revokedGrantStatus?.status).toBe('REVOKED')
        }
        const downloadRequest = await submitAccessRequest('DOWNLOAD_FILE')
        evidence.accessRequests.push(downloadRequest)
        if (config.requireApprovalFlow) {
          await approveAccessRequest(downloadRequest)
          const downloadStatus = await readAccessStatus(downloadRequest.requestId)
          evidence.downloadStatus = {
            requestStatus: downloadStatus.requestStatus,
            grants: downloadStatus.grants
          }
          expect(downloadStatus.requestStatus).toBe('APPROVED')
          const downloadGrant = downloadStatus.grants.find(
            (grant) =>
              grant.grantType === 'DOWNLOAD' &&
              grant.status === 'ACTIVE' &&
              String(grant.businessFileId) === String(registrationFileId)
          )
          expect(
            downloadGrant,
            'approved download request must expose one active grant for the formal business file'
          ).toBeTruthy()
          const downloadResponsePromise = page.waitForResponse(
            (response) =>
              response
                .url()
                .includes(
                  `/admin-api/dcc/registration-certificates/files/${registrationFileId}/download`
                ) && response.request().method() === 'GET',
            { timeout: 60000 }
          )
          await page
            .locator('[data-testid="registration-certificate-approval-result-action"]')
            .getByRole('button', { name: '下载' })
            .click()
          const downloadResponse = await downloadResponsePromise
          const generatedDownloadKey = downloadResponse.request().headers()[
            'x-dcc-download-attempt-key'
          ]
          expect(
            generatedDownloadKey,
            'the page must generate download Idempotency-Key automatically'
          ).toMatch(/^DCC-REG-CERT-DOWNLOAD_GRANT-/)
          const downloadHeaders = await downloadResponse.allHeaders()
          const downloadContentDisposition = downloadHeaders['content-disposition']
          if (!downloadContentDisposition) {
            const bodyText = (await downloadResponse.body()).toString('utf8')
            throw new Error(
              `download response was not an attachment: HTTP ${downloadResponse.status()}, content-type=${downloadHeaders['content-type'] || ''}, body=${bodyText}`
            )
          }
          expect(
            downloadResponse.status(),
            `download HTTP status ${downloadResponse.status()}`
          ).toBe(200)
          expect(
            downloadContentDisposition,
            'download must return an attachment disposition'
          ).toMatch(/attachment/i)
          evidence.download = {
            status: downloadResponse.status(),
            contentDisposition: downloadContentDisposition
          }
        }
      }
      await page.getByRole('tab', { name: '审批结果' }).click()
      await expect(
        page.locator('[data-testid="registration-certificate-approval-result-action"]')
      ).toContainText('BPM Native')

      if (config.requireApprovalFlow) {
        expect(evidence.approvals).toHaveLength(2)
      }

      const configResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/dcc/registration-certificates/reminder-config') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/user/profile?tab=config&config=registrationCertificate`, {
        waitUntil: 'commit'
      })
      await expect(page.locator('[data-testid="registration-certificate-config"]')).toBeVisible({
        timeout: 60000
      })
      const configPayload = await readJsonResponse(await configResponsePromise)
      expect(
        isBusinessOk(configPayload),
        `reminder config code ${configPayload.code}, message=${configPayload.msg || ''}`
      ).toBe(true)

      if (config.requireWriteFixture) {
        expect(
          evidence.writeRequests,
          'real write path must submit access requests and no hidden registration writes'
        ).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              method: 'POST',
              url: expect.stringContaining(
                '/admin-api/dcc/registration-certificates/access-requests'
              )
            })
          ])
        )
      } else {
        expect(
          evidence.writeRequests,
          'read/config diagnostic path must not send registration-certificate write requests'
        ).toEqual([])
      }

      const avatarFailures = evidence.failedResponses.filter(
        (failure) =>
          failure.method === 'GET' &&
          failure.status === 502 &&
          /^\/user\/avatar\//.test(failure.path)
      )
      const unexpectedFailedResponses = evidence.failedResponses.filter(
        (failure) => !avatarFailures.includes(failure)
      )
      const unexplainedConsoleErrors = evidence.consoleErrors.filter(
        (message) =>
          !(
            message ===
              'Failed to load resource: the server responded with a status of 502 (Bad Gateway)' &&
            avatarFailures.length > 0
          )
      )
      evidence.ignoredAssetFailures = avatarFailures
      evidence.unexpectedFailedResponses = unexpectedFailedResponses
      evidence.unexplainedConsoleErrors = unexplainedConsoleErrors
      expect(evidence.pageErrors, 'real page must not emit page errors').toEqual([])
      expect(
        unexpectedFailedResponses,
        'registration flow must not emit unexpected failed responses'
      ).toEqual([])
      expect(
        unexplainedConsoleErrors,
        'real page must not emit unexplained console errors'
      ).toEqual([])

      evidence.status = 'PASS'
      writeResult(evidence)
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error.stack || error.message
      writeResult(evidence)
      throw error
    }
  })
})
