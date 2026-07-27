# Verification Report

## Scope

- 写入当前本机 `ruoyi-vue-pro` 数据库、当前租户 `tenant_id=1` 的测试管理数据。
- 目标项目为 `批记录`，节点为：解析、版本治理、绑定快照、批次任务、填写审批、归档追溯。
- 未访问远端测试服、正式服、备用服或共享存储。

## Result

- 新增测试项 6 个：`批记录节点：解析`、`批记录节点：版本治理`、`批记录节点：绑定快照`、`批记录节点：批次任务`、`批记录节点：填写审批`、`批记录节点：归档追溯`。
- 新增测试目标检查点 24 个，每个节点 4 个。
- 每个节点包含 3 个测试方法项。
- 所有节点均为 `ENABLE`、`SEQUENTIAL`、`parallelSafe=false`。
- 已按用户要求改为业务测试人员可理解的页面操作/页面观察口径，不再使用只有程序员能看到的字段、接口、状态码、hash 或英文内部状态。

## Verification

- RED: 写入前 6 个期望节点均缺失。
- GREEN: 受控事务写入 6 个测试项和 24 个检查点。
- Final SQL verification: `final_node_cases=6`，`final_node_checkpoints=24`。
- Structure verification: 6 个节点均为 3 个方法项 + 4 个目标项。
- Business-readable verification: 内部词扫描命中数为 0。
- Non-target verification: `工艺路线=4`、`智能排产=4` 保持不变。

## Notes

- 当前测试管理 schema 尚无独立 `测试节点` 字段，因此本次用测试项名称 `批记录节点：<节点名>` 和 `test_data_text` 中的 `测试节点=<节点名>` 明确表达节点维度。
- 后续若新增正式 `test_node` 字段，可按这 6 条数据迁移到独立字段。
