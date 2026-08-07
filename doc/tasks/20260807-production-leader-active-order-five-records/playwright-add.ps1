$ErrorActionPreference = 'Stop'

$playwrightCli = 'E:\Int\DevCache\npm-cache\_npx\31e32ef8478fbf80\node_modules\.bin\playwright-cli.ps1'
if (-not (Test-Path -LiteralPath $playwrightCli)) {
    throw "Playwright CLI is missing: $playwrightCli"
}

$code = @'
async (page) => {
const codes = [
  "CODX-AO5-20260807-01",
  "CODX-AO5-20260807-02",
  "CODX-AO5-20260807-03",
  "CODX-AO5-20260807-04",
  "CODX-AO5-20260807-05"
];
await page.getByRole("button", { name: "\u767b\u5f55" }).click();
await page.waitForURL((url) => !url.pathname.includes("/login"), { timeout: 60000 });
await page.goto("http://127.0.0.1:8081/mes/pro/process-pool/production-leader", { waitUntil: "domcontentloaded" });
await page.locator("[data-production-leader-workbench-page]").waitFor({ state: "visible", timeout: 60000 });
await page.locator(".el-tabs__item:visible").filter({ hasText: "\u6d3b\u8dc3\u8ba2\u5355\u6c60" }).first().click();
const section = page.locator("[data-team-leader-active-order-config]").first();
await section.waitFor({ state: "visible", timeout: 30000 });
const results = [];
for (const code of codes) {
  await section.getByRole("button", { name: "\u65b0\u589e\u6d3b\u8dc3\u8ba2\u5355" }).click();
  const dialog = page.locator("[data-team-leader-active-order-add-dialog]").first();
  await dialog.waitFor({ state: "visible", timeout: 30000 });
  const input = dialog.locator("[data-team-leader-active-order-work-order-code] input[role=combobox]").first();
  await input.waitFor({ state: "visible", timeout: 30000 });
  const candidateResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/mes/pro/process-pool/team-leader/active-order/candidates") && response.request().method() === "GET",
    { timeout: 60000 }
  );
  await input.click();
  await input.press("Control+A");
  await input.type(code, { delay: 20 });
  const candidateResponse = await candidateResponsePromise;
  const candidateBody = await candidateResponse.json();
  const candidates = Array.isArray(candidateBody.data) ? candidateBody.data : [];
  const candidate = candidates.find((item) => item.workOrderCode === code);
  if (!candidateResponse.ok() || ![0, 200].includes(candidateBody.code) || !candidate || candidate.eligible !== true) {
    throw new Error("Candidate " + code + " is not eligible: " + JSON.stringify({ http: candidateResponse.status(), body: candidateBody, candidate }));
  }
  const option = page.locator(".el-select-dropdown__item:visible").filter({ hasText: code }).first();
  await option.waitFor({ state: "visible", timeout: 30000 });
  await option.click();
  const addResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/mes/pro/process-pool/team-leader/active-order/add") && response.request().method() === "POST",
    { timeout: 60000 }
  );
  await dialog.getByRole("button", { name: "\u52a0\u5165\u6d3b\u8dc3\u8ba2\u5355" }).click();
  const addResponse = await addResponsePromise;
  const addBody = await addResponse.json();
  const payload = JSON.parse(addResponse.request().postData() || "{}");
  if (!addResponse.ok() || ![0, 200].includes(addBody.code) || Number(payload.workOrderId) !== Number(candidate.workOrderId)) {
    throw new Error("Add " + code + " failed: " + JSON.stringify({ http: addResponse.status(), body: addBody, payload, candidate }));
  }
  await dialog.waitFor({ state: "hidden", timeout: 30000 });
  results.push({ code, workOrderId: candidate.workOrderId, eligible: candidate.eligible, httpStatus: addResponse.status(), businessCode: addBody.code });
  await page.waitForTimeout(300);
}
await page.screenshot({ path: "E:/IntRuoyi/output/playwright/20260807-production-leader-active-order-five-records.png", fullPage: true });
console.log("AO5_RESULTS=" + JSON.stringify(results));
return results;
}
'@

& $playwrightCli --session ao5bulk run-code $code
if ($LASTEXITCODE -ne 0) {
    throw "Playwright CLI failed with exit code $LASTEXITCODE"
}
