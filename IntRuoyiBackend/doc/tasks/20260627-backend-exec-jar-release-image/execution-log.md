BDD: 后端发布镜像必须复制可执行 jar -> Given `yudao-server` 使用 `spring-boot-maven-plugin` 且配置 `classifier=exec` / When 发布镜像构建复制后端 jar / Then Dockerfile 必须复制 `yudao-server-exec.jar`，这样容器内 `app.jar` 才具有 `Main-Class` 和 `Start-Class`

RED: test-server-runtime `docker logs --tail 200 intruoyi-backend` -> FAIL, 真实测试服后端容器连续报错 `no main manifest attribute, in app.jar`

GREEN: `python -X utf8 - <<'PY' ... zipfile.ZipFile('...yudao-server-exec.jar').read('META-INF/MANIFEST.MF') ... PY` -> PASS, `yudao-server-exec.jar` 的 `MANIFEST.MF` 含 `Main-Class: org.springframework.boot.loader.launch.JarLauncher` 与 `Start-Class: cn.iocoder.yudao.server.YudaoServerApplication`

GREEN: Dockerfile diff -> PASS, `script/deploy/int-ruoyi-test/Dockerfile.backend` 与 `yudao-server/Dockerfile` 都已从复制 `yudao-server.jar` 改为复制 `yudao-server-exec.jar`
