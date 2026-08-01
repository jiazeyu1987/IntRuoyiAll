# PQC 检验员职责文档落地

## Task Goal

将用户确认的新一线 PQC 口径下的“PQC 检验员未来一天系统操作流程”写入 `C:\Users\BJB110\Desktop\文档\职责\pqc检验员.md`。

## Milestones

- [x] 创建任务目录和目标职责目录。
- [x] 确认目标文件不存在，避免误覆盖。
- [x] 核对产品需求文档技能、任务收尾规则、PowerShell UTF-8 规则和 PRD 结构参考。
- [ ] 写入 PQC 检验员职责文档。
- [ ] 执行 UTF-8 读取和关键口径验证。

## Expected Verification

- 使用 UTF-8 读取目标职责文档，确认中文可读。
- 搜索确认目标文档包含“一线 PQC 检验入口”“活跃订单”“路线工序”“工序池 PQC 事件”“旧 IPQC 不作为主入口”等关键口径。
- `git diff --check` 验证本任务记录无空白错误。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，文档直接按用户裁定的新一线 PQC 口径编写，不用旧 IPQC 或待检任务替代。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 中文 Markdown 写入必须使用 UTF-8；本任务使用 `apply_patch` 写入文档。
- 本任务只写职责文档和任务记录，不修改生产代码、数据库、接口、路由、权限或运行态。
