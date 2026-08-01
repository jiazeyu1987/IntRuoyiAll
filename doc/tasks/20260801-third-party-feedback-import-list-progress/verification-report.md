# Verification Report

## Summary

本次修复把李萍直报 Excel 的成功路径收敛到正式报工：匹配行先写导入审计记录，再创建 `MesProFeedbackDO`、关联 `sourceImportRecordId`、提交审批中，并由正式报工汇总重算排产进度。缺正式前置条件的行返回结构化跳过原因，不再写 `DIRECT_WORK_REPORT` 直接进度兜底。

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldCreateSubmittedFeedbackAndLinkImportRecordForMatchedRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=ThirdPartyFeedbackImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，25 tests, 0 failures, 17 skipped。
- `node tests/e2e/mes-direct-work-report-import-result-static.spec.js` -> PASS。
- `node tests/e2e/mes-direct-work-report-refresh-schedule-order-static.spec.js` -> PASS。

## Known Non-Blocking Result

- `node tests/e2e/mes-feedback-tracking-static.spec.js` -> FAIL，缺少静态 token `删除报工失败，请检查后端接口。`；该宽口径报工追踪合同不属于本次直接报工列表/进度修复范围，且相关前端文件已有并发/基线改动。

## Scope Notes

- 未新增或修改 SQL/迁移。
- 未执行真实 E2E 导入；原因是本轮未启动本地前后端，也未确认可写测试租户/账号。根据项目规则，该结果不作为页面真实路径通过声明。

## Test Release Progress

- r1-r8 均未生成可部署的完整权威 manifest 或未完成构建，均未部署到测试服务器。
- r8 已完成代码与镜像构建，但 Codex 版本摘要因 Windows PowerShell 5.1 无法传递末尾 stdin 标记 `-` 而退出 1；无 `manifest.json`，明确判定无效。
- 新增回归覆盖 PS1 shim 同时接收 `exec`、末尾 `-` 和 stdin；RED 为 `ExitCode=2`，改用并强制 PowerShell 7 `pwsh.exe` 后 GREEN。
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，104 passed。
- r9 已证明 Codex 修复生效：后端、前端、双镜像和 `manifest.json` 生成成功，双 source commit=`6b998cab2`、dirty=false、`summaryGenerator=codex`。
- r9 最终因未传正式 NAS JSON 配置而失败，未上传、未部署；下一有效候选必须使用新 releaseTag 并显式传入已校验的 `NasConfigPath`，禁止复用 r8/r9 或手工补写 manifest。
- r10 揭示本地失败包被误作 previous release；已新增并通过 NAS 基线选择与空 Git facts 两条回归，完整发布脚本测试更新为 106 passed。
