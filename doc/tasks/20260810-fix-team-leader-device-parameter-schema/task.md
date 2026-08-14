# 修复生产组长页签设备参数规则缺列异常

## Task Goal

修复芋道源码 `/admin` 进入生产组长页签时报“系统异常”的运行态 schema 漂移：当前代码查询 `mes_pro_process_pool_device_parameter_rule.option_values_json`，但本机运行库缺少该正式迁移字段。

## Milestones

- [x] 冻结后端异常栈和目标接口
- [x] 核对正式迁移与当前运行库字段差异
- [x] 按正式迁移补齐本机运行库缺失字段
- [x] 复验接口和生产组长页面不再系统异常
- [x] 记录验证证据并收尾

## Expected Verification

- RED: 运行库 schema 契约证明 `option_values_json` 缺失，且后端日志首个异常为 `Unknown column 'option_values_json'`。
- GREEN: 执行正式迁移后，同一 schema 契约证明目标字段存在。
- REGRESSION: 登录态目标接口或真实页面进入生产组长页签不再返回系统异常。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按正式 schema 迁移修复运行库，不改业务代码兼容旧表。
- 是否存在临时补丁或绕过：否。

## Applicable Gates

- docs/database-rules.md#运行态迁移漂移系统异常门禁：先冻结数据库异常与 Mapper，再用 information_schema 对比运行库和正式迁移，禁止业务 fallback。
- docs/powershell-encoding.md：中文任务记录与 SQL/日志读取使用 UTF-8，不记录数据库密码等敏感信息。
