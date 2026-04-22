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
- 按 `taskType/tags` 的多模型动态分流（P1）
- 运行时观测：`EmbeddedAgentClient.runtimeMetrics()` 合并熔断/路由失败分类、`promptTemplates` 打点，以及 LangChain4j
  工具层的同步/流式调用次数、`toolInvocations`、`toolValidationFailures`、`toolExecutionFailures`
- 基于 LangChain4j 的 Agent 主链与 tool-calling 适配层
- Advisor/Interceptor 统一增强链（提示注入、上下文裁剪、安全过滤、审计可插拔）
- RAG 基础能力（文档导入、切分、embedding、检索、上下文注入）
- RAG 本地知识库目录导入（`workspace/knowledge` 下 `md/txt`）
- RAG 增量索引与清单持久化（`workspace/rag/index-manifest.json`）
- Starter 支持 `agent.product.rag.*` 配置化自动装配 RAG Advisor（含 metadata/source 过滤）
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

> 说明：主工程目录为 `cq-agent-parent/cq-agent`。

- `cqagent-java-sdk`：SDK（`cq-agent-parent/cq-agent/cqagent-java-sdk`）
- `cqagent-spring-boot-starter`：Starter（`cq-agent-parent/cq-agent/cqagent-spring-boot-starter`）
- `cqagent-core-engine`：基于 LangChain4j 的核心运行时（`cq-agent-parent/cq-agent/cqagent-core-engine`）
- `cqagent-spi`：扩展接口（`cq-agent-parent/cq-agent/cqagent-spi`）
- `cqagent-plugins`：插件示例（`cq-agent-parent/cq-agent/cqagent-plugins`）

## Build

Requirements:

- JDK 21+
- Maven 3.9+

```bash
cd cq-agent-parent
mvn -q clean compile -DskipTests
```

## Code Conventions

- 新增运行时链路代码时，优先补齐类/方法注释，说明“职责边界 + 调用顺序”
- 关键分支（路由、重试、熔断、工具调用）增加简短行内注释，便于排障
- 对外 SPI 保持语义稳定；破坏性调整需给出兼容层或迁移提示

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
