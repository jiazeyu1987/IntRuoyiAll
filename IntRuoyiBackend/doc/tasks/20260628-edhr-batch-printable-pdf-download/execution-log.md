# 执行日志：eDHR 批次打印版 PDF 后端实现

- `BDD: 归档生成写入打印快照 -> Given 批次已关闭且存在已审批表单执行记录 / When 生成最终归档 / Then source_manifest_json 写入带 schemaVersion 的打印快照，并包含正文表单快照与特殊节点附录摘要。`
- `BDD: 下载打印版 PDF 使用打印快照 -> Given 批次最新归档为新版打印快照 / When 下载归档 / Then 返回的 PDF 包含真实表单标题、填写值、签名单元格与备注，而不是 manifest 文本章节。`
- `BDD: 旧版归档直接阻塞 -> Given 某归档 source_manifest_json 不含新版 schemaVersion / When 下载或打印 / Then 后端抛出“请先重新生成最终归档后再下载打印版 PDF”。`
- `GREEN: previous-task-check -> PASS，上一后端任务 20260628-mes-work-order-clear-all-blockers 已完成。`
- `GREEN: experience-index-hit -> PASS，已命中并读取 powershell-memory / login-access 门禁。`
- `RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_manifestUsesPrintableSnapshotSchema+generateArchive_downloadPdfContainsQaReadableBatchSections+downloadArchive_legacyManifestFailsFast test -> FAIL，当前实现缺少 PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED，证明旧归档 fail fast 业务错误尚未落地。`
- `GREEN: experience-preflight -> PASS，真实下载/打印验收限定本机 http://localhost:8081 与测试租户，已按门禁完成 experience 命中检查，允许执行官方 login-preflight。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --target-text 下载打印版PDF --timeout 90000 -> PASS`
- `GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_manifestUsesPrintableSnapshotSchema+generateArchive_downloadPdfContainsQaReadableBatchSections+downloadArchive_legacyManifestFailsFast test -> PASS`
- `GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionArchiveControllerTest" test -> PASS`
