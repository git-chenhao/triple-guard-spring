# 告警聚合平台 (Alert Aggregation Platform) 🚨

> 中间件根因分析多智能体架构 - 上游告警聚合系统

## 📋 项目背景

在中间件根因分析多智能体架构中，需要一个上游平台统一接收来自各告警系统的 Webhook，完成告警聚合后再调度各 Sub-Agent 进行根因分析。

## 🎯 核心职责

### 1. 接收告警
- 暴露 HTTP Webhook 接口
- 支持多种告警平台（Prometheus、Grafana、Zabbix 等）
- 告警数据标准化

### 2. 告警去重
- 基于指纹（Fingerprint）过滤重复告警
- 支持自定义去重规则
- 防止告警风暴

### 3. 攒批聚合
- **group_wait**: 等待时间窗口，收集同一组的告警
- **max_count**: 最大告警数量阈值
- **max_wait**: 最大等待时间
- 可配置的聚合策略

### 4. 调度分析
- 聚合批次触发后调用 Sub-Agent
- 使用 A2A 协议（Agent-to-Agent）通信
- 支持多个 Sub-Agent 并行分析

### 5. 持久化
- 所有告警数据落库
- 聚合批次记录
- 分析结果存储
- 历史数据查询

### 6. 通知
- 分析完成后推送飞书卡片
- 支持多渠道通知（飞书/邮件/短信）
- 可配置通知规则

### 7. 可视化
- 告警列表展示
- 聚合批次详情
- 分析结果展示
- 规则配置管理

## 🏗️ 系统架构

```
┌─────────────┐
│ 告警平台     │ (Prometheus/Grafana/Zabbix)
└──────┬──────┘
       │ Webhook
       ↓
┌─────────────────────────────────┐
│      告警聚合平台                │
├─────────────────────────────────┤
│  接收层    │ HTTP Webhook API   │
│  去重层    │ Fingerprint Filter │
│  聚合层    │ Batch Aggregator   │
│  调度层    │ Agent Scheduler    │
│  持久层    │ Database Storage   │
│  通知层    │ Feishu Notifier    │
└──────┬──────────────────────────┘
       │ A2A Protocol
       ↓
┌─────────────────────────────────┐
│     Sub-Agent 集群              │
├──────────┬──────────┬───────────┤
│ Agent 1  │ Agent 2  │ Agent 3   │
│ 根因分析  │ 日志分析  │ 指标分析   │
└──────────┴──────────┴───────────┘
```

## 🚀 技术栈

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Web** - RESTful API
- **Spring Data JPA** - 数据持久化
- **MySQL** - 关系型数据库
- **Redis** - 缓存与去重
- **飞书开放平台** - 通知推送

## 📦 项目结构

```
alert-aggregation-platform/
├── src/main/java/com/chenhao/alertagg/
│   ├── controller/        # REST API 控制器
│   │   ├── AlertWebhookController.java
│   │   └── AlertManagementController.java
│   ├── service/           # 业务逻辑层
│   │   ├── AlertService.java
│   │   ├── DeduplicationService.java
│   │   ├── AggregationService.java
│   │   └── AgentSchedulerService.java
│   ├── entity/            # 实体类
│   │   ├── Alert.java
│   │   └── AlertBatch.java
│   ├── repository/        # 数据访问层
│   ├── config/            # 配置类
│   └── util/              # 工具类
├── src/main/resources/
│   ├── application.yml    # 应用配置
│   └── schema.sql         # 数据库脚本
└── pom.xml
```

## 🔧 快速开始

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用
mvn spring-boot:run
```

访问：http://localhost:8080/health

## 🔄 开发工作流

本项目采用**三重审查工作流**确保代码质量：

1. **代码质量审查** - 编码规范、最佳实践
2. **安全审查** - 漏洞扫描、依赖安全
3. **性能审查** - 性能瓶颈、优化建议

每次代码提交都会触发：
- 🔍 三重 AI 并行审查
- 🔄 GitHub Actions CI/CD
- 📱 飞书实时通知

## 📊 API 文档

### 接收告警 Webhook
```
POST /api/webhook/alerts
Content-Type: application/json

{
  "alertname": "HighCPUUsage",
  "severity": "critical",
  "labels": {
    "service": "order-service",
    "instance": "192.168.1.100"
  },
  "annotations": {
    "summary": "CPU 使用率超过 90%"
  }
}
```

### 查询告警列表
```
GET /api/alerts?page=1&size=20&status=firing
```

### 查询聚合批次
```
GET /api/batches/{batchId}
```

## 🛠️ 配置说明

### 聚合策略配置
```yaml
alert:
  aggregation:
    group_wait: 30s      # 等待时间窗口
    max_count: 100       # 最大告警数量
    max_wait: 5m         # 最大等待时间
```

### A2A Agent 配置
```yaml
agent:
  endpoints:
    - name: root-cause-analyzer
      url: http://agent1:8081/analyze
    - name: log-analyzer
      url: http://agent2:8082/analyze
```

## 📝 开发计划

- [ ] 告警接收 Webhook
- [ ] 基于指纹的去重机制
- [ ] 攒批聚合策略
- [ ] A2A 协议调度器
- [ ] 飞书通知集成
- [ ] 可视化后台页面
- [ ] 告警规则配置

## 📄 License

MIT License

---
*中间件根因分析多智能体架构 - 告警聚合平台*
