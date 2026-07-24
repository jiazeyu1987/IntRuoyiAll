# 任务：补齐展厅公司默认信息

## Goal

根据 `D:\Downloads\瑛泰子公司与产品介绍.txt` 的现有资料，整理并写入 `http://localhost:8081/showroom/company` 公司内容卡片的默认信息，补齐 `孵化平台`、`子公司概览`、`上市信息` 等字段，并让页面直接展示整理后的真实默认内容。

## Scope

- 读取源资料并提炼为适合公司内容卡片展示的默认文案。
- 通过真实前端路径 `/showroom/company` 更新现有公司内容。
- 用 Playwright 真实登录验证后台页面保存结果。
- 更新本任务文档、执行日志与 closeout preview 证据。

## Non-Scope

- 不改动展厅公司业务字段结构、接口契约或审批状态机。
- 不修改无关产品、展厅、审批、讲解等模块代码。
- 不捏造资料中不存在的上市进度、财务指标或监管披露信息。
- 不引入 fallback、mock 或静默降级文案。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-missing-manufacturing-honors\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓公司内容任务已完成字段显示修复，本次可在该基线上继续补齐默认内容。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 工作区存在与本任务无关的在途改动与其他 task 文档。
- Impact: 本任务只新增本任务目录下的文档/脚本，不覆盖其他改动。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [x] M2: 根据源资料整理目标字段文案并记录 BDD。
- [x] M3: 用真实页面验证当前内容尚未达到目标状态并记录 RED。
- [x] M4: 通过真实系统发布链路写入默认信息。
- [x] M5: 复核保存结果、更新任务文档并执行 closeout preview。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\verify-company-default-info.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-fill run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\fill-company-default-info.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\verify-company-default-info.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-default-info-fill --mode preview`

## Current Status

Completed on 2026-05-21.

已根据 `D:\Downloads\瑛泰子公司与产品介绍.txt` 整理七个公司字段默认文案，并写入当前 live 公司版本。前端 `http://localhost:8081/showroom/company` 已复核展示与编辑弹窗内容全部匹配目标文案。

## Blockers And Impact

- Blocker: none.
- Impact: 可继续后续展厅内容维护任务。

## Final Verification Result

- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\verify-company-default-info.mjs`，真实页面 `发展历程` 仍是旧文案 `瑛泰围绕介入医疗器械与高端制造协同发展，持续建设研发、生产与质量体系能力。`
- PASS: 使用测试租户 `测试租户 / aoteman / admin123` 登录本地后端后，显式 UTF-8 `HttpClient` 调用 `PUT http://127.0.0.1:48081/admin-api/showroom/company/publish` 成功返回 `revisionId=5 / revisionNo=5 / status=PUBLISHED`，七个公司字段已更新为目标默认文案。
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\verify-company-default-info.mjs`，后台公司工作台卡片与编辑弹窗均显示目标文案。
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-default-info-fill run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-default-info-fill\scripts\fill-company-default-info.mjs`，当前数据已是目标状态，脚本返回 `saved=false` 且页面复核通过，证明后续重复执行不会再无意义创建新版本。
