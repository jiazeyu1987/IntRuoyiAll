# Verification Report

## Scope

验证范围为活跃订单工序提交详情页新增“生产用料清单”主 tab 的前端功能、数据来源约束和 worktree 真实页面路径。

## Results

- PASS：静态合同测试通过，确认页面使用 ERP 生产用料清单接口，按生产订单号查询，面板按 `sourceBillNo` 分组，不使用领料单、补料单或 BOM。
- PASS：前端类型检查通过。
- PASS：worktree 后端构建并启动成功。
- PASS：真实页面正向样本 `KDMO-309748-1416202028` 验证通过，生产用料清单接口返回 1 条，页面显示单据 `SIM-PML-150-20260821`。
- PASS：用户指定订单 `SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891` 经授权复制本地测试数据后验证通过，生产用料清单接口返回 11 条，页面显示单据 `SIM-PML-CODX-PQC-WO05-001`。

## Evidence files

- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-positive-result.json`
- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-positive.png`
- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-target-empty-result.json`
- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-target-empty.png`
- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-target-after-copy-result.json`
- `doc/tasks/20260908-production-material-list-tab/production-material-list-tab-real-target-after-copy.png`

## Final gate

代码功能、授权数据准备、目标订单真实页面验证均已通过。当前状态为 `ready_for_closeout`，尚未执行最终提交/推送/清理收尾。
