# Execution Log: 20260523-showroom-version-center-implementation

BDD: 版本中心历史列表只返回满足可读条件的已发布 bundle -> Given 公司或产品存在多条已发布 revision / When 用户进入版本中心 / Then 系统只返回具备历史读取条件的 bundle，并显式给出当前内容版本、当前线上版本与当前 release。

BDD: 版本中心详情必须返回可直接编码的字段级合同 -> Given 用户查看某个历史版本 / When 请求 detail 接口 / Then 返回必须包含字段顺序、双语值、内容图片、公开 preview asset、双语语音、当前内容版本、当前线上版本与 blocker 列表。

BDD: 历史版本一步到位重发必须复制成新版本并重建全局 release -> Given 用户选择一个可重发的历史版本 / When 用户执行 republish / Then 系统必须创建新 revision、复制历史媒体、写入新 bundle、发布新内容并调用 showroom release 发布链路，最终返回新 revision 与新 release。

BDD: 历史公司 revision 缺 authoritative snapshot 或全局 release 不健康时必须阻断 -> Given 历史公司版本缺 display_name/type snapshot，或其他 live source 导致全局 release 不健康 / When 用户查看详情或执行 republish / Then 系统必须 fail fast 并返回明确 blocker，不得回退当前 master 或跳过全局问题。

BLOCKER: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest,ShowroomSchemaMapperContractTest,ShowroomVersionCenterServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，失败原因与本任务无关：`ShowroomHttpApiIntegrationTest` 内存在重复测试方法，导致模块 `testCompile` 被拦截，无法用标准 Maven test 路径验证 focused 集合。

RED: focused manual JUnit run -> FAIL，先执行 `mvn -pl yudao-module-showroom dependency:build-classpath "-Dmdep.outputFile=target/version-center-test.cp" "-DincludeScope=test"`、再执行 `javac @yudao-module-showroom/target/javac-version-center.args`、最后执行 `java @yudao-module-showroom/target/java-version-center.args`；失败原因为 `ShowroomVersionCenterServiceTest#republishShouldCopyPublishedProductPackageAndSwitchCurrentRelease` 命中 `SHOWROOM_PREVIEW_STATIC_ASSET_MISSING`，暴露出 republish 复制 preview asset 时错误复用了只含 desktop 的 persisted files。

GREEN: focused manual JUnit run -> PASS，修复 preview asset 复制、detail 权限覆写、history published/bundle 一致性校验、staged global release precheck、公司历史 preview alt 后，重新执行手工 focused JUnit 流程，`ShowroomFoundationContractTest`、`ShowroomSchemaMapperContractTest`、`ShowroomPersistentContentServiceTest`、`ShowroomVersionCenterServiceTest`、`ShowroomVersionCenterControllerTest` 共 30 个测试全部通过。

REGRESSION: `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS，版本中心 controller/service 主链编译通过。

GREEN: backfill contract run -> PASS，执行 `javac @yudao-module-showroom/target/javac-version-center-backfill.args` 与 `java @yudao-module-showroom/target/java-version-center-backfill.args`，`ShowroomVersionCenterBackfillContractTest` 验证 company/product 成功插入，以及 company snapshot 缺失、company narration 多候选、product preview 多候选三类跳过分支均符合 backfill 设计。详见 `database-schema-evidence.md`。

GREEN: `python -m pytest script/tests/test_showroom_version_center_sql.py -q` -> PASS，2 tests / 0 failed，已验证版本中心 schema/backfill SQL 文本合同与 skip 规则。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260523-showroom-version-center-implementation/backend-api-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260523-showroom-version-center-implementation/database-schema-evidence.md` -> PASS。

CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\20260523-showroom-version-center-impl\ruoyi-vue-pro --task-id 20260523-showroom-version-center-implementation --mode preview` -> BLOCKED，preview 已确认默认保留 `task.md` / `execution-log.md`，附属证据文件可清理；但当前 linked worktree/main worktree 均非 ff-clean，且存在与本任务并行的未提交改动，因此本次仅保留 preview 结果，不执行任何清理或合并动作。

CORRECTION: 设计文档把“当前线上产品 revision 解析”表述成可直接从 active release source snapshot 取值；当前系统真实落地并未持久化显式 `productId -> revisionId` 映射，而是持久化 `preview_asset_version_ids_json`。实现中改为以 active release snapshot 内的 product preview asset version 反解 `productId -> sourceRevisionId`，再与 current release product documents 交叉确认产品确实进入线上集合。

CORRECTION: 设计文档默认 preview asset version 可直接复用完整多端文件组；当前持久化实现只在 `showroom_preview_asset_version.image_file_id` 落一份文件引用。republish 按现有真实逻辑复制时，必须把该 desktop fileId 同步填入 desktop/mobile/pad 三个槽位，否则会触发 `SHOWROOM_PREVIEW_STATIC_ASSET_MISSING` fail-fast。
