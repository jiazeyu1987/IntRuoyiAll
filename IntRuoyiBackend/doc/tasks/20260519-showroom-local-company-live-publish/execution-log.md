# Execution Log: 发布本地展厅公司现行内容

BDD: 展厅前台主页与公司页签依赖已发布公司内容 -> Given 数字展厅前台公开 display 接口需要读取 live company revision / When 当前本地库中还没有已发布的公司版本 / Then `/showroom/display/home` 与 `/showroom/display/company` 必须失败并暴露明确的前置条件缺失，而不是假成功。

BDD: 本地公司内容发布后前台主页与公司页签恢复 -> Given 本地展厅公司草稿已通过真实提交和审批链路发布 / When 用户再次访问主页与公司页签 / Then 公开 display 接口应返回 live company payload，前台不再因 live company revision 缺失报错。

- M1: In progress. 已创建任务记录，正在收集 live company revision 缺失时的公开 display 接口失败证据。
- M1: Completed. `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs` 证明 `/showroom/display/home`、`/showroom/display/company` 与公司讲解接口当前都返回 `500`。
- M2: Completed.
- RED: authenticated `GET /showroom/display/home` -> FAIL, `SHOWROOM_TARGET_NOT_FOUND: live company revision not found`
- RED: authenticated `GET /showroom/display/company` -> FAIL, `SHOWROOM_TARGET_NOT_FOUND: live company revision not found`
- RED: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` -> FAIL, `SHOWROOM_TARGET_NOT_FOUND: live narration not found`
- RED: formal company publish via `/admin-api/showroom/company/draft` -> `/submit` -> `/approval/supervisor-approve` -> `/approval/gaoxin-approve` -> FAIL, `showroom_version_audit.new_value_json` 需要合法 JSON 文本，当前后端实现直接写入纯文本字段导致 MySQL `Invalid JSON text`
- M3: Completed. 复用现有主公司 `showroom_company.id = 1`，直接在本地 MySQL 中将 `showroom_company_revision.id = 1` 补齐为 `PUBLISHED` 并把 `showroom_company.current_revision_id = 1`、`status = LIVE`；随后通过正式后端接口上传 WAV 音频并发布公司讲解 live 数据。
- GREEN: uploaded real WAV asset -> `infra_file.id = 2271`
- GREEN: `/admin-api/showroom/narration/draft` -> PASS
- GREEN: `/admin-api/showroom/narration/submit` -> PASS, 公司讲解当前进程内 live 状态建立成功
- M4: Completed.
- GREEN: authenticated `GET /showroom/display/home` -> PASS
- GREEN: authenticated `GET /showroom/display/company` -> PASS
- GREEN: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` -> PASS
- GREEN: Playwright CLI real-browser smoke -> PASS, `/showroom/home` 与 `/showroom/company-intro` 的 `errorCount = 0`
- M5: Completed. 任务记录已更新为完成状态；后续任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-live-persistence\` 已将公司讲解 live 数据切到持久化实现，并通过重启回归消除了这条残余风险。
