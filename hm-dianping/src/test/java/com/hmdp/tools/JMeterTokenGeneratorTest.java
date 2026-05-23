package com.hmdp.tools;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.hmdp.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 为 JMeter 多用户秒杀压测生成登录 token。
 *
 * 这个类特意不加 @SpringBootTest：
 * 1. 生成 token 只需要读取 MySQL 用户、写入 Redis 登录态，不需要启动完整 Web 项目。
 * 2. 当前项目的 RedissonConfig 会创建 6379、6380、6381 三个 RedissonClient，
 *    只要其中一个 Redis 实例没启动，SpringBoot 容器就会启动失败。
 * 3. 因此这里直接读取 application.yaml，手动创建 JDBC 连接和 StringRedisTemplate，
 *    避免被 Redisson 多节点配置影响。
 *
 * 执行命令：
 * mvn -Dtest=JMeterTokenGeneratorTest -DgenerateTokens=true test
 */
@EnabledIfSystemProperty(named = "generateTokens", matches = "true")
class JMeterTokenGeneratorTest {

    // JMeter 的 CSV Data Set Config 会读取这个文件，一行对应一个线程使用的 token。
    private static final Path TOKEN_FILE = Paths.get("assets", "File", "tokens.txt");

    @Test
    void generateTokens() throws IOException {
        // 读取 application.yaml 中的 MySQL 和 Redis 配置，保证和项目运行环境一致。
        Properties properties = loadApplicationProperties();

        // 从 tb_user 查询已有用户。每个用户生成一个 token，用于模拟不同用户下单。
        List<UserDTO> users = loadUsers(properties);
        if (users.isEmpty()) {
            throw new IllegalStateException("No users found in tb_user.");
        }

        // 这里只连接 application.yaml 里的 spring.redis，也就是项目登录态使用的 Redis。
        StringRedisTemplate stringRedisTemplate = createStringRedisTemplate(properties);
        Files.createDirectories(TOKEN_FILE.getParent());

        List<String> tokens = new ArrayList<>(users.size());
        for (UserDTO user : users) {
            // 与 UserServiceImpl.login 保持一致：使用 Hutool UUID 生成无横线 token。
            String token = UUID.randomUUID().toString(true);

            // 与 UserServiceImpl.login 保持一致：只把 UserDTO 字段写入 Redis Hash。
            // Hash 字段值必须转成 String，否则 StringRedisTemplate 无法序列化非字符串值。
            Map<String, Object> userMap = BeanUtil.beanToMap(user, new HashMap<>(),
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

            // 与登录逻辑保持一致：login:token:{token} -> {id, nickName, icon}
            String tokenKey = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);

            // token 有效期沿用 RedisConstants.LOGIN_USER_TTL，当前项目是 30 分钟。
            stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
            tokens.add(token);
        }

        // 输出一行一个 token，JMeter 中变量名配置为 token，请求头使用 ${token}。
        Files.write(TOKEN_FILE, tokens, StandardCharsets.UTF_8);
        System.out.println("Generated " + tokens.size() + " tokens to " + TOKEN_FILE.toAbsolutePath());
    }

    private Properties loadApplicationProperties() {
        // 把 YAML 配置展开成 Properties，例如 spring.redis.host、spring.datasource.url。
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yaml.getObject();
        if (properties == null) {
            throw new IllegalStateException("Failed to load application.yaml.");
        }
        return properties;
    }

    private List<UserDTO> loadUsers(Properties properties) {
        // 直接使用项目的数据源配置连接 hmdp 数据库。
        String url = properties.getProperty("spring.datasource.url");
        String username = properties.getProperty("spring.datasource.username");
        String password = properties.getProperty("spring.datasource.password");

        List<UserDTO> users = new ArrayList<>();
        // 只查询登录态需要的字段。RefreshTokenInterceptor 会根据这些字段还原 UserDTO。
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select id, nick_name, icon from tb_user order by id")) {
            while (resultSet.next()) {
                UserDTO user = new UserDTO();
                user.setId(resultSet.getLong("id"));
                user.setNickName(resultSet.getString("nick_name"));
                user.setIcon(resultSet.getString("icon"));
                users.add(user);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load users from tb_user.", e);
        }
        return users;
    }

    private StringRedisTemplate createStringRedisTemplate(Properties properties) {
        // 读取 Spring Boot Redis 配置，并手动创建 StringRedisTemplate。
        String host = properties.getProperty("spring.redis.host");
        int port = Integer.parseInt(properties.getProperty("spring.redis.port"));
        String password = properties.getProperty("spring.redis.password");

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.trim().isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(password));
        }

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        // 手动 new 的连接工厂不归 Spring 容器管理，需要主动初始化。
        connectionFactory.afterPropertiesSet();
        return new StringRedisTemplate(connectionFactory);
    }
}
