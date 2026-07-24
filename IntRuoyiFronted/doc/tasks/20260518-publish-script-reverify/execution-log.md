BDD: 发布脚本使用的前端构建命令不能被无关页面 lint 阻塞 -> Given `publish-int-ruoyi-to-test.ps1` 直接调用 `pnpm exec vite build --mode test` / When 前端存在不合法的自闭合原生 `audio` 标签 / Then 构建必须明确失败暴露问题，修复后同一命令必须恢复通过。
- RED: `pnpm exec vite build --mode test` with publish-script runtime overrides -> FAIL, `src/views/ai/music/manager/TtsTestPane.vue` triggered `vue/html-self-closing` because the native `audio` element was self-closing.
- GREEN: changed the native `audio` tag in `TtsTestPane.vue` from self-closing form to explicit open/close tags.
- GREEN: `pnpm exec vite build --mode test` with publish-script runtime overrides -> PASS, `Build successful. Please see dist-intruoyi-test directory`.
