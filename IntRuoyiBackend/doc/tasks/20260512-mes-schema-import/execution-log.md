# Execution Log: MES schema import

BDD: MES item APIs have required schema -> Given the MES module is enabled and the admin UI opens material item pages, When `/admin-api/mes/md/item/page` or `/admin-api/mes/md/item-type/simple-list` queries MySQL, Then the required MES item tables must exist and the API must return a normal business response instead of `系统异常`.

BDD: MES home statistics APIs have required schema -> Given the MES module is enabled and the admin UI opens the MES home page, When summary, work-order status, and production trend endpoints query MySQL, Then the required MES production tables must exist and the API must return a normal business response instead of `系统异常`.

## Evidence

- RED: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ruoyi-vue-pro' AND table_name IN ('mes_md_item','mes_md_item_type','mes_pro_workorder','mes_pro_task') ORDER BY table_name;"` -> FAIL, no matching MES tables returned.
- RED: backend log `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260512-200448.out.log` shows `SQLSyntaxErrorException` for missing `mes_md_item_type`, `mes_md_item`, `mes_pro_work_order`, and `mes_pro_feedback`.
- RED: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ruoyi-vue-pro' AND table_name IN ('mes_md_item','mes_md_item_type','mes_pro_work_order','mes_pro_feedback','mes_dv_machinery','mes_pro_andon_record','mes_dv_repair') ORDER BY table_name;"` -> FAIL, no matching MES home or item tables returned.
- GREEN: added `sql/mysql/20260512_mes_schema.sql` with additive `CREATE TABLE IF NOT EXISTS` statements for `mes_md_item`, `mes_md_item_type`, `mes_pro_work_order`, `mes_pro_feedback`, `mes_dv_machinery`, `mes_pro_andon_record`, and `mes_dv_repair`.
- GREEN: `cmd /c "docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro < sql\mysql\20260512_mes_schema.sql"` -> PASS, migration imported into local MySQL.
- GREEN: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ruoyi-vue-pro' AND table_name IN ('mes_md_item','mes_md_item_type','mes_pro_work_order','mes_pro_feedback','mes_dv_machinery','mes_pro_andon_record','mes_dv_repair') ORDER BY table_name;"` -> PASS, all seven required tables returned.
- GREEN: mapper-equivalent SQL for item page and MES home summary/status/trend queries -> PASS, queries execute successfully with `deleted` and `tenant_id` filters.
- GREEN: authenticated API checks with local `admin/admin123`, `tenant-id: 1` -> PASS, `/admin-api/mes/md/item-type/simple-list`, `/admin-api/mes/md/item/page?pageNo=1&pageSize=10`, `/admin-api/mes/home-statistics/summary`, `/admin-api/mes/home-statistics/work-order-status`, and `/admin-api/mes/home-statistics/production-trend` all returned `code=0`.
- GREEN: backend log tail after authenticated verification shows prepared SQL for the MES queries and no new `SQLSyntaxErrorException` for the fixed tables.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.
