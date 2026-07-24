# 任务：SRM NAS定位通配搜索真实浏览器验证脚本（前端）

- Task ID: `20260701-srm-nas-locator-wildcard-search-error`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

为 `SRM -> NAS定位` 通配搜索系统异常修复补一条可复用的真实浏览器验证脚本，确保本机前端可以真实登录并执行 `*MO13*.pdf` 查询，直接读取页面请求结果与 toast 状态，作为后端 SQL 合同修复后的回归验证入口。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-local-admin-api-48081-connection-refused\task.md`
- 状态：`completed`
- 处理说明：上一任务已确认本机 48081 拒连并非前端配置漂移，不阻塞本轮补真实浏览器验证脚本。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实浏览器验证前先按当前登录页结构完成登录前置，不走接口旁路。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。脚本直接走真实登录页和真实页面请求，不通过 mock 返回或接口替身掩盖异常。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 真实浏览器查询 *MO13*.pdf 时直接观察接口结果 -> Given 本机前后端运行正常且账号可登录 / When 脚本在 NAS定位 页面输入 *MO13*.pdf 并点击搜索 / Then 应记录真实接口 payload、页面 toast 与 console/page error，用于判断修复前后的真实表现。`

## Milestones

1. M1：建立前端任务台账并锁定真实验证入口。`completed`
2. M2：补浏览器级验证脚本。`completed`
3. M3：以该脚本作为后端修复回归入口并回填结果。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-wildcard-search-debug.e2e.js`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-wildcard-search-debug.e2e.js` -> `PASS`
- 结论：
  - 脚本可真实登录 `/srm/nas-locator`，发起 `*MO13*.pdf` 查询并抓取 `/admin-api/srm/nas-locator/page` 返回。
  - 修复后验证结果可返回 `code=0`，且页面不再弹出“系统异常”。

## Current Blockers

- 无。
