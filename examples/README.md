# Examples

- `java-sdk-demo`：纯 Java SDK 示例
- `spring-starter-demo`：Spring Boot Starter 示例

## 先安装本地依赖

```bash
cd ../cqgent
mvn -q clean install -DskipTests
```

## 运行 Java SDK 示例

```bash
cd ../examples/java-sdk-demo
mvn -q compile exec:java -Dexec.mainClass=demo.Main
```

## 运行 Starter 示例

```bash
cd ../examples/spring-starter-demo
mvn spring-boot:run
```

启动后可先访问：

- [http://localhost:18080/onboarding](http://localhost:18080/onboarding)
- [http://localhost:18080/demo/chat?q=你好](http://localhost:18080/demo/chat?q=你好)

