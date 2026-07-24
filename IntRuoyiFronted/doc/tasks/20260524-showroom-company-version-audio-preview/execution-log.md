# 执行日志：公司版本页签历史版本补齐语音播放

BDD: 公司版本历史预览展示双语语音 -> Given 用户在 `http://localhost:8081/showroom/company-version` 点击某个历史版本的“查看版本” / When 弹窗加载该 revision 的 `COMPANY` detail 合同 / Then 中文和 English tab 都必须显示该版本对应讲解稿、语音版本、音色与可播放音频，不得继续只展示字段和封面。
RED: `node --test scripts/showroom-admin-company-version-tab.test.mjs` -> FAIL，`CompanyVersionWorkbench.vue` 仍使用 `ShowroomAdminApi.getCompany(...revisionId)` 读取历史详情，源码中也还没有 `getVersionCenterDetail`、双语音频播放器与无音频显式提示。
GREEN: `node --test scripts/showroom-admin-company-version-tab.test.mjs scripts/showroom-admin-version-center.test.mjs` -> PASS，历史预览已切到版本中心 detail 合同，源码断言确认存在双语语音版本/音色/`<audio controls preload="none">`。
GREEN: `pnpm exec eslint src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue scripts/showroom-admin-company-version-tab.test.mjs` -> PASS。
INFO: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` @ `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` -> PASS，仅用于把本地 48081 运行时升级到当前仓库后端代码，未修改后端 repo 跟踪文件。
INFO: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> FAIL，后端 48081 重启成功，但前端 `pnpm dev` 因本地 `vite` 命令解析异常未在 120 秒内拉起 `8081`。
INFO: 手动以前端本地 `vite/bin/vite.js --mode env.local` 拉起 `http://localhost:8081` -> PASS，当前页面可访问。
INFO: 为了让本地开发库匹配当前后端查询列，只对本地 `23306/ruoyi-vue-pro` 执行了兼容 DDL：补齐 `showroom_company_revision.display_name_snapshot/display_name_en_snapshot/company_type_snapshot` 与 `showroom_version_bundle` 表；执行后确认 `showroom/company/current` 返回 HTTP 200。
INFO: 进一步按当前系统已有规则补齐本地验证数据：使用 `showroom_company` 主表回填 company `revision 1..8` 的 snapshot 字段，并仅为存在“同 revision 下最新已发布双语语音”的版本建立 bundle；本地最终得到 company bundle：`revision 7 -> zh 92 / en 93`，`revision 8 -> zh 108 / en 102`。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-version-audio-preview run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-version-audio-preview\scripts\verify-showroom-company-version-audio-preview.mjs` -> PASS，真实 `http://127.0.0.1:8081/showroom/company-version` 登录后自动扫描可读历史 revision，成功打开带 bundle 的公司历史版本弹窗，并验证中文/English 两个 tab 都挂载出与 detail 合同一致的真实音频地址。
GREEN: 运行时 API 复核 -> PASS，`revision 7` 与 `revision 8` 的 `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId={id}` 均返回 `code=0`，且包含真实 `zhAudio / enAudio` URL。
INFO: 残余数据缺口：本地 `revision 1/3/4/5/6` 仍无已发布双语语音，因此未生成 bundle；当前前端实现对有 bundle 的历史版本可正常展示和播放语音，对无 bundle 的旧版本仍会暴露后端真实缺口，而不会伪造历史语音。
