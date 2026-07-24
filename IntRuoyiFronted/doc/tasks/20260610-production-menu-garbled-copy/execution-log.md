# 执行记录：生产管理菜单乱码修复

## BDD 场景

- `BDD: 生产管理子菜单显示中文文案 -> Given 用户打开后台管理系统并展开生产管理菜单 When 菜单树渲染生产管理下的子菜单 Then 子菜单名称应显示规范简体中文且不包含问号乱码`

## TDD 证据

- `RED: node tests\e2e\mes-production-menu-copy-static.spec.js -> FAIL, 生产工单页面源码仍包含问号乱码`
- `RED: node doc\tasks\20260610-production-menu-garbled-copy\scripts\verify-production-menu-copy-db.mjs -> FAIL, 菜单 5580 仍包含问号乱码 ?????`
- `GREEN: node tests\e2e\mes-production-menu-copy-static.spec.js -> PASS`
- `GREEN: node doc\tasks\20260610-production-menu-garbled-copy\scripts\verify-production-menu-copy-db.mjs -> PASS`
- `GREEN: Playwright 本机真实页面复核 -> PASS, 登录 芋道源码/admin 后打开 /mes/pro/work-order，侧边栏生产管理下显示 排产员工作台 与 排产工单池，菜单文本不包含 ????`

## 扫描结论

- 全量前端扫描输出过大导致首次命令超时；随后收窄到 `src/views/mes/pro/workorder` 复扫，`garbled_text: 0`。
- `rg -n -F "????" src` 无匹配。
- 扫描器在生产工单范围仍提示若干 `MES`、`BOM`、`ID`、`Ref` 等混合术语或注释，本次按用户反馈只处理问号乱码，不扩展为全量文案改造。

## 根因

- 本机运行库 `system_menu` 的 `id=5590`、`id=5580` 菜单名被历史写入为问号；两条记录分别对应 `scheduler-workbench` 与 `schedule-order`。
- 对应源码 SQL `20260610_mes_scheduler_workbench_p7.sql` 与 `20260610_mes_schedule_order_p1.sql` 中中文正常，说明当前问题是运行库数据损坏。
- `生产工单` 页面内 `kingdeeSyncLoading` 注释和金蝶同步成功提示残留问号乱码。

## 验证记录

- `docker exec int-ruoyi-mysql mysql ... DESCRIBE system_menu; SELECT ... WHERE id=5700 OR parent_id=5700` -> PASS，确认 `5590=??????`、`5580=?????`。
- `docker cp ... repair-production-menu-garbled-copy.sql int-ruoyi-mysql:/tmp/...` 与容器内 `mysql --default-character-set=utf8mb4 < /tmp/...` -> PASS，避免 Windows 命令行中文参数链路。
- `SELECT id,name,parent_id,path,sort FROM system_menu WHERE id IN (5590,5580)` -> PASS，结果为 `5580 排产工单池`、`5590 排产员工作台`。
- `node --check tests\e2e\mes-production-menu-copy-static.spec.js` -> PASS。
- `node --check doc\tasks\20260610-production-menu-garbled-copy\scripts\verify-production-menu-copy-db.mjs` -> PASS。
- `git diff --check` -> PASS，仅输出既有 CRLF 警告。
- `npm run ts:check` -> FAIL, 默认 Node 堆内存 OOM。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> FAIL, 未触及文件 `src/views/mes/pro/batchrecordtemplate/index.vue` 存在 `handleSignatureCells`、`signatureDialog` 等签名单元格属性缺失。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260610-production-menu-garbled-copy --mode preview` -> PASS, status `ready`，delete `<none>`，blocked `<none>`。
