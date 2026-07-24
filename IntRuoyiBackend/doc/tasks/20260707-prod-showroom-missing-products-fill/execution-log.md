BDD: 正式服展厅产品应补齐为全量产品 -> Given 正式服 admin 或 website 仍缺产品, When 核对正式库、导入全量产品包并发布展厅, Then 正式服产品数量、旧编号映射、展柜引用、语音资产和 website 发布包均完整一致。
GREEN: experience-preflight -> PASS, 已读取 PowerShell、server-access、login-access、release-backup-restore 和上一轮正式服 product_003~009 修复任务记录；用户已明确授权正式服补全，写入范围限定为展厅产品补齐/导入/发布/验证。
GREEN: prod-vs-local-schema-preflight -> PASS, 本机 showroom_product 含 frozen_flag/frozen_hall_count，正式服无冻结字段；本次补全禁止直接复制冻结字段，只通过正式服兼容的产品 zip 导入链路补齐。
GREEN: local-full-export -> PASS, 本机芋道源码/admin 真实页面导出 zip：evidence/showroom-products-local-admin-20260707T122451Z.zip，大小 692656854 bytes。
GREEN: prod-backup-before-fill -> PASS, 正式服导入前已生成数据库备份，路径见本次命令输出。
GREEN: prod-direct-api-import-skip -> PASS, 正式服通过现有 /showroom/product/import-excel 直传 163 产品 zip，sameProductAction=SKIP，响应证据 prod-direct-api-import-response-1783428976532.json。
GREEN: prod-db-count-after-import -> PASS, 正式服产品补齐后 active_total=163、active_int=163，16 个缺失 INT 产品均存在。
GREEN: prod-final-admin-data-verify -> PASS, 正式服管理端产品总数 163，16 个补入 INT 产品均可查询，Website current release 正常，证据 prod-final-admin-data-verify-1783429419199.json。
GREEN: prod-task-complete -> PASS, 正式服产品补齐、展柜画布布局修复、手动发布和最终数据核验均完成；最终 release=20260707T130144Z-be276b74dfa8-081780e2a98e。
NOTE: legacy-product-code-empty -> 16 个补入产品 legacy_product_code 为空，本次未做旧编号猜测映射，避免 product_* 错配。
GREEN: closeout-cleanup -> PASS, 已仅删除本次临时大体积导出 zip，保留关键 JSON/日志证据。
GREEN: prod-yudao-legacy-code-backfill -> PASS, 正式服芋道源码租户补写 15 个可靠旧产品编号映射；INT-83/product_082 因名称匹配歧义未猜测写入，备份 /opt/intruoyi/runtime/backups/prod-showroom-product-legacy-before-20260707T130917Z.sql.gz，证据 prod-yudao-legacy-code-backfill-1783429758847.json。
