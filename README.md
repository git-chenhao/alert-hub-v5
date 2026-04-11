# Alert Hub V5

统一告警聚合平台 - 接收、去重、聚合、通知一站式解决方案

## 功能特性

### 核心功能
- **接收告警**: HTTP Webhook 接口，支持单个和批量接收
- **智能去重**: 基于指纹算法（SHA-256）的告警去重
- **批量聚合**: 可配置时间窗口的告警聚合策略
- **持久化存储**: 支持 H2 内存数据库和 MySQL 生产数据库
- **飞书通知**: 卡片消息推送，支持签名验证
- **后台管理**: Thymeleaf 可视化管理界面

### 技术栈
- **Java 17**
- **Spring Boot 3.2.2**
  - Spring Web
  - Spring Data JPA
  - Spring Cache
  - Spring Scheduling
  - Spring WebFlux
- **Thymeleaf** - 服务端渲染模板
- **Caffeine** - 高性能缓存
- **H2 Database** - 开发环境数据库
- **MySQL** - 生产环境数据库
- **Lombok** - 代码简化

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+

### 2. 启动应用

```bash
# 开发环境（使用 H2 内存数据库）
mvn spring-boot:run

# 或者指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境（使用 MySQL）
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 3. 访问应用

- **后台管理界面**: http://localhost:8080/admin
- **Webhook 接口**: http://localhost:8080/api/v1/alerts
- **H2 控制台**: http://localhost:8080/h2-console

## 使用说明

### 发送告警

#### 单个告警
```bash
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "source": "prometheus",
    "title": "CPU 使用率过高",
    "content": "当前 CPU 使用率达到 95%",
    "severity": "critical",
    "labels": {
      "instance": "server-001",
      "job": "node-exporter"
    },
    "annotations": {
      "description": "实例 server-001 CPU 使用率超过 90%"
    }
  }'
```

#### 批量告警
```bash
curl -X POST http://localhost:8080/api/v1/alerts/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "source": "prometheus",
      "title": "告警1",
      "severity": "warning"
    },
    {
      "source": "grafana",
      "title": "告警2",
      "severity": "info"
    }
  ]'
```

### API 响应格式

所有 API 响应遵循统一的 `AlertResponse` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1712832000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 响应状态码（200 成功，400 客户端错误，500 服务端错误） |
| `message` | String | 响应消息 |
| `data` | T | 响应数据（泛型，错误时可能为 null） |
| `timestamp` | Long | 响应时间戳（毫秒） |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "告警已接收",
  "data": {
    "id": 1,
    "fingerprint": "a1b2c3d4...",
    "source": "prometheus",
    "title": "CPU 使用率过高",
    "severity": "critical",
    "status": "pending",
    "createdAt": "2026-04-11 18:00:00"
  },
  "timestamp": 1712832000000
}
```

#### 去重响应示例

当告警指纹重复时，返回成功但 `data` 为 null：

```json
{
  "code": 200,
  "message": "告警重复，已去重",
  "data": null,
  "timestamp": 1712832000000
}
```

### 配置说明

#### application.yml 配置项

```yaml
alerthub:
  # 去重配置
  deduplication:
    enabled: true          # 是否启用去重
    cache-ttl: 300         # 缓存过期时间（秒）
    max-cache-size: 10000  # 最大缓存数量

  # 聚合配置
  aggregation:
    enabled: true          # 是否启用聚合
    window-size: 60        # 时间窗口（秒）
    max-batch-size: 100    # 最大批次大小

  # 飞书通知配置
  feishu:
    enabled: false         # 是否启用飞书通知
    webhook-url: https://open.feishu.cn/open-apis/bot/v2/hook/your-token
    secret: your-secret-key

  # 调度配置
  scheduler:
    enabled: true          # 是否启用定时调度
    initial-delay: 10000   # 初始延迟（毫秒）
    fixed-rate: 60000      # 执行频率（毫秒）
```

#### 环境变量

| 环境变量 | 必填 | 默认值 | 说明 |
|----------|------|--------|------|
| `ALERTHUB_ADMIN_PASSWORD` | 否 | （空） | 管理后台登录密码。未设置时启动会自动生成随机密码并输出到控制台日志 |
| `SPRING_PROFILES_ACTIVE` | 否 | `dev` | Spring 环境 profile（`dev` / `prod`） |
| `SERVER_PORT` | 否 | `8080` | 服务监听端口 |

**密码配置建议**：

```bash
# 启动时通过环境变量设置管理员密码
export ALERTHUB_ADMIN_PASSWORD=your-secure-password
mvn spring-boot:run

# 或者使用 Docker 环境变量
docker run -e ALERTHUB_ADMIN_PASSWORD=your-secure-password alert-hub-v5
```

> 如果不设置 `ALERTHUB_ADMIN_PASSWORD`，应用启动时会在控制台输出自动生成的密码，格式如下：
> ```
> ========================================
> Generated Admin Password: a1b2c3d4e5f67890
> Please set ALERTHUB_ADMIN_PASSWORD environment variable!
> ========================================
> ```

## 项目结构

```
alert-hub-v5/
├── src/main/
│   ├── java/com/alerthub/
│   │   ├── AlertHubApplication.java      # 主类
│   │   ├── config/                        # 配置类
│   │   │   ├── CacheConfig.java          # 缓存配置
│   │   │   └── WebConfig.java            # Web 配置
│   │   ├── controller/                    # 控制器
│   │   │   ├── AlertWebhookController.java   # Webhook 接口
│   │   │   └── DashboardController.java      # 管理界面
│   │   ├── dto/                          # 数据传输对象
│   │   ├── entity/                       # 实体类
│   │   │   ├── Alert.java                # 告警实体
│   │   │   └── AlertBatch.java           # 批次实体
│   │   ├── repository/                   # 数据访问层
│   │   └── service/                      # 业务逻辑层
│   │       ├── AlertService.java         # 告警服务
│   │       ├── DeduplicationService.java # 去重服务
│   │       ├── AggregationService.java   # 聚合服务
│   │       └── FeishuNotificationService.java  # 飞书通知
│   └── resources/
│       ├── application.yml               # 主配置
│       ├── application-dev.yml           # 开发环境配置
│       ├── application-prod.yml          # 生产环境配置
│       └── templates/                    # Thymeleaf 模板
└── pom.xml                               # Maven 配置
```

## 架构设计

### 告警处理流程

1. **接收告警** - 通过 HTTP Webhook 接收告警
2. **生成指纹** - 基于 source + title + severity + labels 生成 SHA-256 指纹
3. **去重检查** - 使用 Caffeine 缓存检查指纹是否存在
4. **持久化** - 保存到数据库，状态为 pending
5. **定时聚合** - 每分钟执行一次聚合任务
6. **批次创建** - 创建批次并关联告警
7. **飞书通知** - 发送卡片消息到飞书群
8. **状态更新** - 更新批次和告警状态

### 数据模型

#### Alert（告警）
- fingerprint: 告警指纹（唯一）
- source: 告警来源
- title: 告警标题
- content: 告警内容
- severity: 告警级别（critical/warning/info）
- status: 告警状态（pending/aggregated/sent/resolved）
- batchId: 关联批次 ID
- labels/annotations: 标签和注解

#### AlertBatch（批次）
- batchNo: 批次号
- windowStart/windowEnd: 时间窗口
- status: 批次状态（open/closed/sent）
- alertCount: 告警数量
- summary: 批次摘要

## 安全认证与登录流程

### 访问控制模型

Alert Hub 采用基于 Spring Security 的访问控制策略，将端点分为两类：

| 端点类别 | 路径 | 认证要求 | 说明 |
|----------|------|----------|------|
| Webhook API | `/api/v1/alerts/**` | 无需认证 | 外部系统推送告警的入口 |
| 管理后台 | `/admin/**` | 需要认证 | 告警管理、批次查看、手动聚合等 |
| 登录页面 | `/login` | 无需认证 | 表单登录入口 |
| H2 控制台 | `/h2-console/**` | 无需认证 | 仅限开发环境使用 |
| 健康检查 | `/actuator/**` | 无需认证 | 运维监控端点 |

### 登录流程

1. 访问 `/admin` 或任何 `/admin/**` 路径时，未认证用户会被自动重定向到 `/login`
2. 在登录页面输入用户名（默认 `admin`）和密码
3. 认证成功后自动跳转到管理后台首页 `/admin`
4. 退出登录请访问 `/admin/logout`，退出后跳转到 `/login?logout`

### 安全配置

- **认证方式**：Spring Security 表单登录（Form Login）
- **密码编码**：BCrypt 加密存储
- **CSRF 防护**：管理界面启用 CSRF 保护，API 端点（`/api/**`）已排除
- **安全开关**：通过 `alerthub.security.enabled` 配置项控制，设为 `false` 可关闭所有认证（不推荐在生产环境使用）

```yaml
alerthub:
  security:
    enabled: true              # 是否启用安全认证
    admin-username: admin      # 管理员用户名
    admin-password: ${ALERTHUB_ADMIN_PASSWORD:}  # 密码，建议通过环境变量设置
```

> **生产环境安全建议**：务必通过 `ALERTHUB_ADMIN_PASSWORD` 环境变量设置强密码，并确保 `alerthub.security.enabled` 为 `true`。

## 错误码与异常处理

### 错误响应格式

所有错误响应使用与成功响应相同的 `AlertResponse` 结构：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "timestamp": 1712832000000
}
```

### 错误码列表

| 错误码 | 触发场景 | 说明 |
|--------|----------|------|
| 400 | 请求参数校验失败 | 必填字段缺失或格式不正确（如 `source`、`title`、`severity` 为空） |
| 400 | 非法参数 | 传入不合法的参数值（`IllegalArgumentException`） |
| 500 | 服务端内部错误 | 未预期的运行时异常 |

### 常见错误示例

**参数校验失败（400）**：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "timestamp": 1712832000000
}
```

触发条件：请求体中缺少必填字段（`source`、`title`、`severity`）。

**服务端错误（500）**：

```json
{
  "code": 500,
  "message": "系统内部错误",
  "timestamp": 1712832000000
}
```

> **注意**：在生产环境（`prod` profile）下，500 错误的 `message` 字段不会返回具体异常信息，以避免泄露敏感信息。开发环境下会包含详细的错误描述。

## 开发指南

### 编译项目
```bash
mvn clean package
```

### 运行测试
```bash
mvn test
```

### 打包部署
```bash
mvn clean package -Dmaven.test.skip=true
java -jar target/alert-hub-v5-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### CI/CD

项目使用 GitHub Actions 进行持续集成，配置文件位于 `.github/workflows/ci.yml`。

**触发条件**：
- 推送到 `main` 分支
- 向 `main` 分支提交 Pull Request

**构建流程**：
1. 检出代码
2. 配置 JDK 17（Temurin 发行版）
3. Maven 缓存加速
4. 执行 `mvn compile` 编译验证

## License

MIT License
