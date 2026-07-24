# 执行日志：发布后智能排产 smoke 审批人身份解析门禁修复

- `BDD: 第三方报工导入审批人支持唯一 username 解析 -> Given 导入文件的工段长字段填写的是启用用户的唯一 username / When 系统在归属阶段解析审批人 / Then 必须把审批责任绑定到该 username 对应的唯一用户，后续审批人登录与 approveUserId 保持一致。`
- `BDD: 唯一昵称仍按原契约可解析 -> Given 导入文件的工段长字段填写的是唯一 nickname / When 系统解析审批人 / Then 必须继续按原有昵称契约命中该用户，不回退行为。`
- `BDD: 重复昵称仍然 fail fast -> Given 导入文件的工段长字段填写的是重复 nickname，且没有与之同名的唯一 username / When 系统解析审批人 / Then 必须明确返回“匹配到多名用户”，不得静默任选其一或吞错继续。`
- `BDD: 发布后 smoke 审批人标识绑定当前 supervisor 账号 -> Given 维护发布链为智能排产 smoke 配置 supervisor 账号 / When 生成测试服运行时 .env / Then 第三方报工导入审批人字段必须显式绑定该 supervisor 的唯一标识，避免依赖历史昵称默认值。`
- `DIAG: remote-env-probe -> PASS，测试服 /opt/intruoyi/runtime/.env 当前包含 MES_SMOKE_SUPERVISOR_USERNAME=messmokesupervisor，未显式设置 MES_SMOKE_FEEDBACK_APPROVER_NAME，说明 smoke 运行态会落到脚本默认昵称。`
- `DIAG: remote-user-identity-probe -> PASS，测试服 tenant_id=1 下启用用户 mes_smoke_supervisor(id=910253) 与 messmokesupervisor(id=910260) 共用 nickname=eDHR矩阵-审批人，且角色同为 报工冒烟审批员；当前重复昵称会稳定触发 approveUserId 不唯一。`
- `RED: ssh root@172.30.30.58 "cat /opt/intruoyi/runtime/smoke/yudao-ui-admin-vue3/output/artifacts/smoke-report.json"` -> FAIL，最新发布后真实 smoke 在 /admin-api/mes/pro/feedback/import-record/attribute 返回 `第三方报工导管报工第 2 行工段长匹配到多名用户：eDHR矩阵-审批人`，证明默认审批人昵称在测试服当前真实数据下不唯一。`
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProFeedbackImportRecordServiceImplTest" test` -> PASS，6 passed；新增回归覆盖 supervisor username 直绑审批人、唯一 nickname 兼容与重复 nickname 继续 fail-fast。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\scripts\validate-maintenance-publish-contract.cjs` -> PASS，维护发布脚本已显式输出 MES_SMOKE_FEEDBACK_APPROVER_NAME=$effectiveMesSmokeFeedbackApproverName，且该值绑定 supervisor username。`
- `DIAG: remote-config-drift-after-release -> PASS，测试服最新 release 的 /opt/intruoyi/runtime/.env 已包含 MES_SMOKE_FEEDBACK_APPROVER_NAME=messmokesupervisor，但 smoke 产物 config.json 仍显示 feedbackApproverName=eDHR矩阵-审批人，说明问题不在 .env 写出，而在 scheduler smoke 容器环境变量透传白名单缺少该字段。`
- `GREEN: python -X utf8 -m pytest script\tests\test_scheduler_smoke_release_contract.py -q` -> PASS，3 passed；业务仓发布契约现已覆盖 docker-compose、远端 .env 与 scheduler smoke npm wrapper 对 MES_SMOKE_FEEDBACK_APPROVER_NAME 的完整透传。`
- `GREEN: release-20260619-2230-role-e2e-gate-feedback-approver-wrapper -> PASS，测试服 /opt/intruoyi/runtime/.env 为 IMAGE_TAG=release-20260619-2230-role-e2e-gate-feedback-approver-wrapper，远端 smoke config.json 显示 feedbackApproverName=messmokesupervisor，smoke-report.json 为 PASS，正式报工 approveUserId=910260。`
