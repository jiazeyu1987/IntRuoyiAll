# Docker Build & Up

目标: 快速部署体验系统，帮助了解系统之间的依赖关系。
依赖：docker compose v2，删除`name: yudao-system`，降低`version`版本为`3.3`以下，支持`docker-compose`。

## 功能文件列表

```text
.
├── Docker-HOWTO.md                 
├── docker-compose.yml              
├── docker.env                      <-- 提供docker-compose环境变量配置
├── yudao-server
│   └── Dockerfile
└── yudao-ui-admin
    ├── .dockerignore
    ├── Dockerfile
    └── nginx.conf                  <-- 提供基础配置，gzip压缩、api转发
```

## 构建 jar 包

```shell
# 创建maven缓存volume
docker volume create --name yudao-maven-repo

docker run -it --rm --name yudao-maven \
    -v yudao-maven-repo:/root/.m2 \
    -v $PWD:/usr/src/mymaven \
    -w /usr/src/mymaven \
    maven mvn clean install package '-Dmaven.test.skip=true'
```

## 构建启动服务

启动前必须在 `docker.env` 中显式填写 DCC 受控下载加密配置：

- `DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION`
- `DCC_DOWNLOAD_ENCRYPTION_KEY_ID`
- `DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY`，Base64 解码后必须是 16、24 或 32 字节 AES key
- `DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY`

这些配置没有默认值；缺失时 compose 会失败，不会启动一个无法受控加密下载的后端。

```shell
docker compose --env-file docker.env up -d
```

首次运行会自动构建容器。可以通过`docker compose build [service]`来手动构建所有或某个docker镜像

`--env-file docker.env`不是可选参数；DCC 受控下载加密必须显式配置。
`docker.env is required` for DCC controlled download encryption.

## 服务器的宿主机端口映射

- admin ui: http://localhost:8080
- api server: http://localhost:48080
- mysql: root/123456, port: 3306
- redis: port: 6379
