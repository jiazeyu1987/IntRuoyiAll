# 20260524 prod frontend only publish g6 g7

## 任务目标

- 记录收到 `PROD` 后的正式发布前检查和 G6/G7 复验计划。
- 若标准 direct publish 脚本会改写正式数据库密码，则 fail closed，并等待 frontend-only 发布确认。

## 里程碑

1. 创建任务文档与 BDD/TDD 证据。
2. 检查正式状态和发布脚本风险。
3. 若安全则发布并复验；若存在生产数据风险则阻塞。
4. 更新证据并等待用户确认。

## 预期验证

- 标准发布路径不得改坏正式 `.env`。
- frontend-only 发布如获批准，必须保持生产 `.env`、MySQL、MinIO 和 backend image 不变。
- 发布后 G6/G7 必须通过 Playwright 真实路径。

## 当前状态

- 状态：blocked
- 当前阶段：direct publish 脚本因 MySQL 密码不一致风险被阻塞。
- 已完成：
  - 用户已输入 `PROD`。
  - 用户提供责任人候选：`jiazeyu`、`tangbin`。
  - 正式运行状态当前可用。
  - direct publish 脚本风险已确认：即使 skip data，也会重写远端 `.env` 的 `MYSQL_ROOT_PASSWORD`。
- 当前阻塞：
  - 已新增 fail-closed frontend-only 发布脚本；真正执行生产替换前仍需用户明确回复 `FRONTEND-ONLY PROD`。
