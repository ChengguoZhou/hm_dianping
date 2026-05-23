# 黑马点评 Redis 学习项目

本项目是一个基于 `Spring Boot + MyBatis-Plus + MySQL + Redis` 的后端练习项目，主要用于学习 Redis 在真实业务场景中的常见用法，包括登录认证、缓存优化、分布式锁、秒杀下单和异步订单处理等内容。

课程地址：`https://www.bilibili.com/video/BV1cr4y1671t`

## 项目适用人群

- 正在学习 Java 后端开发的初学者
- 想系统学习 Redis 常见业务场景的同学
- 正在跟练黑马点评实战项目的学习者
- 想复习缓存、分布式锁、秒杀系统基础方案的开发者

## 项目简介

当前仓库是黑马点评项目的学习版后端，核心围绕登录认证、商铺查询、Redis 缓存、优惠券秒杀和订单并发控制展开。

项目进度参考课程课件《Redis 企业实战》的章节组织如下：

| 课程章节 | 当前进度 | 项目实现重点 |
| --- | --- | --- |
| 01 短信登录 | 已完成 | 手机号验证码登录、Redis 共享 Session、登录拦截器、Token 刷新、`UserDTO` 用户脱敏 |
| 02 商户查询缓存 | 已完成 | 商铺查询缓存、商铺类型缓存、缓存空值解决穿透、互斥锁和逻辑过期解决击穿、`CacheClient` 抽取 |
| 03 优惠券秒杀 | 进行中，核心流程已完成 | 全局唯一 ID、秒杀下单、乐观锁扣库存、一人一单、Redis 分布式锁、Lua 原子校验、Redisson、阻塞队列异步下单 |
| 04 达人探店 | 待完善 | 已保留 `BlogController`、`BlogService` 等基础结构，发布笔记、点赞排行榜等逻辑尚未展开 |
| 05 好友关注 | 待完善 | 已保留关注相关表、实体、Mapper、Controller，关注、取关、共同关注和 Feed 流尚未展开 |
| 06 附近商户 | 待完善 | 当前已有按商铺类型分页查询，GEO 坐标导入和附近商户搜索尚未展开 |
| 07 用户签到 | 未开始 | BitMap 签到和连续签到统计尚未实现 |
| 08 UV 统计 | 未开始 | HyperLogLog UV 统计尚未实现 |

当前代码的主体学习内容集中在前三章：先用 Redis 解决登录态共享，再围绕商铺查询学习企业缓存问题，最后进入秒杀场景，逐步引入全局 ID、锁、Lua 脚本和异步订单处理。

## 提交记录概览

根据当前提交历史，项目迭代可以按课程章节归纳为：

| 课程阶段 | 相关提交 | 说明 |
| --- | --- | --- |
| 01 短信登录 | `fb311db` | 基于 Redis 实现共享 Session 登录，完成登录态保存、Token 校验和用户信息脱敏 |
| 02 商户查询缓存 | `1635e02`、`9e3bd49`、`bb3d133`、`675fdaf`、`828f2f7`、`bb0e500` | 完成商铺和商铺类型缓存，依次处理缓存穿透、缓存击穿、逻辑过期，并抽取 `CacheClient` |
| 03 优惠券秒杀：基础下单 | `054fc70`、`1c207cd` | 完成全局 ID、秒杀下单、乐观锁扣库存、一人一单和事务代理处理 |
| 03 优惠券秒杀：分布式并发控制 | `3c52bc2`、`c968a2d`、`93087c4` | 从手写 Redis 锁演进到 Lua 安全解锁，再集成 Redisson 分布式锁 |
| 03 优惠券秒杀：Redis 优化 | `034985e` | 使用 Lua 脚本完成库存和一人一单的原子校验，并通过阻塞队列异步创建订单 |

## 项目结构

项目采用典型的单体分层架构：

- `controller`：对外提供 HTTP 接口
- `service`：编写核心业务逻辑
- `mapper`：负责数据库访问
- `entity`：数据库实体对象
- `dto`：接口传输对象
- `config`：Spring MVC、MyBatis、Redisson 等配置
- `utils`：Redis 常量、拦截器、缓存工具、锁工具、ID 生成器等通用组件
- `resources/db/hmdp.sql`：数据库初始化脚本
- `resources/lua`：Redis Lua 脚本
- `resources/application.yaml`：项目核心配置文件

当前后端核心技术栈如下：

- `Spring Boot 2.3.12.RELEASE`
- `MyBatis-Plus 3.4.3`
- `MySQL 5.7+`
- `Redis 6.x+`
- `Redisson 3.13.6`
- `Lombok`
- `Hutool`
- `AspectJ`

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
│  │  └─ impl
│  └─ utils
├─ src/main/resources
│  ├─ db
│  ├─ lua
│  ├─ mapper
│  └─ application.yaml
├─ src/test/java/com/hmdp
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
cd hm-dianping/hm-dianping
```

### 2. 初始化数据库

使用 MySQL 创建数据库：

```sql
CREATE DATABASE hmdp DEFAULT CHARACTER SET utf8mb4;
```

然后执行初始化脚本：

```text
src/main/resources/db/hmdp.sql
```

### 3. 配置数据库和 Redis

修改配置文件：

```text
src/main/resources/application.yaml
```

当前项目默认配置示例：

```yaml
server:
  port: 8081

spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/hmdp?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: root
  redis:
    host: 192.168.42.130
    port: 6379
    password: root
```

如果 Redis 部署在本机，通常需要改成：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

如果本地 Redis 没有密码，需要删除或注释 `spring.redis.password`。

### 4. 安装依赖

在项目模块目录执行：

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

启动成功后，后端默认运行在：

```text
http://localhost:8081
```

## 主要接口

### 用户相关

- `POST /user/code?phone={phone}`：发送验证码，当前项目用日志打印验证码代替真实短信
- `POST /user/login`：手机号验证码登录
- `POST /user/logout`：登出接口，目前仍为待完善状态
- `GET /user/me`：获取当前登录用户
- `GET /user/info/{id}`：获取用户详情

### 商铺相关

- `GET /shop/{id}`：根据 ID 查询商铺，已接入 Redis 缓存
- `POST /shop`：新增商铺
- `PUT /shop`：更新商铺并删除缓存
- `GET /shop/of/type`：按类型分页查询商铺
- `GET /shop/of/name`：按名称分页查询商铺
- `GET /shop-type/list`：查询商铺类型列表，已接入 Redis 缓存

### 优惠券和秒杀相关

- `POST /voucher`：新增普通优惠券
- `POST /voucher/seckill`：新增秒杀券，并同步库存到 Redis
- `GET /voucher/list/{shopId}`：查询店铺优惠券
- `POST /voucher-order/seckill/{id}`：秒杀下单

## 当前秒杀流程

当前秒杀流程采用“Redis 预校验 + 异步落库”的实现方式：

1. 用户请求 `POST /voucher-order/seckill/{id}`
2. 后端获取当前登录用户 ID
3. 执行 `lua/seckill.lua`
4. Lua 脚本在 Redis 中原子判断库存和一人一单
5. 校验通过后扣减 Redis 库存并记录用户已下单
6. 后端生成全局唯一订单 ID
7. 将订单对象放入本地阻塞队列
8. 后台线程从队列取出订单
9. 使用 Redisson 按用户 ID 加锁，兜底防止重复下单
10. 通过事务方法扣减数据库库存并保存订单

需要注意：当前异步队列是 JVM 本地阻塞队列，项目重启会丢失尚未处理的订单任务。后续可以升级为 Redis Stream 或消息队列。

## 常见部署检查

如果是第一次部署，建议按下面顺序检查：

1. 确认 MySQL 已启动
2. 确认 Redis 已启动
3. 确认 `application.yaml` 中数据库地址、用户名、密码正确
4. 确认 Redis 地址、端口、密码正确
5. 确认 `hmdp.sql` 已成功导入
6. 确认秒杀券库存已同步到 Redis：`seckill:stock:{voucherId}`
7. 最后启动 Spring Boot 项目

## 常见问题

### 1. 中文乱码

处理方式主要有两点：

- 数据库连接 URL 中添加 `characterEncoding=utf8`
- 开启 `server.servlet.encoding` 的 UTF-8 配置

当前 `application.yaml` 中已经包含相关配置。

### 2. 获取验证码后手机没有短信

本项目没有接入真实短信服务。验证码会保存到 Redis，并通过后端日志打印：

```text
发送短信验证码成功，验证码:123456
```

### 3. 前端请求不到后端接口

后端默认端口是 `8081`。如果前端运行在 `localhost:8080`，需要确认前端请求或 nginx 代理转发到了：

```text
http://localhost:8081
```

### 4. Lua 脚本报 `ARGV[1] nil` 或 `redis.call nil`

Redis Lua 脚本中不能写下面这种代码：

```lua
redis = {}
KEYS = {}
ARGV = {}
```

这些变量是 Redis 在执行脚本时自动注入的，手动赋值会覆盖运行时对象，导致参数丢失或 `redis.call` 变成 `nil`。

如果曾经执行过覆盖 `redis` 的脚本，建议重启 Redis 服务恢复 Lua 运行环境。

### 5. Git 无法读取提交记录

如果本地出现 `detected dubious ownership in repository`，可执行：

```bash
git config --global --add safe.directory E:/Projects/hm-dianping
```

## IDEA 常用快捷键

| 快捷键 | 作用 |
| --- | --- |
| `Ctrl + H` | 查看类继承层级 |
| `Alt + Insert` | 快速生成构造器、getter、setter 等 |
| `Ctrl + Alt + B` | 查看接口或方法的具体实现 |
| `Ctrl + Alt + T` | 快速包裹 `if`、`try`、`for` 等结构 |
| `Ctrl + I` | 快速实现接口方法 |
| `Ctrl + E` | 打开最近使用的文件 |
| `Ctrl + D` | 复制当前行 |
| `Ctrl + Alt + L` | 格式化代码 |
| `Ctrl + Shift + F` | 全局文本搜索 |
| `Ctrl + Shift + R` | 全局文本替换 |
| `Ctrl + Alt + V` | 快速抽取变量 |
| `Ctrl + Alt + M` | 快速抽取方法 |
| `Shift + F6` | 重命名变量、方法、类 |

## 学习建议

这个项目适合按课程章节推进：

1. 先看“短信登录”，理解验证码、Token、Redis 共享 Session 和拦截器链路
2. 再看“商户查询缓存”，理解缓存穿透、缓存击穿和逻辑过期
3. 然后看 `CacheClient`，理解缓存逻辑如何抽象复用
4. 接着看“优惠券秒杀”，理解库存扣减、一人一单、事务代理和并发安全
5. 最后看 Lua 脚本、Redisson 分布式锁和异步订单处理，为后续 Redis Stream 改造做准备

## 说明

这是一个偏学习记录型项目。README 主要用于记录当前实现进度、部署方式、常见问题和复习路径，方便后续继续完善 Redis Stream、达人探店、点赞、关注、Feed 流、附近商户、用户签到和 UV 统计等功能。
