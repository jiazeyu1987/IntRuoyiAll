# Task: 生产工单临时冻结真实数据 E2E 补强

## Goal

为“生产工单临时冻结”能力补充真实数据 Playwright E2E 用例，覆盖生产工单页开关展示、生产排产页冻结工单范围排除，以及受控的临时冻结开关往返验证脚本。

## Scope

- 检查同仓库上一条前端任务状态，确认没有未闭环前序任务阻塞本次 E2E 补强。
- 创建当前任务文档、执行日志、前端证据文件和脚本目录。
- 新增至少 2 个默认可执行的真实数据只读 E2E 用例。
- 新增 1 个显式受控的破坏性 E2E 脚本，仅在操作者主动设置允许开关时才执行真实开关往返。
- 不修改后端契约，不引入 mock 数据，不新增测试专用前端控件。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-category-governance-split/task.md`
- Status before this task: completed.
- Related feature baseline: root task `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260515-pro-work-order-temporary-freeze\task.md` is completed.
- Impact: 当前前端仓库没有未闭环的上一条任务阻塞；临时冻结功能已落地，可以直接补真实数据 E2E。

## Milestones

- [x] M1: 检查前序任务状态并创建当前任务目录、文档和证据文件。
- [x] M2: 新增真实数据只读 E2E 用例，覆盖生产工单页和生产排产页。
- [x] M3: 新增受控破坏性 E2E 脚本，覆盖临时冻结开关往返。
- [x] M4: 运行安全用例、记录 GREEN 证据，并提交当前任务相关改动。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-smoke run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-smoke.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-task-scope run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-task-scope.mjs`
- 受控脚本 `exercise-temp-freeze-roundtrip.mjs` 仅在显式设置允许开关时运行，否则 fail fast 提示风险与前置条件。

## Current Status

Completed. 已新增 3 个 Playwright 脚本，完成 2 个只读真实数据用例的 GREEN 验证，并交付 1 个默认受控拒绝执行的破坏性往返脚本。

## Blocker And Impact

- Blocker: none for the current safe E2E slice.
- Impact:
  - 临时冻结功能现在有可复用的任务级真实数据 E2E 用例资产。
  - 破坏性往返脚本默认拒绝执行，需显式设置 `ALLOW_DESTRUCTIVE_TEMP_FREEZE_E2E=1` 后再运行。

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-smoke run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-smoke.mjs`
  - PASS，验证了生产工单页 `临时冻结` 开关展示与 `temporary-freeze-status` 真实接口。
- `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-task-scope run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-task-scope.mjs`
  - PASS，验证了生产排产页工单分页请求携带 `temporaryFrozen=false`。
- `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-roundtrip run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\exercise-temp-freeze-roundtrip.mjs`
  - PASS（安全拒绝），在未设置 `ALLOW_DESTRUCTIVE_TEMP_FREEZE_E2E=1` 时 fail fast 拒绝执行，没有修改真实排产数据。
