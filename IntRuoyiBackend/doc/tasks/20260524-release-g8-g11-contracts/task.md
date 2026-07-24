# 20260524 release g8 g11 contracts

## 任务目标

- 在服务仓库中为正式发布 G8/G9/G10/G11 文档契约增加可测试门禁。
- 保持当前系统约定：生产回滚、数据恢复、真实通知和正式发布均需要真实 owner 确认；缺少确认时 fail closed。

## 里程碑

1. 创建服务仓库任务文档与 BDD/TDD 证据框架。
2. RED：新增文档契约测试，证明当前 release go/no-go 文档缺少完整 G8-G11 确认接口。
3. GREEN：更新文档，使测试覆盖的契约齐备。
4. 执行回归、UTF-8 检查和 task-closeout 预览。
5. 提交服务仓库测试与任务证据。

## 预期验证

- `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi`

## 当前状态

- 状态：completed
- 当前阶段：G8-G11 文档契约测试与 Go/No-Go 文档补全已完成；真实 owner/webhook/发送证据仍待外部提供。
- 阻塞边界：
  - 文档和测试可以完成。
  - 真实 owner、webhook/target 和发送证据必须由业务/运维提供，不能由 Codex 代填。
