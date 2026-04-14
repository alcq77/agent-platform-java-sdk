# AI Agent Java SDK (Preview)

[CI Compile Check](https://github.com/alcq77/agent-platform-java-sdk/actions/workflows/ci-compile.yml)

一个面向 Java 的轻量 Agent 开发工具包。  
目标是让项目通过 **SDK / Spring Boot Starter** 方式快速集成 Agent 能力，而不是先搭一整套独立平台。

> 当前仓库仍在慢速迭代中，会继续优化。欢迎反馈和建议。

## Why This Project

- 尽量少依赖：优先内嵌能力，减少额外部署成本
- 易扩展：模型、工具、会话存储都提供 SPI
- 可控：支持路由、熔断、工具调用安全校验
- 可落地：提供 Starter 和示例工程，便于直接接入

## Features

- 模型路由（逻辑模型、主备、加权、健康感知）
- 标准 tool-calling 回路（含参数安全校验）
- 会话管理（内置内存/文件系统 + Redis/JDBC 存储）
- 插件与技能目录加载（workspace + plugins + skills）
- Spring Boot 自动装配与健康检查（含 `cqgentSessionStore` Actuator 端点）

## Quick Start

### Spring Boot Starter

见：`[docs/product/quick-start-starter.md](docs/product/quick-start-starter.md)`

### Java SDK

见：`[docs/product/quick-start-sdk.md](docs/product/quick-start-sdk.md)`

## Examples

- `examples/java-sdk-demo`：纯 Java 最小示例
- `examples/spring-starter-demo`：Spring Boot 最小示例

## Project Layout

> 说明：源码主目录为 `cqgent`。

- `cqgent-java-sdk`：SDK（源码位于 `cqgent/product/cqgent-java-sdk`）
- `cqgent-spring-boot-starter`：Starter（源码位于 `cqgent/product/cqgent-spring-boot-starter`）
- `cqgent-core-engine`：核心执行逻辑（源码位于 `cqgent/product/cqgent-core-engine`）
- `cqgent-spi`：扩展接口（源码位于 `cqgent/product/cqgent-spi`）
- `cqgent-plugins`：插件示例（源码位于 `cqgent/product/cqgent-plugins`）

## Build

Requirements:

- JDK 21+
- Maven 3.9+

```bash
cd cqgent
mvn -q clean compile -DskipTests
```

## Docs

- [配置参考](docs/product/config-reference.md)
- [核心功能基线](docs/product/core-capabilities.md)
- [API 参考](docs/product/api-reference.md)
- [SPI 扩展](docs/product/spi-extension.md)
- [Workspace 规范](docs/product/workspace-layout.md)
- [迁移指南](docs/product/migration-guide.md)
- [发布治理](docs/product/release-governance.md)

## Roadmap

- 更完善的工具调用协议与观测
- 更细粒度的路由与熔断策略
- Redis / DB 会话存储插件
- 更完整的示例与自动化测试

## Contributing

欢迎通过 Issue / PR 提建议。  
如果你在接入过程中遇到问题，也欢迎直接提一个最小复现。