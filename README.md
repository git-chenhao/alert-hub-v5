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

<!-- SMOKE-MARKER: ci-verification-checkpoint -->

## License

MIT License

control-plane real e2e smoke 2026-03-28 run-3
<!-- SMOKE_MARKER: v2-test-20260309 -->
second smoke marker

control-plane real e2e smoke 2026-03-28 run-4
control-plane real e2e smoke 2026-03-28 run-5
