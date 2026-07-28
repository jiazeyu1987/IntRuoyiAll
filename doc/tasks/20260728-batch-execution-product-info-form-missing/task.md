# 20260728 批次执行批记录表单缺少产品信息表单

## Task Goal

修复批次执行中“批记录表单”区域未展示同批记录版本“产品信息表单”的问题，确保该表单跟随正式 `MAIN + BATCH_RECORD` 批记录任务补齐，并统一固定在排序 `80`，等正式批记录表单完成后再填写，而不是来自表单槽位 `formBindings` 或工序开始配置。

## Milestones

- [x] 复现并定位批次执行详情中批记录表单缺失的具体链路。
- [x] 编写或更新最小回归测试，先证明“产品信息表单”缺失。
- [x] 实施最小正式修复，保持三类配置入口边界清晰。
- [x] 运行定向回归验证并记录 RED/GREEN 证据。
- [ ] 收尾：状态进入 `ready_for_closeout`，生成验证报告并完成 cleanup。

## Expected Verification

- 后端或前端定向回归测试覆盖批次执行详情/页面中正式批记录表单包含“产品信息表单”。
- 产品信息成员任务 `batchRecordSort` 固定为 `80`，在同工序正式批记录未完成前不可填写。
- 相邻批记录表单、表单槽位 `formBindings`、工序开始配置链路不互相替代。
- 不引入 fallback、默认表单、空值补齐或吞异常。

## Applicable Gates

- 批记录表单必须按工序设置中的逐工序批记录表单绑定读取，不能从 `formBindings`、默认 `MAIN`、工序开始上传人或其他特殊表单推断。
- eDHR 批次详情字段为空时，必须核对配置接口/表来源字段、执行任务快照字段、详情接口组装链路和既有优先级。
- 当前配置存在时必须优先当前 BATCH 工序配置；发布快照只在当前配置整体缺失且规则允许的场景使用。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；活跃批次读取时会从已有正式主批记录任务的定义/版本补齐缺失的产品信息成员任务，并统一产品信息排序为 `80`，确保正式批记录表单先填、产品信息后填，同时避免同工序排序唯一键冲突。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-batch-execution-product-info-form-missing/bug-regression-evidence.md

## Current Status

blocked

## Closeout Blocker

- `git push origin int_main` 被远端 non-fast-forward 拒绝。
- 当前 `int_main` 为 `ahead 6, behind 6`，且工作区仍有非本任务并行改动，不能安全执行 pull/rebase 或清理。
- 本任务实现提交：`842850cf fix: restore product info batch record task`。
- 本轮 80 排序补充变更尚未提交；目标回归与相邻 4 方法回归已通过，但提交/推送仍受远端 non-fast-forward 与并行未提交改动阻塞。
