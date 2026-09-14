# Bug Regression Evidence: 一线生产设备卡片截断

## Bug

一线生产设备卡片使用 `configuredDeviceCards.value.slice(0, 3)`，当某个工序运行态返回 4 台及以上设备时，第四台及后续设备不会出现在设备卡片中。

## Expected

生产组长进入一线生产并选择工序后，设备卡片应展示运行态 `devices` 返回的全部工序设备，不得在前端额外截断；单次提交仍只提交当前选中的设备参数。

## Reproduction

`C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs`

## Root Cause

前端 `visibleDeviceCards` computed 在展示层对正式运行态设备集合执行 `slice(0, 3)`，这是用户流程未要求的额外 UI 限制。

## RED:

`C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs` -> FAIL，断言命中 `configuredDeviceCards.value.slice(0, 3)`。

## GREEN:

`C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs` -> PASS。

## Verification

- `C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs` -> PASS。
- `C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe tests/e2e/frontline-production-device-row-density-static.spec.cjs`，workdir `IntRuoyiFronted` -> PASS。
- `C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe tests/e2e/frontline-production-device-parameter-range-static.spec.cjs`，workdir `IntRuoyiFronted` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs doc/tasks/20260808-frontline-production-risk-fixes/task.md doc/tasks/20260808-frontline-production-risk-fixes/execution-log.md` -> PASS，仅 LF/CRLF 提示，无 whitespace error。

## Blockers

无剩余实现阻塞。未复跑 Maven/JUnit，因为本轮只改前端展示集合和前端静态合同，后端提交、签名、参数校验代码未变化。