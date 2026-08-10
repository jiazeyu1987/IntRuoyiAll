# PQC 无设备检验项目提交修复

## Task Goal

修复一线 PQC 检验项目设备选择规则：QA 规程中 `equipmentRequired=false` 且无设备选项的检验项目，前端不显示/不强制设备选择，正式提交时前后端都不要求 `selectedEquipmentId` 与 `selectedEquipmentNumber`，后端保存明细时设备快照字段保持空值。

## Milestones

- [x] M1 复现并锁定红框 PQC 与 QA 规程产品/工序/项目映射事实
- [x] M2 新增 RED 回归测试，证明无设备检验项目当前被错误强制设备
- [x] M3 实现前端与后端最小正式规则修复
- [x] M4 运行 GREEN 与相邻回归验证
- [x] M5 更新验证报告与收尾状态

## Expected Verification

- 前端静态合同：无设备 PQC 项目使用后端 `equipmentRequired=false`，不显示设备必填，不提交必填设备字段。
- 后端单元回归：无设备 PQC 项目提交时不要求设备字段，明细设备快照字段为空；设备必填项目仍严格校验设备 ID 与编号。
- 相邻回归：PQC 设备标准/检验方法静态合同、PQC 正式提交相邻链路不被破坏。
- 基础检查：`git diff --check`。

## Current Status

completed

实现、验证、经验沉淀和 task-closeout-cleanup preview/apply 均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本次按 QA 规程正式字段 `equipmentRequired` 决定是否要求设备。
- `是否从根因和长期维护角度解决`：是。修复前端映射/提交校验与后端明细构建的正式规则边界。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Bug Regression Fix Loop：先复现/新增失败回归，再最小修复并记录 RED/GREEN。
- 前端提交前严格验证与草稿态计算隔离门禁：正式提交结构字段只对设备必填项目严格校验；无设备项目不得被必填字段阻塞。
- MES PQC 项目级检验快照门禁：PQC 检验项目必须来自 QA 规程按产品、路线版本、路线工序和工序的正式映射，不用设备配置补齐 QA 项目关系。
- Strict No-Fallback：不得用默认设备、空成功、隐藏错误或前端文案掩盖正式设备必填/非必填规则。
