# 黑马点评 Redis 学习项目

本项目是一个基于 `Spring Boot + MyBatis-Plus + MySQL + Redis` 的练手项目，主要用于学习 Redis 在真实业务中的常见用法，以及整理写代码过程中常用的 IntelliJ IDEA 快捷键。

课程地址：
`https://www.bilibili.com/video/BV1cr4y1671t`

## 项目使用人群

适合以下人群使用：

- 正在学习 Java 后端开发的初学者
- 想系统学习 Redis 常见业务场景的同学
- 正在跟练黑马点评实战项目的学生

## 项目简介

当前仓库是黑马点评项目的学习版后端，重点围绕登录认证、店铺查询和 Redis 缓存展开。根据现有 Github 提交记录，项目目前已经完成了以下阶段性内容：

- 初始化 Spring Boot 项目基础结构
- 完成登录认证基础功能
- 隐藏用户敏感信息
- 基于 Redis 实现共享 Session 登录
- 完成店铺查询、新增、更新和分页查询
- 添加登录拦截器和刷新 Token 拦截器
- 实现商铺类型缓存
- 通过缓存空对象解决缓存穿透
- 通过互斥思路避免缓存击穿

## 提交记录概览

根据当前提交历史，项目迭代主线如下：

| 提交日期 | Commit | 说明 |
| --- | --- | --- |
| 2026-05-10 | `f12ad66` | 初始化项目基础结构和登录认证功能 |
| 2026-05-11 | `79b4e73` | 隐藏用户敏感信息 |
| 2026-05-11 | `fb311db` | 基于 Redis 实现共享 session 登录 |
| 2026-05-14 | `1635e02` | 完成商铺服务、缓存与登录拦截功能 |
| 2026-05-14 | `9e3bd49` | 添加商铺类型 Redis 缓存 |
| 2026-05-14 | `bb3d133` | 解决缓存穿透，补充查询与配置 |
| 2026-05-15 | `675fdaf` | 店铺查询功能添加避免缓存击穿功能 |

## 项目架构

项目采用典型的单体分层架构：

- `controller`：对外提供 HTTP 接口
- `service`：编写业务逻辑
- `mapper`：负责数据库访问
- `entity`：数据库实体对象
- `dto`：接口传输对象
- `config`：Spring MVC、MyBatis 等配置
- `utils`：Redis 常量、拦截器、正则工具、用户上下文等通用组件
- `resources/db/hmdp.sql`：初始化数据库脚本
- `resources/application.yaml`：项目核心配置文件

当前后端核心技术栈如下：

- `Spring Boot 2.3.12.RELEASE`
- `MyBatis-Plus 3.4.3`
- `MySQL 5.7+`
- `Redis`
- `Lombok`
- `Hutool`

## 目录结构

```text
hm-dianping
├─ src/main/java/com/hmdp
│  ├─ config
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ mapper
│  ├─ service
│  └─ utils
├─ src/main/resources
│  ├─ db
│  ├─ mapper
│  └─ application.yaml
└─ pom.xml
```

## 开发环境要求

建议本地环境如下：

- JDK `1.8`
- Maven `3.6+`
- MySQL `5.7` 或兼容版本
- Redis `6.x` 或兼容版本
- IntelliJ IDEA

## 安装与部署

### 1. 克隆项目

```bash
git clone <your-repo-url>
cd hm-dianping
```

### 2. 初始化数据库

使用 MySQL 创建数据库：

```sql
CREATE DATABASE hmdp DEFAULT CHARACTER SET utf8mb4;
```

然后执行脚本：

`hm-dianping/src/main/resources/db/hmdp.sql`

### 3. 配置数据库和 Redis

修改配置文件：

`hm-dianping/src/main/resources/application.yaml`

当前项目默认配置如下，需要按你的本地环境修改：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/hmdp?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: root
  redis:
    host: 192.168.42.130
    port: 6379
    password: root
```

如果你本机部署 Redis，通常需要把 `redis.host` 改成：

```yaml
spring:
  redis:
    host: 127.0.0.1
```

### 4. 安装依赖

在项目根目录执行：

```bash
mvn clean install
```

### 5. 启动项目

方式一：使用 IDEA 直接启动

- 打开启动类 `HmDianPingApplication`
- 运行 `main` 方法

方式二：使用 Maven 启动

```bash
mvn spring-boot:run
```

启动成功后，服务默认运行在：

`http://localhost:8081`

## 常见部署说明

如果你是第一次部署这个项目，建议按下面顺序检查：

1. 先确认 MySQL 已启动
2. 再确认 Redis 已启动
3. 检查 `application.yaml` 中数据库和 Redis 地址是否正确
4. 确认 `hmdp.sql` 已成功导入
5. 最后再启动 Spring Boot 项目

## 常见问题

### 1. 中文乱码

项目中已经记录过这个问题。处理方式主要有两点：

- 数据库连接 URL 中添加 `characterEncoding=utf8`
- 开启 `server.servlet.encoding` 的 UTF-8 配置

当前配置文件中已经包含相关设置。

### 2. Git 无法读取提交记录

如果本地出现 `detected dubious ownership in repository`，可执行：

```bash
git config --global --add safe.directory E:/Projects/hm-dianping
```

## IDEA 常用快捷键

当前仓库原 README 中已经记录了以下快捷键：

| 快捷键              | 作用                      |
|------------------|-------------------------|
| `Ctrl + H`       | 查看类继承层级                 |
| `Alt + Insert`   | 快速生成构造器、getter、setter 等 |
| `Ctrl + Alt + B` | 查看接口或方法的具体实现            |
| `Ctrl + Alt + T` | 快速包裹 `if/try/for` 等结构   |
| `Ctrl + I`       | 快速实现接口方法                |
| `Ctrl + E`       | 打开最近使用的文件               |
| `Ctrl + D`       | 复制配置                    |

补充一些 Java 开发中更常用的 IDEA 快捷键：

| 快捷键 | 作用 |
| --- | --- |
| `Shift + Shift` | 全局搜索文件、类、方法、配置 |
| `Ctrl + N` | 查找类 |
| `Ctrl + Shift + N` | 查找文件 |
| `Ctrl + Alt + L` | 格式化代码 |
| `Ctrl + /` | 单行注释 |
| `Ctrl + Shift + /` | 多行注释 |
| `Alt + Enter` | 快速修复、导包、代码提示处理 |
| `Ctrl + P` | 查看方法参数提示 |
| `Ctrl + Q` | 查看方法文档 |
| `Ctrl + B` 或 `Ctrl + 鼠标左键` | 跳转到定义 |
| `Ctrl + Alt + Left` | 返回上一次光标位置 |
| `Ctrl + F12` | 查看当前文件结构 |
| `Ctrl + D` | 复制当前行 |
| `Ctrl + Y` | 删除当前行 |
| `Shift + F6` | 重命名变量、方法、类 |
| `Alt + 1` | 打开或关闭项目目录栏 |
| `Ctrl + Shift + F` | 全局文本搜索 |
| `Ctrl + Shift + R` | 全局文本替换 |
| `Ctrl + Alt + V` | 快速抽取变量 |
| `Ctrl + Alt + M` | 快速抽取方法 |

## 学习建议

这个项目更适合按“功能点 + Redis 场景”来学习：

1. 先看登录流程和拦截器执行过程
2. 再看商铺查询如何接入 Redis 缓存
3. 然后理解缓存穿透与缓存击穿的处理方式
4. 最后结合提交记录回顾每一步代码演进

## 说明

这是一个偏学习记录型项目，README 除了介绍项目本身，也额外整理了开发时常用的 IDEA 快捷键，方便后续复习和查阅。
