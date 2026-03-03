# Alert Hub V5

统一告警聚合平台

## 功能
- 接收告警：HTTP Webhook 接口
- 去重：基于指纹过滤
- 攒批聚合：可配置窗口策略
- 调度分析：A2A 协议调用 Sub-Agent
- 持久化：告警及批次数据落库
- 通知：飞书卡片推送
- 可视化：后台管理界面

## 技术栈
- Java 17 + Spring Boot 3.2
- Spring Data JPA + H2/MySQL
