# 一线 PQC 重复覆盖身份系统异常修复

## Task Goal

修复一线 PQC 加载活跃订单工序时，将跨生产工序或跨轮次的合法 PQC 任务误判为重复覆盖身份并返回系统异常的问题。

## Milestones

- [x] M1：从 int_main 后端日志定位异常栈和业务身份
- [x] M2：新增完整任务身份回归并取得 RED
- [x] M3：修复覆盖匹配身份
- [x] M4：执行定向后端回归与证据校验
- [x] M5：完成真实前端组合规程显示与通用任务提交验证
- [x] M6：完成任务收尾

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcTaskOverlayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 相关一线 PQC 上下文定向回归
- Bug regression evidence validator
- Backend API evidence validator
- 标准 `int_main` 后端重启与运行 Jar 来源核验
- Playwright 真实前端选择目标活跃订单，验证产品 QA 与通用规程流程内容同时可见

## Design Constraints Check

- PQC 任务身份必须包含 `activeOrderId + routeProcessId + processId + regulationVersionId + qaProcessId + qaItemCode + inspectionRuleKey + inspectionType + businessDate + shiftCode + roundNo`。
- 不按第一条任务、排序或默认生产工序消除歧义。
- 真正完整身份重复仍需 fail fast，不能静默去重。
- 不修改数据库 schema；用户已授权通过正式前端补齐共享路线生产工序配置并发布有效版本。
- 用户已明确要求真实 E2E；验收路径只允许通过前端操作并保持只读。
- 用户已在当前轮明确授权重启 int_main 后端。
- 不提交、不推送；用户本轮未授权 Git 操作。

## Current Status

completed

代码修复、定向回归、标准打包/重启、真实 Playwright E2E、任务产物清理和两条任务自有模拟订单的前端清理均已通过；未执行未授权的 Git 提交或推送。

## Cleanup Completed

- 已删除技能临时证据文件和 `output/playwright/20260912-frontline-pqc-common-flow/`；保留核心任务记录与正式回归测试。
- 已通过生产组长页面“清理测试单”清理模拟订单 `1009200079/1009200080`，只读数据库复核对应活跃订单和工单均为 `deleted=1`。
