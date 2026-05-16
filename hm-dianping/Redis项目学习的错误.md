# 遇到的问题

1.通过Postman调http://localhost:8081/shop接口更新店铺信息，结果保存到数据库的数据中文出现“？”乱码

> 解决：1、application.yaml文件第13行添加characterEncoding=utf8（大概率添加这个就不出现乱码问题了）
>
> ```yaml
> url: jdbc:mysql://127.0.0.1:3306/hmdp?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
> ```
>
> 2.同样是application.yaml添加server.servlet.encoding相关属性
>
> ```yaml
> server:
>   port: 8081
>   servlet:
>     encoding:
>       charset: utf-8
>       enabled: true
>       force: true
> ```

# 琐碎的小知识点

## 1.在CacheClient工具类中，我们使用了泛型。什么是泛型以及泛型的适用场景和好处是什么？

> Java 泛型就是用 `<T>`、`<E>`、`<K,V>` 这类类型占位符，让<u>类、接口、方法</u>可以适配不同类型，同时保证类型安全。

## 2.在CacheClient中，我们使用了有参、有返回值的函数式接口Function。还有哪些其他类型的函数式接口？

| 场景                 | 函数式接口            | 调用方法   |
| -------------------- | :-------------------- | ---------- |
| 有参有返回值         | `Function<T, R>`      | `apply()`  |
| 有参无返回值         | `Consumer<T>`         | `accept()` |
| 无参有返回值         | `Supplier<T>`         | `get()`    |
| 有参返回 boolean     | `Predicate<T>`        | `test()`   |
| 两参有返回值         | `BiFunction<T, U, R>` | `apply()`  |
| 两参无返回值         | `BiConsumer<T, U>`    | `accept()` |
| 一参一返回，类型相同 | `UnaryOperator<T>`    | `apply()`  |
| 两参一返回，类型相同 | `BinaryOperator<T>`   | `apply()`  |

## 3.在ShopServiceImpl实现类中，我们通过this::getById这种lambda表达式简化的写法调用CacheClient工具类。在Java中，lambda表达式是什么？以及有哪些可以简化的写法？

> lambda表达式可以理解成<u>把一段函数逻辑当作参数传递</u>。
>
> **基本格式：**参数 -> 方法体
>
> 例如：id -> getById(id)
>
> 什么时候可以简化？
>
> 1.省略参数类型
>
> ```java
> Function<Long, Shop> func = (Long id) -> {
>     return getById(id);
> };
> # 省略后(Java 可以根据 Function<Long, Shop> 推断出 id 是 Long 类型。)
> Function<Long, Shop> func = (id) -> {
>     return getById(id);
> };
> ```
>
> 2.只有一个参数时，可以省略小括号
>
> ```java
> # 一个参数，可以省略小括号
> id -> getById(id)
> # 有两个参数，不能省略小括号
> (a, b) -> a + b
> ```
>
> 3.方法体只有一行时，可以省略 `{}` 和 `return`且必须同时省略
>
> ```java
> # 简化前
> Function<Long, Shop> func = id -> {
>     return getById(id);
> };
> # 简化后
> Function<Long, Shop> func = id -> getById(id); 
> ```
>
> 4.参数 -> 对象.方法(参数) 简化为 对象::实例方法