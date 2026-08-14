# 本地主工作区前后端重启验证报告

## Result

PASS.

## Runtime Verification

- Runtime profile: `int_main`, slot `0`, frontend `8081`, backend `48081`.
- Backend listener after restart: PID `38348`, command line points to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081 --spring.profiles.active=local`.
- Frontend listener after restart: PID `12608`, command line points to `E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\vite --mode env.local --port 8081`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Frontend entry: `http://127.0.0.1:8081/` returned HTTP `200`.

## Conclusion

The local frontend and backend for `E:\IntRuoyi` were restarted successfully on the fixed `int_main` ports without changing runtime configuration or stopping unrelated worktree processes.
