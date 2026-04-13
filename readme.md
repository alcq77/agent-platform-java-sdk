# aiAgent Jar 产品

面向 Java 项目的智能体开发组件，支持以 Jar 方式快速集成，不依赖独立微服务平台。

## 产品形态

- `product-java-sdk`：纯 Java 接入（非 Spring 项目）
- `product-spring-boot-starter`：Spring Boot 自动装配接入
- `product-core-engine`：内嵌执行内核（会话、路由、工具）
- `product-spi`：模型提供方/工具/会话存储扩展接口
- `product-plugins`：可选插件集合（示例：`product-plugin-time`）

## 5分钟上手

### 1) Spring Boot Starter

文档：[`docs/product/quick-start-starter.md`](docs/product/quick-start-starter.md)

### 2) Java SDK

文档：[`docs/product/quick-start-sdk.md`](docs/product/quick-start-sdk.md)

## 模块结构

仓库主工程：`agent-platform`

- `common/common-core`：基础通用能力
- `agent/agent-api`：Agent 请求响应契约
- `model/model-api`：Model 请求响应契约
- `product/product-spi`：扩展 SPI
- `product/product-core-engine`：内核实现
- `product/product-java-sdk`：SDK 分发
- `product/product-spring-boot-starter`：Starter 分发
- `product/product-test-kit`：最小测试夹具

## 核心能力

- **模型路由**：逻辑模型映射、主备候选、加权顺序、健康感知剔除
- **会话能力**：内置会话管理与多轮上下文
- **工具扩展**：通过 `ProductTool` 增加业务工具
- **模型扩展**：通过 `ProductModelProvider` 增加任意厂商或协议
- **Workspace 初始化**：Starter 启动时自动初始化 `workspace` 目录与基础文件

## 文档导航

- [架构设计](01-architecture-design.md)
- [技术栈说明](02-tech-stack-evaluation.md)
- [任务拆分](03-task-breakdown.md)
- [数据与事件](04-database-events.md)
- [开发规范](05-development-standards.md)
- [配置参考](docs/product/config-reference.md)
- [API 参考](docs/product/api-reference.md)
- [SPI 扩展](docs/product/spi-extension.md)
- [Workspace 规范](docs/product/workspace-layout.md)
- [迁移指南](docs/product/migration-guide.md)
- [发布治理](docs/product/release-governance.md)

## 构建

要求：

- JDK 21+
- Maven 3.9+

执行：

```bash
cd agent-platform
mvn -q clean compile -DskipTests
```

## 示例

- `examples/java-sdk-demo`：纯 Java 代码示例
- `examples/spring-starter-demo`：Starter 配置与控制器示例

## 脚本

- `scripts/run-examples.ps1`：安装依赖并运行示例
- `scripts/preflight-check.ps1`：发布前编译与测试检查

## 路线图

- 标准 tool-calling 闭环
- 路由熔断与动态降权
- Redis/DB 会话存储插件
- Starter 与 SDK 示例工程完善
