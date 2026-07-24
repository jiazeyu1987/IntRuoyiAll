BDD: 提示词管理页必须展示可读中文模板 -> Given PRODUCT_COVER 当前版本存储的是 UTF-8 被错误字符集解释后的乱码文本 / When 后端返回当前提示词与历史提示词 / Then 页面看到的模板正文必须恢复为正常中文，不得出现 `�`、`?` 或半损坏句子。
BDD: 封面生成必须使用修复后的提示词正文 -> Given PRODUCT_COVER 当前版本模板正文在库里仍是已知乱码形式 / When 后端渲染产品封面提示词 / Then 渲染结果必须包含正常中文说明和正确占位符替换，而不是继续输出乱码。
INFO: 真实复现 -> 本地真实接口 `GET /admin-api/showroom/prompt/current?sceneCode=PRODUCT_COVER` 当前返回的 `templateText` 含大量 `�` / `?`，可直接在提示管理页复现截图中的乱码现象。
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 `requireCurrentAndHistoryShouldRepairStoredWindows1252Utf8Mojibake` 断言未通过，证明原修复逻辑无法恢复真实运行库里的单字节混合乱码。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`ISO-8859-1`、混合 `cp1252`/控制位乱码，以及“乱码行 + 正常中文行”三类用例全部通过。
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，最新修复已打进 `yudao-server.jar`。
GREEN: runtime probe -> PASS，`GET http://127.0.0.1:48081/v3/api-docs` 返回 `200`，当前监听进程为 `backend-20260524-145748.jar`。
GREEN: real prompt API probe -> PASS，真实登录 `tenant-id=122 / aoteman / admin123` 后调用 `GET /admin-api/showroom/prompt/current?sceneCode=PRODUCT_COVER` 与 `GET /admin-api/showroom/prompt/history?sceneCode=PRODUCT_COVER`，模板正文汉字数恢复到 `460`，且不再包含 `�` / `?`。
