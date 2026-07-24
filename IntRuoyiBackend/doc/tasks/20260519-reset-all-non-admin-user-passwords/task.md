# 任务：批量重置非管理员用户密码为 111111

## 目标

将当前运行库中的现有后台用户密码统一重置为 `111111`，但排除管理员账号，且保持系统现有密码加密规则不变。

## 范围

- 盘点当前 `system_users` 用户数据，确认管理员排除规则。
- 使用与系统一致的 BCrypt 密码加密方式执行批量重置。
- 记录重置前后的只读校验证据与影响范围。
- 不修改管理员账号密码。

## 里程碑

- [x] M1: 确认任务前置状态、旧任务阻塞记录和当前数据库连接方式。
- [x] M2: 盘点当前用户数量、管理员账号及待重置用户范围。
- [x] M3: 执行非管理员用户密码批量重置为 `111111`。
- [x] M4: 验证管理员未受影响、非管理员已完成重置并记录结果。
- [x] M5: 更新任务文档、执行收尾预览并提交本任务相关改动。

## 预期验证

- 重置前后用户总数一致。
- 用户名为 `admin` 的账号密码不被本次任务覆盖。
- 所有非 `admin` 用户的 `password` 字段被更新为新的 BCrypt 哈希。
- 通过抽样校验确认 `111111` 可匹配非管理员的新密码哈希，而管理员密码哈希保持原值。

## 当前状态

Completed on 2026-05-19. 非管理员用户已批量重置为 `111111`，管理员账号 `admin` 未受影响，收尾预览已执行并清理临时摘要文件。

## 最终验证结果

- PASS: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `admin/admin123`
- PASS: `PUT /admin-api/system/user/update-password` executed successfully for `2121` non-admin users
- PASS: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `zhangsan/111111`, `dccquery/111111`, `A4020063/111111`
- PASS: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `admin/111111` continued to fail, confirming admin password was not modified
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-reset-all-non-admin-user-passwords --mode preview`
