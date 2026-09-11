# DCC P4 Review Round 2 Fixes

## Task Goal

修复二轮审查确认的三个高优先级阻塞：项目 OWNER 权威数据源、GxP 旧库迁移索引顺序、审批签名原因后端真实性。

## Milestones

- [x] 为 `dcc_project_access_rule` 建立正式持久化模型与 OWNER 查询实现，并覆盖真实实现测试。
- [x] 调整 GxP 旧库迁移为先删除完整索引、再扩字段、最后创建前缀索引，并补顺序合同。
- [x] 移除审批签名原因默认值，在请求 VO、审批适配器与服务边界 fail fast，并修正回归测试。
- [x] 运行定向与相邻回归，记录 RED/GREEN 和验证报告。

## Expected Verification

- DccProjectAccessServiceImpl 真实实现测试，不 mock 被测服务。
- GxP SQL 静态顺序合同；如可安全建立隔离 MySQL schema，再验证真实旧表升级。
- DCC 审批请求 Bean Validation 与签名服务空原因拒绝测试。
- DCC 定向回归、SQL 合同、`git diff --check`。
- 本轮不执行数据库写入、48081 重启、E2E、Git 提交或推送，除非用户另行明确授权。

## Current Status

completed

实现、定向验证、经验沉淀、cleanup 和代码提交均已完成。

## Design Constraints Check

- 项目访问只能读取 `dcc_project_access_rule`，不得从修正任务、负责人文本或菜单权限推断。
- OWNER > EDIT > VIEW；升大版本只接受当前有效 OWNER。
- 旧库迁移必须先移除不兼容索引，字段扩容后再创建前缀索引。
- 审批原因必须来自用户输入；VO 和服务边界均拒绝空白，不生成默认审计原因。
- 不引入 fallback、兼容分支、吞异常或模拟成功。
