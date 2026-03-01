# Triple Guard Spring 🔒

三重审查工作流验证项目 - Spring Boot Web 应用

## 📋 项目概述

这是一个用于验证自动化代码审查工作流的示例项目，采用三重审查机制：

- **第一重：代码质量审查** - 编码规范、最佳实践
- **第二重：安全审查** - 漏洞扫描、依赖安全
- **第三重：性能审查** - 性能瓶颈、优化建议

## 🚀 技术栈

- Java 17
- Spring Boot 3.2.0
- Maven

## 📦 项目结构

```
triple-guard-spring/
├── src/
│   ├── main/
│   │   ├── java/com/chenhao/tripleguard/
│   │   └── resources/
│   └── test/
├── .github/workflows/
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

## 🔄 工作流

1. **代码提交** → 触发三重审查
2. **并行审查** → 3个AI代理同时审查
3. **汇总报告** → 生成综合审查报告
4. **CI/CD** → GitHub Actions 自动构建
5. **飞书通知** → 全流程状态推送

## 📊 审查维度

| 审查类型 | 检查项 |
|---------|--------|
| 代码质量 | 命名规范、代码重复、复杂度 |
| 安全审查 | SQL注入、XSS、敏感信息泄露 |
| 性能审查 | N+1查询、内存泄漏、缓存优化 |

---
*Created with ❤️ by OpenClaw Triple Review Workflow*
