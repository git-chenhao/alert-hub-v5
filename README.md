# Alert Hub V5

统一告警聚合平台，用于接收、去重、聚合、分析外部告警，并通过飞书推送通知。

## 核心功能

- **HTTP Webhook 接收告警** - REST API 接收外部告警数据
- **告警去重** - 基于指纹算法（SHA-256）进行去重
- **攒批聚合** - 可配置时间窗口（1min/5min/10min/30min）聚合告警
- **A2A 调度分析** - 调用 Sub-Agent 进行智能分析
- **持久化存储** - JPA + H2/MySQL 存储
- **飞书通知** - 卡片消息推送
- **管理界面** - Web 控制台查看告警/批次

## 技术栈

- Java 17
- Spring Boot 3.2.x
- Spring Data JPA
- H2 Database (开发) / MySQL (生产)
- Maven 构建工具
- Lombok

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 构建项目

```bash
mvn clean package
```

### 启动应用

```bash
mvn spring-boot:run
```

应用启动后访问：
- Dashboard: http://localhost:8080/api/admin/dashboard
- API Docs: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

### 发送测试告警

```bash
curl -X POST http://localhost:8080/api/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "source": "prometheus",
    "severity": "HIGH",
    "title": "CPU使用率过高",
    "description": "CPU使用率超过90%",
    "labels": {
      "env": "prod",
      "service": "api-server"
    }
  }'
```

## API 接口

### 告警接收

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/alerts` | 接收单个告警 |
| POST | `/api/alerts/batch` | 批量接收告警 |
| POST | `/api/alerts/alertmanager` | Prometheus Alertmanager Webhook |

### 管理接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/admin/alerts` | 分页查询告警列表 |
| GET | `/api/admin/alerts/{id}` | 获取告警详情 |
| GET | `/api/admin/batches` | 分页查询批次列表 |
| GET | `/api/admin/batches/{id}` | 获取批次详情 |
| GET | `/api/admin/stats` | 获取统计信息 |
| POST | `/api/admin/batches/{id}/analyze` | 手动触发分析 |
| POST | `/api/admin/batches/{id}/notify` | 手动发送通知 |

## 配置说明

### application.yml 主要配置

```yaml
# 聚合配置
alerthub:
  aggregation:
    window-minutes: 5      # 聚合时间窗口（分钟）
    enabled: true

  # 去重配置
  deduplication:
    cache-ttl-minutes: 60  # 缓存有效期

  # A2A 分析服务
  analysis:
    enabled: false
    endpoint: http://localhost:8081/api/analyze
    timeout-seconds: 30

  # 飞书通知
  notification:
    feishu:
      enabled: false
      webhook-url: ${FEISHU_WEBHOOK_URL:}
      min-severity: MEDIUM
```

### 环境变量

| 变量名 | 描述 |
|--------|------|
| `FEISHU_WEBHOOK_URL` | 飞书机器人 Webhook URL |

## 项目结构

```
alert-hub-v5/
├── src/main/java/com/alerthub/
│   ├── AlertHubApplication.java    # 主启动类
│   ├── config/                      # 配置类
│   ├── controller/                  # REST 控制器
│   ├── model/                       # 实体类
│   ├── repository/                  # 数据访问层
│   ├── service/                     # 业务逻辑层
│   ├── dto/                         # 数据传输对象
│   ├── util/                        # 工具类
│   └── exception/                   # 异常处理
├── src/main/resources/
│   ├── application.yml              # 主配置
│   ├── application-dev.yml          # 开发环境配置
│   └── templates/                   # Thymeleaf 模板
└── src/test/                        # 测试代码
```

## 告警处理流程

```
外部系统 → Webhook 接收 → 指纹去重 → 聚合批次 → A2A 分析 → 飞书通知
```

1. **接收**: 外部系统通过 Webhook 发送告警
2. **去重**: 基于 source + severity + title + labels 生成指纹，检查是否重复
3. **聚合**: 按 source + severity 分组，在时间窗口内聚合
4. **分析**: 调用 A2A 分析服务进行智能分析（可选）
5. **通知**: 发送飞书卡片消息通知（可选）

## 开发指南

### 运行测试

```bash
mvn test
```

### 打包部署

```bash
mvn clean package -DskipTests
java -jar target/alert-hub-v5-1.0.0-SNAPSHOT.jar
```

### 生产配置

生产环境建议使用 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/alerthub
    username: alerthub
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

## License

MIT License
