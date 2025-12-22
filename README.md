🚀🚀🚀Telegram TDLib installation and configuration🚀🚀🚀
1. Jar is configured in /libs
2. Install jar using command mvn install:install-file -Dfile=libs/spring-boot-starter-telegram-1.18.0.jar -DgroupId=dev.voroby -DartifactId=spring-boot-starter-telegram -Dversion=1.18.0 -Dpackaging=jar
3. Download TDLib shared library from https://tdlib.github.io/td/build.html?language=Java
```
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y make git zlib1g-dev libssl-dev gperf php-cli cmake default-jdk g++

rm -rf td
git clone https://github.com/tdlib/td.git
cd td
git checkout 5c77c4692c28eb48a68ef1c1eeb1b1d732d507d3
rm -rf build
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX:PATH=../example/java/td \
      -DTD_ENABLE_JNI=ON ..
cmake --build . --target install
cd ..
cd example/java
rm -rf build
mkdir build
cd build 
cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX:PATH=../../../tdlib -DTd_DIR:PATH=$(readlink -e ../td/lib/cmake/Td) ..
cmake --build . --target install
cd ../../..

copy tdlib/bin/libtdjni.so to /libs/native root dir

-Djava.library.path=/home/vkhomyn/projects/sociallabs/libs/native set up IntelljIdea VM options 
```


# Workflow Automation System

Enterprise-grade workflow automation platform аналогічний до n8n, побудований на Spring Boot та Vue 3.

## 🏗️ Архітектура

### Backend Stack
- **Spring Boot 3.2** - Core framework
- **PostgreSQL** - Database
- **Hibernate/JPA** - ORM
- **Flyway** - Database migrations
- **Maven** - Build tool

### Frontend Stack
- **Vue 3** - UI framework
- **Vue Flow** - Workflow canvas
- **Pinia** - State management
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Vite** - Build tool

## 📁 Структура Проекту

```
workflow-automation/
├── backend/
│   ├── src/main/java/com/workflow/
│   │   ├── domain/              # Entities & Repositories
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   └── repository/
│   │   │
│   │   ├── node/                # Node System
│   │   │   ├── core/            # Core abstractions
│   │   │   ├── base/            # Base node types
│   │   │   ├── parameter/       # Parameter types
│   │   │   ├── nodes/           # Concrete nodes
│   │   │   └── factory/
│   │   │
│   │   ├── execution/           # Execution Engine
│   │   │   ├── engine/
│   │   │   ├── strategy/
│   │   │   └── context/
│   │   │
│   │   ├── service/             # Business Logic
│   │   ├── controller/          # REST API
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── config/              # Configuration
│   │   ├── security/            # Security & Encryption
│   │   └── exception/           # Exception handling
│   │
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│
└── frontend/
    ├── src/
    │   ├── components/
    │   ├── stores/
    │   ├── services/
    │   ├── types/
    │   └── views/
    └── package.json
```

## 🎯 Основні Концепції

### 1. Node System (Система Нод)

Система нод побудована за принципами ООП з використанням:
- **Дженеріки** - для типобезпечних параметрів
- **Поліморфізм** - базові класи для різних типів нод
- **Інкапсуляція** - приватна логіка в базових класах
- **Абстракція** - interfaces та abstract classes

#### Типи Нод

**Trigger Nodes** - запускають workflow:
```java
public abstract class AbstractTriggerNode extends AbstractNode {
    public abstract boolean activate(ExecutionContext context);
    public abstract void deactivate(ExecutionContext context);
}
```

**Action Nodes** - виконують дії:
```java
public abstract class AbstractActionNode extends AbstractNode {
    protected abstract Map<String, Object> processItem(
        Map<String, Object> item,
        ExecutionContext context
    );
}
```

**Transform Nodes** - трансформують дані:
```java
public abstract class AbstractTransformNode extends AbstractNode {
    protected abstract List<Map<String, Object>> transform(
        List<Map<String, Object>> inputData,
        ExecutionContext context
    );
}
```

### 2. Parameter System (Система Параметрів)

Generic система параметрів з типобезпекою:

```java
public abstract class NodeParameter<T> {
    public abstract boolean validate(T value);
    public abstract T parseValue(Object rawValue);
    public abstract String getType();
}
```

Конкретні типи:
- `StringParameter` - текстові поля
- `NumberParameter` - числові значення
- `OptionsParameter` - dropdown селектори
- `JsonParameter` - JSON дані
- `CredentialParameter` - посилання на credentials
- `CollectionParameter` - масиви об'єктів

### 3. Execution Engine (Engine Виконання)

**WorkflowEngine** - головний клас для виконання:
- Будує граф залежностей нод
- Визначає порядок виконання
- Керує передачею даних між нодами
- Обробляє помилки та логування

**ExecutionContext** - контекст виконання ноди:
```java
ExecutionContext.builder()
    .executionId(id)
    .nodeId(nodeId)
    .parameters(params)
    .credentials(creds)
    .inputData(data)
    .workflowData(sharedData)
    .build();
```

### 4. Database Schema

**Головні таблиці:**
- `workflows` - визначення workflows
- `nodes` - типи/шаблони нод
- `node_instances` - ноди у workflows
- `connections` - з'єднання між нодами
- `credentials` - зашифровані credentials
- `workflow_executions` - історія виконань
- `execution_logs` - логи кожної ноди

## 🚀 Швидкий Старт

### Backend Setup

1. **Встановити PostgreSQL**
```bash
createdb workflow_db
```

2. **Налаштувати application.yml**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: your_username
    password: your_password
```

3. **Запустити Backend**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Setup

1. **Встановити залежності**
```bash
cd frontend
npm install
```

2. **Запустити Dev Server**
```bash
npm run dev
```

Фронтенд буде доступний на `http://localhost:5173`
Backend API на `http://localhost:8080`

## 📝 Створення Власної Ноди

### 1. Створити Definition

```java
public class MyCustomNodeDefinition extends NodeDefinition {
    
    public MyCustomNodeDefinition() {
        this.type = "myapp.customAction";
        this.name = "My Custom Action";
        this.description = "Does something custom";
        this.category = "Custom";
        this.nodeType = NodeType.ACTION;
        
        defineParameters();
        defineOutputs();
    }
    
    @Override
    protected void defineParameters() {
        addParameter(StringParameter.builder()
            .name("message")
            .displayName("Message")
            .description("Message to process")
            .required(true)
            .build());
    }
    
    @Override
    protected void defineOutputs() {
        addOutput("main", OutputDefinition.builder()
            .name("main")
            .displayName("Main Output")
            .type("main")
            .build());
    }
    
    @Override
    public Class<? extends NodeExecutor> getExecutorClass() {
        return MyCustomNodeExecutor.class;
    }
}
```

### 2. Створити Executor

```java
@Slf4j
@Component
public class MyCustomNodeExecutor extends AbstractActionNode {
    
    public MyCustomNodeExecutor() {
        super("myapp.customAction");
    }
    
    @Override
    protected Map<String, Object> processItem(
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {
        
        String message = context.getParameter("message", String.class);
        
        // Your custom logic here
        Map<String, Object> result = new HashMap<>();
        result.put("processedMessage", message.toUpperCase());
        result.put("timestamp", Instant.now().toString());
        
        return result;
    }
}
```

### 3. Зареєструвати Ноду

```java
@Configuration
public class NodeRegistrationConfig {
    
    @PostConstruct
    public void registerNodes() {
        NodeRegistry registry = NodeRegistry.getInstance();
        
        // Register your custom node
        registry.register(new MyCustomNodeDefinition());
    }
}
```

### 4. Додати до Database

```sql
INSERT INTO nodes (
    type, name, description, category, icon, color,
    node_type, parameter_schema, executor_class
) VALUES (
    'myapp.customAction',
    'My Custom Action',
    'Does something custom',
    'Custom',
    'fas fa-cog',
    '#FF6B6B',
    'ACTION',
    '{}',
    'com.workflow.node.nodes.custom.MyCustomNodeExecutor'
);
```

## 🔐 Security

### Credential Encryption

Всі credentials зашифровані за допомогою AES-256-GCM:

```java
@Service
public class CredentialEncryption {
    
    public String encrypt(String data) {
        // AES-256-GCM encryption
    }
    
    public String decrypt(String encryptedData) {
        // Decryption
    }
}
```

Ключ шифрування задається через environment variable:
```bash
ENCRYPTION_KEY=your_32_character_encryption_key
```

## 🧪 Testing

### Unit Tests

```java
@SpringBootTest
class TelegramNodeTest {
    
    @Test
    void testSendMessage() {
        ExecutionContext context = ExecutionContext.builder()
            .parameters(Map.of(
                "chatId", "123456",
                "text", "Test message"
            ))
            .credentials(Map.of(
                "botToken", "test_token"
            ))
            .build();
            
        TelegramSendMessageExecutor executor = 
            new TelegramSendMessageExecutor();
            
        NodeResult result = executor.execute(context);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
```

## 📊 API Endpoints

### Workflows
- `GET /api/v1/workflows` - Get all workflows
- `GET /api/v1/workflows/{id}` - Get workflow by ID
- `POST /api/v1/workflows` - Create workflow
- `PUT /api/v1/workflows/{id}` - Update workflow
- `DELETE /api/v1/workflows/{id}` - Delete workflow
- `POST /api/v1/workflows/{id}/toggle` - Activate/deactivate
- `POST /api/v1/workflows/{id}/execute` - Execute manually

### Nodes
- `GET /api/v1/nodes/available` - Get available nodes
- `GET /api/v1/nodes/{type}` - Get node definition
- `GET /api/v1/nodes/category/{category}` - Get nodes by category

### Credentials
- `GET /api/v1/credentials` - Get all credentials
- `POST /api/v1/credentials` - Create credential
- `PUT /api/v1/credentials/{id}` - Update credential
- `DELETE /api/v1/credentials/{id}` - Delete credential

### Executions
- `GET /api/v1/executions` - Get all executions
- `GET /api/v1/executions/{id}` - Get execution by ID
- `POST /api/v1/executions/{id}/stop` - Stop execution
- `POST /api/v1/executions/{id}/retry` - Retry execution

### Webhooks
- `POST /api/v1/webhooks/{workflowId}/**` - Webhook endpoint

## 🎨 Frontend Development

### Workflow Canvas

```vue
<template>
  <WorkflowCanvas 
    :workflow="currentWorkflow"
    @node-added="handleNodeAdded"
    @connection-created="handleConnectionCreated"
  />
</template>
```

### Node Panel

```vue
<template>
  <NodePanel 
    :available-nodes="availableNodes"
    @node-selected="addNodeToCanvas"
  />
</template>
```

## 🔧 Configuration

### Environment Variables

```bash
# Database
DB_USERNAME=workflow_user
DB_PASSWORD=secure_password

# Security
ENCRYPTION_KEY=your_32_character_encryption_key

# Webhook
WEBHOOK_BASE_URL=https://your-domain.com

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### Application Settings

```yaml
workflow:
  execution:
    thread-pool-size: 10
    max-concurrent-executions: 100
    timeout-minutes: 60
```

## 📈 Performance Considerations

1. **Connection Pooling** - HikariCP налаштований для оптимальної роботи
2. **Batch Processing** - Hibernate batch insert/update
3. **Async Execution** - Thread pool для паралельного виконання
4. **Caching** - Caching для node definitions
5. **Indexing** - Database indexes на часто використовувані поля

## 🚀 Production Deployment

### Docker Compose

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: workflow_db
      POSTGRES_USER: workflow_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_USERNAME: workflow_user
      DB_PASSWORD: ${DB_PASSWORD}
      ENCRYPTION_KEY: ${ENCRYPTION_KEY}
    depends_on:
      - postgres
    ports:
      - "8080:8080"
      
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Vue 3 Documentation](https://vuejs.org/)
- [Vue Flow Documentation](https://vueflow.dev/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

MIT License - see LICENSE file for details

## 👥 Authors

Built with ❤️ using modern Java and Vue.js best practices