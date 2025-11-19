# 图书管理系统

## 项目简介

这是一个基于Spring Boot开发的图书管理系统，提供图书管理、借阅记录管理、用户管理等功能。系统支持管理员和普通用户两种角色，实现了图书的添加、编辑、删除、查询以及借阅、归还等核心功能。

## 技术栈

- **后端框架**: Spring Boot 3.5.7
- **安全框架**: Spring Security
- **数据访问**: Spring Data JPA
- **模板引擎**: Thymeleaf
- **数据库**: MySQL 8.0
- **构建工具**: Maven
- **Java版本**: JDK 17
- **其他依赖**:
  - Apache POI (Excel导入导出)
  - OpenCSV (CSV文件处理)
  - Thymeleaf Layout Dialect (模板布局)

## 功能特性

### 用户功能
- **用户认证**：安全的登录/注销功能，基于Spring Security实现
- **图书浏览**：查看图书列表，支持搜索和筛选
- **图书详情**：查看图书的详细信息，包括封面、作者、简介等
- **借阅系统**：
  - 借阅图书（默认14天借阅期）
  - 借阅前确认提示，防止误操作
  - 查看个人借阅记录和状态
  - 归还已借阅图书
- **个人中心**：管理个人信息

### 管理员功能
- **图书管理**：
  - 添加新图书（包括封面上传）
  - 编辑现有图书信息
  - 删除图书
  - 批量导入/导出图书数据（Excel、CSV格式）
- **分类管理**：添加、编辑、删除图书分类
- **用户管理**：
  - 查看所有用户
  - 管理用户角色和权限
- **借阅管理**：
  - 查看所有借阅记录
  - 筛选和搜索借阅记录
  - 处理逾期记录
  - 统计借阅数据
- **数据管理**：支持Excel和CSV格式的数据导入导出

### 系统特性
- **响应式设计**：适配不同屏幕尺寸的设备
- **权限控制**：基于角色的访问控制（RBAC）
- **数据验证**：前端和后端双重数据验证
- **错误处理**：友好的错误提示和日志记录
- **文件上传**：支持图书封面图片上传

## 项目结构

```
src/
├── main/
│   ├── java/com/Southern/book/
│   │   ├── config/         # 配置类（安全配置、Web配置）
│   │   ├── controller/     # 控制器（REST API和页面控制器）
│   │   ├── entity/         # 实体类
│   │   ├── repository/     # 数据访问层
│   │   ├── service/        # 业务逻辑层
│   │   └── BookApplication.java  # 应用入口
│   ├── resources/
│   │   ├── application.properties  # 应用配置
│   │   ├── templates/      # Thymeleaf模板
│   │   └── static/         # 静态资源
│   └── test/               # 测试代码
└── uploads/                # 上传文件目录（图书封面等）
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- 现代Web浏览器

### 安装与运行

1. **克隆项目**（如果通过Git获取）
```bash
git clone https://github.com/your-repo/book-management-system.git
cd book-management-system
```

2. **配置数据库**
   - 创建MySQL数据库（例如：`book_management`）
   - 编辑 `src/main/resources/application.properties` 文件，配置数据库连接信息：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/book_management?useSSL=false&serverTimezone=UTC
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```

3. **导入初始数据**
   - 执行 `src/main/resources/static/book.sql` 文件中的SQL语句，初始化数据库结构和测试数据

4. **构建项目**
```bash
mvn clean install
```

5. **运行项目**
```bash
mvn spring-boot:run
```
或者使用生成的JAR文件：
```bash
java -jar target/book-0.0.1-SNAPSHOT.jar
```

6. **访问系统**
   - 打开浏览器访问 `http://localhost:8080`
   - 使用默认账号登录（见下方系统用户部分）

### 主要API接口

#### 图书相关
- `GET /books` - 获取图书列表
- `GET /books/view/{id}` - 查看图书详情
- `POST /books/add` - 添加图书（管理员）
- `POST /books/edit/{id}` - 编辑图书（管理员）
- `GET /books/delete/{id}` - 删除图书（管理员）

#### 借阅相关
- `GET /borrow/user/borrow?bookId={id}` - 用户借阅图书
- `GET /borrow/user/return/{id}` - 用户归还图书
- `GET /borrow/my-records` - 查看个人借阅记录
- `GET /borrow/records` - 查看所有借阅记录（管理员）

#### 用户相关
- `POST /login` - 用户登录
- `GET /logout` - 用户登出
- `GET /admin/users` - 查看所有用户（管理员）

### 使用说明

#### 图书借阅流程
1. 在图书列表页面找到想要借阅的图书
2. 点击图书旁的"借阅"按钮
3. 在弹出的确认对话框中确认借阅信息
4. 系统处理借阅请求并更新借阅状态
5. 借阅成功后，可在"我的借阅记录"中查看

#### 图书归还流程
1. 进入"我的借阅记录"页面
2. 找到需要归还的图书记录
3. 点击"归还"按钮
4. 系统确认归还并更新图书状态

#### 管理员操作指南
1. 以管理员身份登录系统
2. 通过左侧菜单访问各管理功能
3. 图书管理：可添加、编辑、删除图书，支持搜索和分类筛选
4. 用户管理：可查看所有用户信息
5. 借阅管理：可查看所有借阅记录，筛选逾期记录
6. 数据管理：支持Excel和CSV格式的数据导入导出

## 数据库初始化

在首次运行前，请确保已创建数据库并导入初始数据。可以使用项目中的SQL文件：`src/main/resources/static/book.sql`

## 系统用户

- **管理员账户**：
  - 用户名：admin
  - 密码：admin123

- **普通用户账户**：
  - 用户名：user
  - 密码：admin123

## 借阅规则

- 每本图书默认借阅期限为14天
- 借阅时系统会检查图书的可借数量
- 支持查看个人借阅记录和逾期信息

## 部署指南

### 开发环境部署
参考"快速开始"部分的安装与运行步骤。

### 生产环境部署

#### 1. 构建生产版本
```bash
mvn clean package -DskipTests
```

#### 2. 配置生产环境
- 创建独立的配置文件 `application-prod.properties`
- 配置生产数据库连接、日志级别等
- 设置安全的密钥和密码

#### 3. 部署到Tomcat
- 将生成的WAR文件（如果配置为WAR打包）复制到Tomcat的 `webapps` 目录
- 启动Tomcat服务器

#### 4. 部署为独立JAR
```bash
java -jar -Dspring.profiles.active=prod target/book-0.0.1-SNAPSHOT.jar
```

#### 5. 使用Docker部署（推荐）

创建 `Dockerfile`：
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/book-0.0.1-SNAPSHOT.jar /app/app.jar
COPY uploads /app/uploads
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

构建并运行Docker容器：
```bash
docker build -t book-management-system .
docker run -d -p 8080:8080 -v ./uploads:/app/uploads --name book-system book-management-system
```

## 注意事项

1. **文件上传**：
   - 上传的图书封面图片保存在项目根目录下的 `uploads` 文件夹中
   - 确保 `uploads` 目录有写入权限
   - 在生产环境中，建议配置独立的文件存储服务（如AWS S3）

2. **安全性**：
   - 系统使用Spring Security进行权限控制
   - 生产环境中请修改默认账号密码
   - 建议配置HTTPS

3. **性能优化**：
   - 对于大量数据，建议优化数据库查询
   - 考虑添加缓存机制（如Redis）

4. **备份策略**：
   - 定期备份数据库
   - 备份 `uploads` 目录中的文件

## 常见问题（FAQ）

### Q: 无法登录系统？
A: 请检查以下几点：
- 用户名和密码是否正确
- 数据库连接是否正常
- Spring Security配置是否正确

### Q: 无法上传图书封面？
A: 请检查：
- `uploads` 目录是否存在且有写入权限
- 文件大小是否超过限制
- 文件类型是否被允许

### Q: 借阅图书失败？
A: 可能的原因：
- 图书库存不足
- 用户借阅数量达到上限
- 系统错误，请检查日志

### Q: 数据库连接失败？
A: 请检查：
- MySQL服务是否运行
- 数据库连接配置是否正确
- 数据库用户是否有足够权限

## 故障排除

### 日志查看
系统日志默认输出到控制台。在生产环境中，可以配置日志文件：

在 `application.properties` 中添加：
```properties
logging.file.name=logs/book-system.log
logging.level.root=INFO
logging.level.com.Southern.book=DEBUG
```

### 常见错误及解决方案

1. **404错误**：页面不存在
   - 检查URL是否正确
   - 确认控制器映射是否正确配置

2. **500错误**：服务器内部错误
   - 查看系统日志获取详细错误信息
   - 检查数据库连接
   - 检查相关业务逻辑

3. **权限拒绝错误**：
   - 确认当前用户是否有足够权限
   - 检查Spring Security配置

4. **数据库错误**：
   - 检查SQL语法
   - 确认数据库表结构是否正确
   - 查看数据库连接配置

## 许可证

本项目仅供学习和参考使用。

## 联系方式

如有问题或建议，请联系项目维护者。

## 更新日志

### 1.0.0
- 初始版本发布
- 实现图书管理核心功能
- 实现用户借阅系统
- 实现管理员后台管理功能