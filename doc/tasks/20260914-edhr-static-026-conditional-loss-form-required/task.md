# EDHR-STATIC-026 条件损耗表必填规则修复

## Task Goal

修复 EDHR-STATIC-026：配置为“有损耗才必填”的损耗/报废表，只在订单、工序或正式来源存在真实损耗/报废事实时，才作为最终放行必填证据；无损耗订单不得因条件表单未填写而阻断最终放行。

## Milestones

- [x] 读取仓库与 eDHR/批记录/表单必填/损耗/完工回填/最终放行相关规则。
- [x] 编写 BDD 场景和 RED 静态合同/定向测试。
- [x] 最小修改条件必填适用性判断，不改变无条件必填和真实损耗阻断规则。
- [x] 运行非 E2E 定向验证和静态逻辑检查。
- [x] 记录验证报告、收尾状态和建议缺陷总表更新内容。

## Expected Verification

- 静态合同或单元测试证明：`CONDITIONAL_REQUIRED` 损耗/报废表在无真实损耗/报废事实时不被 DHR 完整性检查要求填写。
- 静态合同或单元测试证明：同一表单在存在真实损耗/报废事实时仍为必填，缺少正式填写证据必须阻断。
- 静态合同或单元测试证明：无条件 `REQUIRED` 表单不受本修复放松。
- 禁止 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作；用户于 2026-09-14 授权提交并融合进 `int_main`。

## Current Status

completed

实现、非 E2E 定向验证、cleanup apply 和 `int_main` 融合均已完成。任务分支实现提交为 `7838ff738`；`int_main` 融合提交为 `ded9c4a51`。

## Design Constraints Check

- No fallback: 不把全部损耗表改成可选，不生成虚假损耗表，不用空成功掩盖缺失证据。
- Fail fast: 条件 JSON 无法识别或真实损耗事实存在但缺少正式表单/证据时，继续明确阻断。
- Source boundary: 批记录表单只取逐工序正式绑定；`formBindings` 和工序开始配置不得补齐。
- Scope: 只处理 EDHR-STATIC-026，不编辑共享缺陷总表，不处理 020/021/022/024/025 等其他缺陷。

## Cleanup Keep

- doc/tasks/20260914-edhr-static-026-conditional-loss-form-required/task.md
- doc/tasks/20260914-edhr-static-026-conditional-loss-form-required/execution-log.md
- doc/tasks/20260914-edhr-static-026-conditional-loss-form-required/verification-report.md
- doc/tasks/20260914-edhr-static-026-conditional-loss-form-required/bug-regression-evidence.md
