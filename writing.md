# T6

由于这题的实践性不强，业务逻辑不甚重要，因此跳过讲解整体思路，直接开始解释代码。

## 代码解释

我们先讲一个未加优化的“毛坯房”，这个项目仅能做到基础的读写、保存数据。

### 初始化数据库

```sql
create database backend_t6;
create table user_info(
    id int primary key auto_increment,
    username varchar(255),
    password varchar(511));
create index name_index on user_info(username);
```

作用分别是:

1. 创建一个新的数据库;
2. 创建一张新表,包含主键整数值`id`,字符串`username`,`password`;(仅泛指数据,没有实际意义,不当做真的用户名/密码看待)`auto_increment`表示这个值将自动从小到大生成。当然，对于这个题来说这个id也可以不加。
3. 在用户名一列上创建索引.
索引的作用将在下部分阐述.

### `User`类

```java
public class User {
    private int id;
    private String username;
    private String password;

    public User() {
    }
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}

```

这个类很好理解，指代的就是映射到数据库中每条数据的类，其属性`id`、`username`、`password`都是数据库表的列名。

### `DBMapper`接口

```java

@Mapper

public interface DBMapper {
    @Insert("INSERT INTO user_info (username, password) VALUES ()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void writeDB(User user);

    @Select("SELECT username, password FROM user_info ORDER BY id")
    List<User> readDB();
}

```

为什么这里要用接口？在下一部分中将解释。
这里使用`@Mapper`注解，让`Mybatis`知道这是一个mapper接口，赋予它操作数据库的能力。
这里这个接口有两个简单的方法。
`writeDB`方法的作用是向数据库中写入一条新的`User`信息，`#{username}, #{password}`指的是这个方法的输入值；`@Option`这个注解在这里是标明这个`id`是自动生成的，让框架把自动生成的值赋给对象的`id`属性。
`readDB()`方法是用来读取数据库的，返回的是数据库中的所有信息，以一张表的形式显示。

### `DBService`类

```java

@Service
public class DBService {

    @Autowired
    private DBMapper dbMapper;
    public void setDbMapper(DBMapper dbMapper) {
        this.dbMapper = dbMapper;
    }
    public void writeDB(User user) {
        dbMapper.writeDB(user);
    }
    public List<User> readDB() {
        return dbMapper.readDB();
    }
}
```

这个项目中`Service`层其实不是很重要，因为业务逻辑很简单。这个类中基本就是简单的调用mapper层的方法。
值得注意的是，我们写了`private DBMapper dbMapper;`这样的语句，定义了一个mapper类型的变量，而这个对象的创建便是由Spring管理，即使mapper接口没有实现类，有控制权的Spring也可以自己创建一个实现类，并把这个对象归为这个类。至于为什么要这样，在下一部分中将解释。

### `DBController`类

```java

@RestController
public class DBController {
    @Autowired
    private DBService dbService;
    public void setDbService(DBService dbService) {
        this.dbService = dbService;
    }

    @PostMapping("/db")
    public void writeDB(@RequestBody User user) {
        dbService.writeDB(user);
    }
    @GetMapping("/db")
    public List<User> readDB() {
        return dbService.readDB();
    }
}

```

调用`DBService`对象的两个方法，分别对应写入/查询。

### 外部yml文件

`application.yml`:

```yml
spring:
  config:
    import:
      - "C:/Users/qyr/DBDemo-config.yaml"
  application:
    name: DBDemo
```

`DBDemo-config.yaml`:

```yml
spring: 
  datasource:
    url: jdbc:mysql://localhost:3306/backend_t6
    username: 不给你看
    password: 不给你看
    driver-class-name: com.mysql.cj.jdbc.Driver
```

上面的文件是在项目中`resource`目录中的，它采用引用下面这个文件的方式，目的就是不把用户名和密码直接写在项目源码中。这样一来，只有我在我电脑上才能用这个项目运行MySQL，操作数据库了。
当然，实际项目中我的用户名和密码不是“不给你看”~~（废话）~~

### 怎么写加分项？

如果我们要实现分页查询数据，我们就需要在`DBMapper`中写入相应方法：

```java

    @Select("SELECT username, password FROM user_info ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<User> readDBWithPagination(@Param("limit") int limit, @Param("offset") int offset);
```

这里用到了SQL语句中的LIMIT和OFFSET传入参数，分别表示最大输出多少个、从哪行开始。

在`DBController`中，我们要加上这个：

```java

    @GetMapping("/db/page")
    public List<User> readByPage(@RequestParam int size, @RequestParam int page) {
        return dbService.readByPage(size, page);
    }
```

这样就开了一个接口，让前端传入每页的大小（size其实就是limit）、页的序号，从而明确要输出的是什么了。

由于page和offset的实际意义不一样，我们需要service层进行一定的处理：

```java

    public List<User> readByPage(int limit, int page) {
        int offset = (page - 1) * limit;
        return dbMapper.readByPage(limit, offset);
    }
```

我们需要在这层中算出来offset，再传给mapper。

## 思考

先来解答上文中的疑问：

1. 索引有什么作用？
   索引可以理解为一种为查找而生的结构，它通过某些算法提升查找效率，当我们为某个列加上索引，这个索引就会对这个列上的数据进行一定的处理和记录。
   例如，如果我们用以下语句查询数据：

   ```sql
   SELECT * FROM user_info WHERE username="Alice"
   ```

   如果我们不建立索引，数据库就只能一条一条找，而当我们建立了索引，数据库就能更快通过索引找到"Alice"所在的行。
2. 为什么Mapper是一个接口？为什么不手动创建其实现类？
   下文即将提及，Mapper层在项目中的作用其实就是与数据库交互，对它进行增删改查。因此，一般Mapper是用来执行SQL语句的。由此可得，Mapper中的方法是最重要的，而几乎不管“属性”。
   而由于我们使用了MyBatis框架，它能大大简化我们用Java代码执行SQL语句、操作数据库，因此Mapper的方法我们只需要交给MyBatis就好。怎么交付呢？就是为方法加上注解，在注解中告诉框架我要干什么。一旦我把SQL语句写进注解中，我就发现好像方法里没什么可写的了。此时不难发现，用一个不需要在方法里写东西的接口代表mapper再合适不过了。
   而不手动写实现类也是由此而来：如果要写实现类，还要手动复写方法。MyBatis和Spring比我们更懂方法应该怎么写、对象怎么去创建，交给它们就好。

接下来是题目中的思考题：

1. 各个层次都是用来干嘛的？

   我的项目层次长这样：
   ![pasted-image.png · 32](https://img.cdn1.vip/i/6ae0942a598eb_1793102890.webp)
   在这道题中：

   - controller层负责与前端的数据交互，也就是web相关的实现；
   - service层负责处理业务逻辑；
   - mapper层负责进行数据库的增删改查。
   - entity包就是普通的承载数据的类。

   而题中所说的DAO，实际上也是一种层次：Data Access Object，它旨在隔离开业务逻辑与数据库，负责数据库的交互。乍一听好像与我上文中说的mapper的职责差不多，实际上mapper是DAO的一种实现方式，它的实现方式就是`map`（映射），将数据库的字段与类的属性形成一一映射。
2. 如果只有Controller，改个查询逻辑有多麻烦？
   首先我们要改动与前端的交互方式，改变实现接口的方法；哪怕这一步省了，我们也要更改接收数据后处理它的方法，改完后可能还要修改操作数据库的SQL语句或方式。如果有多个接口用到同一个逻辑，还要改好几次。分层之后，我们只需要在service层做出主要的逻辑上的改动，再在另外两个层次做少量改动即可。三个层次分工明确，需求出现时它们分到的部分也总会是明确的。
3. 什么是 ORM？它替你做了哪些事？如果不用 ORM，用原生 JDBC 写一次查询要经历哪几步？
   对象关系映射（Object-Relational Mapping），指的就是将数据库中的数据映射到Java中的对象，从而更好地用Java语言操作数据库。mapper就是一种ORM的实现。假如纯用原生JDBC，我们要经历的步骤大概有：

   - 与数据库建立联系；
   - 编辑好要执行的SQL语句；
   - 调用JDBC APi，执行语句；
   - 将读取得到的结果转换为Java中的数据结构；
   - 关闭联系。
   其复杂性可见一斑，而很多工作其实都不是能体现业务逻辑的。如果我们让MyBatis帮我们，它就能帮我们省下这些我们重复写会写烦的语句，同时它建立的映射简化了我们对数据库的操作，让我们关注于业务逻辑本身。
4. 为什么不能直接拼接 SQL 字符串？拼接会带来什么风险？
   这里的拼接字符串，指的应该就是类似于以下的操作：

   ```java
    String username = getUsernameFromRequest();

    String sql =
    "SELECT * FROM user_info WHERE username = '"
    + username + "'";
   ```

   这样写固然能让执行的SQL语句一目了然，然而却藏着很大的风险：
   假如前端传来的一个请求中，username或其他的字符串是在SQL中有意义的语句片段，当字符串被拼接后，这个SQL语句就变了，它将不再执行原本的逻辑，而是执行恶意者想要执行的语句，这就相当于让恶意者钻进了代码层。
   例如，假如select语句中where后面跟的东西被篡改为能使其输出整张表的语句，则整张表被输出出来，原本隐私的信息直接暴露出来。

最后，这个项目还有什么可以优化的？
显然能优化的点很多，因为我们写的是一个“毛坯房”，只能提供基本的功能。例如，我们没有对错误信息的优化；我们不能根据不同情况返回不同的状态码；按页查询时，我们没有考虑非法输入值、没有考虑当页数过大输出空页面的情况。
所以，之后我们仍要在实践中学习更多优化的方法。

## 测试

![pasted-image.png · 174](https://img.cdn1.vip/i/6ae09e4f3185c_1793105487.webp)

初始有三条数据

![pasted-image.png · 205](https://img.cdn1.vip/i/6ae09ea334da0_1793105571.webp)

通过接口成功查询到。id=0是因为用于查询的SQL语句中不包含
Id，而int变量默认值是0。

![pasted-image.png · 183](https://img.cdn1.vip/i/6ae09f9b2abdc_1793105819.webp)
![pasted-image.png · 217](https://img.cdn1.vip/i/6ae09fb640f90_1793105846.webp)

通过get方法成功插入了一条新的数据。

![pasted-image.png · 207](https://img.cdn1.vip/i/6ae0a09918209_1793106073.webp)
![pasted-image.png · 187](https://img.cdn1.vip/i/6ae0a0a8d9876_1793106088.webp)

用分页查找功能，按每页最多查3条数据，成功查询到了这两页数据。