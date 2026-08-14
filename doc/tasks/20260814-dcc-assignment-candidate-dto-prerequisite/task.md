# DCC 项目代码分配候选 DTO 前置修复

## 任务目标

补齐提交 `333029852` 已被正式控制器、服务与测试引用但遗漏的两个 DCC assignment candidate DTO，使干净 worktree 可以编译并继续一线 PQC 方案 A 的后续门禁。

## 里程碑

- [x] M1：确认用户授权只补两个 DTO，并保留旧冲突改动 patch，不自动套回。
- [x] M2：建立隔离 worktree、释放本任务空 worktree 槽位并登记 `int_main slot 14`。
- [x] M3：记录 BDD，复现干净 checkout 的编译 RED。
- [x] M4：仅新增两个正式 DTO，运行目标测试和回归 GREEN。
- [ ] M5：主管代码评审、独立 Agent 验证、提交并 `--ff-only` 合入 `int_main`。
- [ ] M6：按规则清理本任务 worktree 和端口登记。

## 预期验证

- DCC reactor 目标测试在 DTO 缺失时因 `cannot find symbol` 失败，补齐后通过。
- `backend-api-delivery` 与 `bug-regression-fix-loop` evidence validator 通过。
- `git diff --check`、分支运行端口 guard、提交文件清单和独立验证通过。
- 合并只使用 fast-forward，不包含主工作区其它未提交改动。

## 适用经验门禁

- 一线 PQC 的正式权威仍为路线到 DCC 的正式关系及 QA 的 `dccProjectCodeId`；本修复不增加第二套关系、不按产品或路线推算 QA。
- Maven 使用 reactor `-am` 验证，不能用本地陈旧产物掩盖缺少类型。
- 附加 worktree 必须登记 `1..19` 槽位；提交前运行 branch runtime guard。
- 主工作区已存在同名未跟踪 DTO，快进前必须先核对 patch 备份与内容等值，只处理这两个精确文件。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；补回正式调用链已依赖但提交遗漏的 API 数据合同。
- 是否存在临时补丁或绕过：否；只保留用户要求的冲突旧改动 patch 备份，不自动应用。

## Current Status

ready_for_closeout
