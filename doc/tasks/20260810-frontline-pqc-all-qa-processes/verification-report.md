# Verification Report

## Summary

- 结论：当前源码逻辑不是直接按订单产品取一个 QA 工序；一线 PQC 工序接口按活跃订单、工艺路线、路线绑定项目代码和已发布 QA 规程解析工序。
- 运行态现象：目标本机活跃订单接口只返回 1 个工序，因为当前数据库同路线版本只存在 1 条 `MES_QA / PUBLISHED` QA 规程。
- 阻塞项：未执行数据写入或重新发布 QA 规程；缺少当前截图中多工序 QA 已发布到目标路线版本的正式数据前置。

## Evidence

- 只读接口：`/mes/pro/feedback/frontline/device-account/pqc/active-order/processes` 对目标活跃订单返回 `count=1`，工序为“清洗工序”。
- 只读数据库核对：目标路线版本下 `mes_qa_inspection_regulation` 仅有 1 条 `owner_module='MES_QA' AND lifecycle_status='PUBLISHED'` 记录，`current_version_id=54`，对应“清洗工序”。
- 目标 JUnit：`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，39 tests, 0 failures, 0 errors。

## Blockers

- 当前运行数据没有发布截图中全部 QA 工序对应的逐工序规程；在不写入/不重新发布 QA 的前提下，页面无法展示不存在于发布表中的工序。
