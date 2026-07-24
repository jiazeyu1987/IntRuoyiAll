# 执行日志：电子签名 portal 路径改为子页签

INFO: skill -> 使用 `backend-api-delivery`，并读取 backend evidence contract。

BDD: portal 返回文件签名子页签 -> Given DCC adapter 生成电子签名入口 / When 查询 primary route / Then 返回 /signature-governance/file-signatures。

BDD: portal 返回用户授权子页签 -> Given DCC adapter 生成授权入口 / When 查询 secondary route / Then 返回 /signature-governance/authorizations。

BDD: portal 返回批记录签名子页签 -> Given eDHR adapter 生成签名入口 / When 查询 primary route / Then 返回 /signature-governance/batch-signatures。

RED: mvn -pl yudao-module-dcc "-Dtest=DccSignatureGovernancePortalAdapterTest,SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test -> FAIL，DCC adapter 仍返回模块名 `DCC 电子签名` 和旧路径语义。

RED: mvn -pl yudao-module-mes -Dtest=MesEdhrSignatureGovernancePortalAdapterTest test -> FAIL，eDHR adapter primary route 仍为 `/signature-governance?tab=batch-signatures`。

GREEN: mvn -pl yudao-module-dcc "-Dtest=DccSignatureGovernancePortalAdapterTest,SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test -> PASS，16 tests。

GREEN: mvn -pl yudao-module-mes "-Dtest=MesEdhrSignatureGovernancePortalAdapterTest" test -> PASS，1 test。

GREEN: experience-preflight -> PASS，本次仅重启本机 `int_main` 后端以加载当前源码，目标健康检查为 `http://127.0.0.1:48081/actuator/health`；不操作测试服、备份服或正式服。

GREEN: restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> PASS，本机后端已按当前源码启动。

GREEN: curl http://127.0.0.1:48081/actuator/health -> PASS，返回 `{"status":"UP"}`。

GREEN: task-closeout-cleanup --mode preview/apply -> PASS，无临时产物需要删除。
