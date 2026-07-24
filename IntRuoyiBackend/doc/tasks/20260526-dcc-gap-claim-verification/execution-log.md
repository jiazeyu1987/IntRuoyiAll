# Execution Log

BDD: gap claim verification -> Given 用户列出 R05/R06/R07/验证层缺口 / When reviewer 基于当前 `int_main` 代码、测试、日志和运行库逐条复核 / Then 每条缺口必须标记为属实、部分属实或不属实，并记录证据。

INFO: R05 frontend inspection -> `src/views/dcc/controlled-file/detail/index.vue` 和 `src/views/dcc/controlled-file/mine/index.vue` 仅发现主动撤回入口；未发现撤回完成后的“删除流程 / 重新提交”二选一入口。

INFO: R05 backend inspection -> `DccControlledFileWorkflowServiceImpl.withdrawControlledFile` 取消 BPM 流程并将 DCC 文件状态置为 `WITHDRAWN`；未发现 DCC 撤回态删除流程或重新提交状态流转。

INFO: R07 frontend inspection -> 外来文件评审相关隐藏路由指向现有受控文件上传页面，通过页面标题和按钮文案区分，并提交 `processType=EXTERNAL_REVIEW`。

INFO: R07 backend inspection -> 后端支持并持久化 `EXTERNAL_REVIEW`，但未发现独立外来评审字段、参与人、评审结论、节点模型或输出物流程。

GREEN: R06 runtime category query -> PASS, 本地运行库租户 `122` 查询到 `体系文件`、`技术文件-DHF`、`技术文件-DMR` 三个未删除类别。

INFO: verification evidence review -> Maven 全量测试未重跑；此前已在当前 `int_main` 集成后重跑 DCC Playwright 真实路径 E2E，结果 `11 passed in 251.80s`。

GREEN: independent verification report -> PASS, `verification-report.md` 已记录逐条结论与剩余风险。
