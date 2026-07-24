# 20260724-publish-current-to-test-server

## Task Goal

将当前 `E:\IntRuoyi` 程序发布到测试服务器 `172.30.30.58`。

## Milestones

- [x] 建立任务记录与执行日志。
- [x] 核对 CI/CD 技能契约、服务器访问文档与发布脚本入口。
- [ ] 完成发布前置检查。
- [ ] 执行测试服务器发布。
- [ ] 验证测试服务器前端、后端健康检查与展厅站点。
- [ ] 收尾并记录最终验证结果。

## Expected Verification

- 发布命令使用当前系统脚本：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\publish-int-ruoyi.ps1 -Environment test -ServerHost 172.30.30.58`。
- 测试服务器后端健康检查：`http://172.30.30.58:48081/actuator/health`。
- 测试服务器前端入口：`http://172.30.30.58:8081/`。
- 测试服务器展厅站点：`http://172.30.30.58:8083/`。

## Current Status

blocked

## Blockers

- `docs/experience-index.md` 缺失；按项目规则，高风险发布工作在缺少经验门禁时必须阻塞，除非用户明确授权带风险继续。
- 当前 Git 工作区不是干净状态；发布“当前程序”会把未提交/未跟踪改动一起构建到测试服务器，需要用户明确确认这些改动都属于本次发布范围。

## Dirty Workspace Evidence

- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiFronted/tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`
- `doc/tasks/20260724-batch-fda-audit-log-coverage/`
- `doc/tasks/20260724-edhr-document-filler-display/`
- `doc/tasks/20260724-route-form-slot-execution-task-generation/`
- `doc/tasks/fix-batch-exec-last-update-created-time/`
- `doc/tasks/fix-batch-record-fill-rule/`
- `doc/tasks/rewrite-access-docs-current-system/`
- `docs/login-access.md`
- `docs/server-access.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否；当前被缺失经验门禁与脏工作区阻塞，不能先发布再补确认。
- `是否存在临时补丁或绕过`：否。

