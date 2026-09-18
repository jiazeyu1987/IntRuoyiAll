# DCC-STATIC-027 元数据身份同步修复

## Task Goal

修复 `docs/bugs/20260912-dcc-90-step-static-audit.md` 中 DCC-STATIC-027：正式文件基础信息修改文件编号、项目或分类后，Master 权威逻辑身份必须同步，冲突检查必须使用新逻辑键，读取当前版本时必须拒绝 Master 与 ACTIVE 文件身份不一致，确保 OLD 不会返回 NEW。

## Milestones

- [x] 读取项目规则、缺陷条目和 bug-regression-fix-loop 技能要求
- [x] 记录 BDD，并补充可失败的定向回归测试
- [x] 最小化修复元数据更新与读取身份一致性逻辑
- [x] 运行定向单元/静态合同或编译验证
- [x] 更新验证报告与收尾状态

## Expected Verification

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest test`
- `git diff --check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-027-metadata-identity-sync\verification-report.md`
- 本地验证后按用户后续明确指令融合进 E:\IntRuoyi 的 int_main；不执行 E2E，不启动或重启服务，不写数据库，不操作远程，不推送 Git。

## Design Constraints Check

- 遵守无 fallback、无静默降级、无 mock 成功原则。
- 修改范围仅限 DCC-STATIC-027 的后端静态逻辑、定向测试和任务证据。
- 基础信息修改若形成新逻辑身份，必须在同一事务内校验新逻辑键唯一性，并同步 Master 的 `dccProjectCodeId`、`fileTypeTaxonomyLeafId`、`normalizedFileNumber`。
- 当前版本读取必须复核 Master 与 ACTIVE 文件身份一致；不一致时失败，不返回错误匹配结果。

## Current Status

completed

- 实现、RED/GREEN、bug-regression evidence validator、cleanup preview/apply 均已通过。
- 已按 2026-09-13 后续用户指令融合进本地 `int_main`，代码提交 `38ddcc335`；未推送 `origin`。
