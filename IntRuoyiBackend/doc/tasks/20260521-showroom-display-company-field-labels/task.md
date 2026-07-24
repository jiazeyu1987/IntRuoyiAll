# 任务：统一展厅前台公司 display 字段标签

## Goal

修复展厅前台公司介绍链路中公司公开字段标签与公司工作台合同不一致的问题，确保 `core_manufacturing_capability`、`honors_awards` 等字段在 `GET /showroom/display/company` 与真实前台页面上使用统一的公司标签，不通过前端临时映射或 fallback 掩盖后端契约偏差。

## Scope

- 复现并记录真实前台 `showroom/company-intro` 公司字段展示结果。
- 为 `GET /showroom/display/company` 补一条 RED 回归测试，锁定公司字段标签必须与公司工作台合同一致。
- 在 `ShowroomFieldDisplaySupport` / `ShowroomApiRuntime` 的最小范围内修复公司字段标签映射。
- 运行定向后端测试，并回写任务与执行日志。

## Non-Scope

- 不修改前端页面布局、前端文案临时映射或展示样式。
- 不改动产品、展厅、审批、指派、讲解等无关 display 契约。
- 不调整数据库内容值，只修复标签映射与输出契约。
- 不引入 fallback、兼容分支、mock 数据或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-company-hall-bilingual-fields\task.md`
- Status before this task: `Completed`
- Impact: 公司/展厅前台输出链路已存在，本次可继续在 display 契约层修复公司字段标签一致性。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 存在与本任务无关的脚本与 task 文档未提交改动。
- Impact: 本任务必须严格限定在 `showroom` 模块与本任务文档范围内，避免覆盖或提交无关在途工作。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [x] M2: 记录 BDD 场景并先补 RED 回归测试。
- [x] M3: 最小修复公司 display 字段标签映射。
- [x] M4: 运行定向测试并记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并在边界允许时准备提交。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#companyDisplayShouldUseUnifiedCompanyFieldLabels" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-display-company-field-labels --mode preview`

## Current Status

Completed on 2026-05-21.

已完成真实前台链路定位、后端 RED/GREEN、运行时重启与前台实测。根因是 `ShowroomFieldDisplaySupport` 的公司字段标签映射仍保留旧文案 `股权信息 / 荣誉奖项`，导致前台公司介绍页与公司工作台合同不一致；现已统一为 `上市信息 / 荣誉资质`。

## Blockers And Impact

- Blocker: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java` 当前存在与本任务无关的在途改动，新增了未收口的 `ProductPublishReqVO` 相关断言，导致模块级 `testCompile` 现阶段报编译错误。
- Impact: 本次保留的独立回归测试文件已单独编译通过；标准 `mvn test` 级别的再次全量回归需要等该共享测试文件的无关改动先收口。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#companyDisplayShouldUseUnifiedCompanyFieldLabels" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: 独立回归测试文件 `yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\foundation\ShowroomCompanyFieldLabelContractTest.java` 已单独编译通过
- PASS: 真实前台 `http://127.0.0.1:8081/showroom/company-intro` 经 Playwright 验证后显示标签 `发展历程 / 园区介绍 / 核心制造能力 / 荣誉资质`，且不再出现旧标签 `荣誉奖项`
