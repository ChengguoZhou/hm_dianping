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