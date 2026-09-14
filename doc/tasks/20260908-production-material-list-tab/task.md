# 生产用料清单 Tab 开发验证

## Task Goal

在活跃订单工序提交详情页新增“生产用料清单”主 tab，按当前活跃订单对应生产订单编号读取 ERP 生产用料清单，并以接近纸质单据的“单据头 + 明细 + 制单审核信息”方式展示；支持同一生产订单存在多张生产用料清单。

## Milestones

- [done] 建立 worktree 与任务记录
- [done] 定位现有生产用料清单 API/数据结构
- [done] 编写 RED 静态/接口契约
- [done] 实现前端展示
- [done] 定向静态测试、类型检查、worktree 正向真实页面验证
- [done] 用户授权后，为指定 SIM-COPY 订单复制本地正式生产用料清单测试数据并完成正向展示验证

## Expected Verification

- 静态合同证明详情页存在“生产用料清单”主 tab，且不会用领料单/补料单数据冒充。
- 后端或前端合同证明按当前详情生产订单号查询生产用料清单，且支持多张单据。
- 定向后端编译/测试通过。
- 定向前端静态测试通过。
- worktree 独立端口真实页面验证可见该 tab 和目标订单对应生产用料清单内容。

## Current Status

ready_for_closeout

实现、数据准备和 worktree 真实页面验证已完成；尚未执行最终提交/推送/清理收尾。

2026-09-08 已从 `int_main` 创建 worktree `D:\IntRuoyiWorktree\production-material-list-tab`，分支 `codex/20260908-production-material-list-tab`，预约 `int_main slot=32`，前端端口 `8207`，后端端口 `48207`。

2026-09-08 已实现“生产用料清单”主 tab：页面按当前显示生产订单号调用 `/erp/production-material-list/page`，按 `sourceBillNo` 分组逐张展示。

2026-09-08 经用户授权，已从同类“球囊扩张压力泵”生产用料清单来源 `881MO101365 / 881PPBOM00001983` 复制有效行到目标生产订单 `SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891`，新单据编号 `SIM-PML-CODX-PQC-WO05-001`，目标租户保留 11 行。

## 设计约束检查

- 不使用 BOM 推断生产用料清单。
- 不把领料单/补料单数据当生产用料清单展示。
- 同一生产订单多张生产用料清单必须全部展示。
- 缺少正式生产用料清单时显示空态，不伪造默认成功数据。
