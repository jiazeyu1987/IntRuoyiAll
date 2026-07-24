# 任务：继续修复公司页面进入时 narration revision mismatch

## Goal

- 修复进入公司页面时仍出现 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch` 的问题。
- 区分这是读取链路遗漏、旧坏数据残留，还是未部署最新修复导致，并给出最小闭环修复。
- 保持 fail-fast 语义，不引入 fallback、静默跳过或默认成功。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\company\CompanyWorkbench.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-page-revision-mismatch-followup\**`

## Non-Scope

- 不混入 `infra runtime control` 未完成任务的脚本、SQL、控制器或测试改动。
- 不修改 Website 前端视觉样式。
- 不用 mock 成功、绕过校验或放宽 revision 一致性要求来掩盖真实坏数据。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-save-revision-mismatch-fix\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact on this task: 上一任务已修复“保存公司信息”时的新 revision 迁移逻辑；当前 follow-up 需要继续排查进入页面时仍报错的剩余链路或历史数据问题。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [x] M2：确认公司页面真实调用链并记录报错接口。
- [x] M3：先写 RED 复现或记录真实坏数据前置条件。
- [x] M4：最小修复剩余链路或历史数据对齐逻辑并验证 GREEN。
- [x] M5：回写证据、执行 cleanup 预览，并评估是否可安全提交当前任务文件。

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-page-revision-mismatch-followup\execution-log.md`
- `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "... live company / preview / mapped product preview alignment queries ..."`

## Current Status

Completed on 2026-05-23.

## Blockers

- 暂无；先前因用户切换到更高优先级 showroom 缺陷而暂停，现已恢复并完成本次本地 live 数据修复。

## Current Findings

- 当前“进入公司页面”对应的真实失败接口不是 `GET /admin-api/showroom/company/current`，而是 `GET /showroom/display/website-config`。
- `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` 在修复前就是正常的；失败点只出现在严格校验 `source_revision_id` 的 release / website-config 聚合链路。
- 本地 MySQL 中主公司 current revision 已是 `8`，但 latest live 公司中英文讲解与 company preview 仍停留在 `source_revision_id = 7`。
- 已新增并执行本地修复脚本 `doc/tasks/20260523-showroom-company-page-revision-mismatch-followup/align-local-live-revisions.sql`，将当前 website-config 依赖的 live company / preview / hall-mapped product preview 对齐到运行时代码读取规则。
- 修复后 `GET /showroom/display/website-config` 已恢复返回 `code=0`。

## Final Verification Result

- PASS: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config`
- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-page-revision-mismatch-followup\execution-log.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260523-showroom-company-page-revision-mismatch-followup --mode preview`

## Cleanup Keep

- `doc/tasks/20260523-showroom-company-page-revision-mismatch-followup/task.md`
- `doc/tasks/20260523-showroom-company-page-revision-mismatch-followup/execution-log.md`
