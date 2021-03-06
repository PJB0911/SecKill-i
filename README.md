# 基于 SpringBoot 高并发商城秒杀系统（基础项目）

* [项目简介](#项目简介)
  * [什么是秒杀](#什么是秒杀)
  * [秒杀场景特点](#秒杀场景特点)
  * [秒杀架构设计理念](#秒杀架构设计理念)
  * [需要解决的问题](#需要解决的问题)
* [效果展示](#效果展示)
* [项目架构](#项目架构)
  * [数据模型](#数据模型)
  * [DAO/Service/Controller结构](#DAO/Service/Controller结构)
* [要点和细节](#要点和细节)
  * [基本设计](#基本设计)
	* [Data Object/Model/View Object](#data-objectmodelview-object)
	* [通用返回对象](#通用返回对象)
	* [处理错误信息](#处理错误信息)
    * [异常拦截器处理自定义异常](#异常拦截器处理自定义异常)
    * [跨域问题](#跨域问题)
  * [优化校验规则](#优化校验规则)
    * [校验规则](#校验规则)
    * [封装校验结果](#封装校验结果)
    * [创建校验器/使用校验](#创建校验器使用校验)
  * [用户业务](#用户业务)
    * [短信发送业务](#短信发送业务)
    * [注册业务](#注册业务)
    * [登录业务](#登录业务)
  * [商品业务](#商品业务)
    * [商品添加业务](#商品添加业务)
    * [获取商品业务](#获取商品业务)
    * [查询所有商品](#查询所有商品)
  * [交易业务](#交易业务)
    * [下单业务](#下单业务)
    * [订单ID的生成](#订单id的生成)
  * [秒杀业务](#秒杀业务)
    * [秒杀DO/Model和VO](#秒杀domodel和vo)
    * [升级获取商品业务](#升级获取商品业务)
    * [活动商品下单业务](#活动商品下单业务)
* [改进](#改进)

------

## 开发工具 
IntelliJ IDEA 2019.3.3 x64

## 开发环境				
| JDK |Maven | Mysql |SpringBoot |
|--|--|--|--|
|1.8 | 3.6.3 | 5.7 | 2.1.5.RELEASE | 

## 项目简介
本项目是高并发商城秒杀系统的基础项目，主要是模拟应对大并发场景下，如何完成商品的秒杀业务。针对秒杀场景下为应对大并发所做的优化见 [高并发商城秒杀系统SecKill-ii](https://github.com/PJB0911/SecKill-ii)。

### 什么是秒杀

秒杀场景一般会在电商网站举行一些活动或者节假日在12306网站上抢票时遇到。对于电商网站中一些稀缺或者特价商品，电商网站一般会在约定时间点对其进行限量销售，因为这些商品的特殊性，会吸引大量用户前来抢购，并且会在约定的时间点同时在秒杀页面进行抢购。

### 秒杀场景特点

- 秒杀时大量用户会在同一时间同时进行抢购，网站瞬时访问流量激增。
- 秒杀一般是访问请求数量远远大于库存数量，只有少部分用户能够秒杀成功。
- 秒杀业务流程比较简单，一般就是下订单减库存。

### 秒杀架构设计理念

- **限流**： 鉴于只有少部分用户能够秒杀成功，所以要限制大部分流量，只允许少部分流量进入服务后端。
- **削峰**：对于秒杀系统瞬时会有大量用户涌入，所以在抢购一开始会有很高的瞬间峰值。高峰值流量是压垮系统很重要的原因，所以如何把瞬间的高流量变成一段时间平稳的流量也是设计秒杀系统很重要的思路。实现削峰的常用的方法有利用缓存和消息中间件等技术。
- **异步处理**：秒杀系统是一个高并发系统，采用异步处理模式可以极大地提高系统并发量，其实异步处理就是削峰的一种实现方式。
- **内存缓存**：秒杀系统最大的瓶颈一般都是数据库读写，由于数据库读写属于磁盘IO，性能很低，如果能够把部分数据或业务逻辑转移到内存缓存，效率会有极大地提升。
- **水平拓展**：当然如果我们想支持更多用户，更大的并发，最好就将系统设计成弹性可拓展的。拓展机器,像淘宝、京东等双十一活动时会增加大量机器应对交易高峰。

### 需要解决的问题：

- **业务功能：用户模块、商品模块、订单模块、秒杀模块**
- 云端部署，性能压测
- 单机服务器优化：Tomcat并发容量
- 分布式扩展：Nginx反向代理，分布式会话
- 查询性能优化：商品页面缓存、数据热点缓存（多级缓存：redis/guava/nginx lua）
- 查询性能优化：页面静态化(CDN)，前后端分离
- 交易性能优化：库存缓存、异步处理（RocketMQ）、缓存一致性
- 流量削峰：秒杀令牌，秒杀大闸，队列泄洪
- 接口安全：防刷限流

------



## 效果展示

**注册**

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/register.png)

**获取验证码**

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/register_OTP.png)

**商品列表**

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/list.png)

**商品详情**

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/item.png)


------

## 项目架构

#### 数据模型

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/models.png)

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/datamodels.png)

#### DAO/Service/Controller结构

![Image text](https://github.com/PJB0911/SecKill-i/blob/master/images/classmodels.png)

------

## 要点和细节

### 基本设计
#### Data Object/Model/View Object

通常的做法是一张用户信息`user_info`表，包含了用户的**所有信息**。而企业级一般将用户的**敏感信息**从用户表从分离出来，比如密码，单独作为一张表。这样，就需要两个DAO来操作同一个用户，分别是`UserDAO`和`UserPasswordDAO`，这就是Data Object，从数据库直接映射出来的Object。

但是在Service层操作的时候，又需要将两个Data Object对象合在一起操作，所以就把两个Data Object封装成一个`Model`对象，包含了用户的**所有信息**。

但是在Controller层，我们并不希望将`UserModel`的“密码”、“注册信息”等无关信息暴露给前端，这就需要`View Object`，将需要暴露的字段从`Model`中剔除掉。

```java
public class UserDO {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;
    private String registerMode;
    private String thirdPartyId;
}
```

```java
public class UserPasswordDO {
    private Integer id;
    private String encrptPassword;
    private Integer userId;
}
```

```java
public class UserModel {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;
    private String registerMode;
    private String thirdPartyId;
    //从UserPasswordDO得到的属性
    private String encrptPassword;
}
```

```java
public class UserVO {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;
}
```

同样，对于商品，**库存**是频繁操作的字段，也应该分离出来，成为两张表。一张`item`表，一张`item_stock`表。

#### 通用返回对象

一般要使用一个统一的类，来返回后端处理的对象。不然默认给前端是对象的`toString()`方法，不易阅读，而且，不能包含是处理成功还是失败的信息。这个类就是`CommonReturnType`。

```java
public class CommonReturnType {
    //有success和fail
    private String status;
    //若success，data返回前端需要的JSON数据；
    //若fail，则data使用通用的错误码格式
    private Object data;
    public static CommonReturnType create(Object result){
        return CommonReturnType.create(result,"success");
    }
    public static CommonReturnType create(Object result,String status){
        CommonReturnType type=new CommonReturnType();
        type.setStatus(status);
        type.setData(result);
        return type;
    }
}
```

#### 处理错误信息

当程序内部出错后，Spring Boot会显示默认的出错页面。这些页面对于用户来说不够友好。需要将错误封装起来，通过`CommonReturnType`返回给用户，告诉用户哪里出错了，比如“密码输入错误”、“服务器内部错误”等等。

这些内容，封装到了`error`包下面的三个类里面。一个是`CommonError`接口，一个是枚举异常类`EmBizError`，一个是异常处理类`BizException`。

`CommonError`接口提供三个方法，一个获得错误码的方法`getErrCode()`，一个获得错误信息的方法`getErrMsg()`，一个设置错误信息的方法`setErrMsg(String errMsg)`。

```java
public interface CommonError {
    int getErrCode();
    String getErrMsg();
    CommonError setErrMsg(String errMsg);
}
```

错误类型枚举类`EmBizError`含有两个属性，一个是错误码`errCode`一个是错误信息`errMsg`。通过`CommonError`接口的方法，获得相应错误码和错误信息。

```java
public enum EmBizError implements CommonError {
    //10000通用错误类型
    PARAMETER_VALIDATION_ERROR(100001,"参数不合法"),
    UNKNOWN_ERROR(100002,"未知错误"),
    //2000用户信息相关错误
    USER_NOT_EXIST(20001,"用户不存在"),
    USER_LOGIN_FAIL(20002,"用户手机或密码不正确"),
    USER_NOT_LOGIN(20003,"用户还未登录"),
    //3000交易信息错误
    STOCK_NOT_ENOUGH(30001,"库存不足"),
    MQ_SEND_FAIL(30002,"库存异步消息失败"),
    RATELIMIT(30003,"活动太火爆，请稍后再试");

    private int errCode;
    private String errMsg;

    EmBizError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }
    @Override
    public String getErrMsg() {
        return this.errMsg;
    }
    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg=errMsg;
        return this;
    }
}
```

`BizException`继承`Exception`类实现`CommonError`接口，用于在程序出错时，抛出异常。

```java
public class BizException extends Exception implements CommonError{
    private CommonError commonError;
    //直接接受EmBizError的传参，用于构造业务异常，多态
    public BizException(CommonError commonError){
        super();
        this.commonError=commonError;
    }
    //接受自定义errMsg构造义务异常
    public BizException(CommonError commonError,String errMsg){
        super();
        this.commonError=commonError;
        this.commonError.setErrMsg(errMsg);
    }
    //省略Override Methods
}
```

这样，在程序中可以抛出自定义的异常了。

```java
throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR);
```

#### 异常拦截器处理自定义异常

虽然上面抛出了自定义的`BizException`异常，但是SpringBoot还是和之前一样，返回500页面。这是由于，`BizException`被抛给了Tomcat，而Tomcat不知道如何处理`BizException`。所以，需要一个**拦截器**，拦截抛出的`BizException`。

在`controller.BaseController`中新建一个`handlerException()`方法， 添加`@ExceptionHandler`和`@ResponseStatus`注解。这样，抛出的异常，就会先进入这个方法进行处理，如果是`BizException`，那么创建一个`Map`来封装错误码和错误信息，返回给前端。

```java
@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.OK)
@ResponseBody
public Object handlerException(HttpServletRequest request, Exception ex){
    Map<String,Object> responseData=new HashMap<>();
    if(ex instanceof BizException){
        BizException bizException=(BizException)ex;
        responseData.put("errCode",bizException.getErrCode());
        responseData.put("errMsg",bizException.getErrMsg());
        //打印堆栈信息，开发过程需要。发布后不需要
        ex.printStackTrace();
     }else{
        responseData.put("errCode", EmBizError.UNKNOWN_ERROR.getErrCode());
        responseData.put("errMsg",EmBizError.UNKNOWN_ERROR.getErrMsg());
        ex.printStackTrace();
    }
    return CommonReturnType.create(responseData,"fail");
}
```

#### 跨域问题

由于浏览器的安全机制，JS只能访问与所在页面同一个域（相同协议、域名、端口）的内容，
但是我们这里，需要通过Ajax请求，去请求后端接口并**返回数据**，这时候就会受到浏览器的安全限制，产生跨域问题（如果只是通过Ajax向后端服务器发送请求而不要求返回数据，是不受跨域限制的）。

所以，前端的HTML页面，在Ajax请求体里面，需要设置`contentType:"application/x-www-form-urlencoded"`，并添加一个额外的字段`xhrFields:{withCredentials: true}`。

后端的Controller类需要添加`@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")`注解。Controller的每一个API的`@RequestMapping`注解，也要添上`consumes = {"application/x-www-form-urlencoded"}`字段。

这样就解决了Ajax跨域问题。

----------


### 优化校验规则

#### 校验规则

之前的入参，都是通过类似`if(StringUtils.isNotBlank(attr1)||StringUtils.isNotBlank(attr12))`的方式来校验的，很繁琐，对于像年龄这样的字段，不仅不能为空，其值还应该在一个范围内，那就更麻烦了。

所以应该封装一个类，专门来校验。这里使用了`org.hibernate.validator`包来进行校验。

比如像`UserModel`类，直接可以对字段添加注解，实现校验规则。

```java
@NotBlank(message = "用户名不能为空")
private String name;

@NotNull(message = "年龄必须填写")
@Min(value = 0,message = "年龄必须大于0")
@Max(value = 120,message = "年龄必须小于120")
private Integer age;
```

#### 封装校验结果

当然，上面只是定义了**校验规则**，我们还需要**校验结果**，所以创建一个`validator.ValidationResult`类，来封装校验结果。

```java
public class ValidationResult {
    //校验结果是否有错
    private boolean hasErrors=false;
    //用Map来封装校验结果和校验出错信息
    private Map<String,String> errorMsgMap=new HashMap<>();
    //实现通过格式化字符串信息获取错误结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errorMsgMap.values().toArray(),",");
    }
    public boolean isHasErrors() {
        return hasErrors;
    }
    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }
    public Map<String, String> getErrorMsgMap() {
        return errorMsgMap;
    }
    public void setErrorMsgMap(Map<String, String> errorMsgMap) {
        this.errorMsgMap = errorMsgMap;
    }
}
```

#### 创建校验器/使用校验

定义了校验规则和校验结果，那如何使用校验呢？新建一个`validator.ValidatorImpl`类，实现`InitializingBean`接口，为了能在这个Bean初始化的时候，初始化其中的`javax.validation.Validator`对象`validator`。

自定义一个校验方法`validate`，这个方法实际上就是调用`validator`对象的`validate(Object obj)`方法。该方法会根据校验规则，返回一个`Set`，如果传入的`Object`出现校验错误，就会把错误加入到`Set`中。

最后，我们遍历这个`Set`，将错误封装到`ValidationResult`即可。

```java
@Component
public class ValidatorImpl implements InitializingBean {
    //javax.validation.Validator校验器
    private Validator validator;
	
    //在Bean初始化时，初始化validator对象。
    @Override
    public void afterPropertiesSet() throws Exception {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    //校验的方法
    public ValidationResult validate(Object bean) {
        //校验的结果
        final ValidationResult result = new ValidationResult();
        //javax.validation.Validator对象的validate(Object obj)方法
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);

        if (constraintViolationSet.size() > 0) {
            result.setHasErrors(true);
            constraintViolationSet.forEach(constraintViolation -> {
                String errMsg = constraintViolation.getMessage();
                String propertyName = constraintViolation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName, errMsg);
            });
        }
        return result;
    }
}
```

这样，当我们需要进行参数校验时，就不用大张旗鼓地手动校验了。直接调用`validate`方法，根据`ValidationResult`对象的`isHasErrors()`方法，就能完成入参校验了。

```java
public void register(UserModel userModel) throws BizException {
    if(userModel==null){
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR);
    }
    //校验器校验
    ValidationResult result=validator.validate(userModel);
    //根据ValidationResult的isHasErrors完成校验。
    if(result.isHasErrors()){
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
}
```

----------


### 用户业务

#### 短信发送业务

注册之前，输入手机号，请求后端`getOtp`接口。接口生成验证码后，发送到用户手机，并且用Map将验证码和手机绑定起来。企业级开发将Map放到分布式Redis里面，这里直接放到Session里面，后续优化用redis。

```java
@RequestMapping(value = "/getOtp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
@ResponseBody
public CommonReturnType getOtp(@RequestParam(name="telphone")String telphone){
    Random random=new Random();
    int randomInt=random.nextInt(99999);
    randomInt+=10000;
    String optCode=String.valueOf(randomInt);
    //将验证码与用户手机号进行关联，这里使用HttpSession
    httpServletRequest.getSession().setAttribute(telphone,optCode);
    //将OPT验证码通过短信通道发送给用户，省略
    System.out.println("telphone="+telphone+"& otpCode="+optCode);
    return CommonReturnType.create(null);
}
```

#### 注册业务

注册请求后端`UserController.register`接口，先进行短信验证，然后将注册信息封装到`UserModel`，调用`UserServiceImpl.register()`，先对注册信息进行入参校验，再将`UserModel`转成`UserDO`、`UserPasswordDO`存入到数据库。

同时需要**注意**的是，`UserServiceImpl.register()`方法，涉及到了数据库多表的写操作，需要加上`@Transactional`注解，以事务的方式进行处理。

详见：`controller.UserController.register()`和`service.impl.UserServiceImpl.register()`。

#### 登录业务

登录请求后端`UserController.login`接口，前端传过来`手机号`和`密码`。判空之后，调用`UserServiceImpl.validateLogin`方法，这个方法先通过`手机号`查询`user_info`表，看是否存在该用户，返回`UserDO`对象，再根据`UserDO.id`去`user_password`表中查询密码。如果密码匹配，则返回`UserModel`对象给`login`方法，最后`login`方法将`UserModel`对象存放到`Session`里面，即完成了登录。

```java
@RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
@ResponseBody
public CommonReturnType login(@RequestParam(name = "telphone")String telphone,
                              @RequestParam(name = "password")String password) throws BizException, UnsupportedEncodingException, NoSuchAlgorithmException {
    //入参校验
    if(org.apache.commons.lang3.StringUtils.isEmpty(telphone)||
       org.apache.commons.lang3.StringUtils.isEmpty(password))
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR);
    //调用Service的方法，验证手机号和密码
    UserModel userModel=userService.validateLogin(telphone,this.EncodeByMD5(password));
    //没有任何异常，则加入到用户登录成功的session内。这里先不用分布式的处理方式。
    this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
    this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
    return CommonReturnType.create(null);
}
```


####  改进：明文密码两次MD5处理

- 客户端：C_PASS=MD5(明文+固定salt)
- 服务端：S_PASS=MD5(C_PASS+随机salt)

加密：出于安全考虑

第一次 （在前端加密，客户端）：密码加密是（明文密码+固定盐值）生成md5用于传输，目的，由于http是明文传输，当输入密码若直接发送服务端验证，此时被截取将直接获取到明文密码，获取用户信息。

加盐值是为了混淆密码，原则就是明文密码不能在网络上传输。

第二次：在服务端再次加密，当获取到前端发送来的密码后。通过MD5（密码+随机盐值）再次生成密码后存入数据库。

防止数据库被盗的情况下，通过md5反查，查获用户密码。方法是随机盐值会在用户注册的时候随机生成，并存在数据库中，用户登陆时就会从数据库获取到随机盐值。

第二次的目的：
黑客若是同时黑掉数据库，通过解析前端js文件，如果知道md5加密的过程，就知道用户的密码。

但是在后端加入随机盐值和传输密码的md5组合，黑客是无法知道通过后端密码加密过程的，从而无法知道密码。

```javascript
    // 获取用户输入密码       
    var inputPass = $("#password").val();
    // 获取salt    
    var salt = "1a2b3c4d";
    // md5+salt，与服务器端的第一次MD5规则一致
    var str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
    var password = md5(str);
```

```java
    /**
     * @param encrptPassword 客户端传递过来的加密密码
     */
    public String login(String telphone, String encrptPassword) {
        // 判断手机号是否存在
        SeckillUser user = this.getMiaoshaUserById(Long.parseLong(mobile));
        if (user == null)
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        // 判断手机号对应的密码是否一致
        String dbPassword = user.getPassword();
        String dbSalt = user.getSalt();
        String calcPass = MD5Util.formPassToDbPass(password, dbSalt);
        if (!calcPass.equals(dbPassword))
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
    
    }

    /**
     * 对客户端传来的密码md5加盐，加盐后的md5为存储在数据库中的密码md5
     * @param formPassword 客户端传来的密码
     * @param saltDb       数据库查出来的salt
     * @return
     */
    public static String formPassToDbPass(String formPassword, String saltDb) {
        String str = "" + saltDb.charAt(0) + saltDb.charAt(2) + formPassword + saltDb.charAt(5) + saltDb.charAt(4);
        return md5(str);
    }
```


----------


### 商品业务

#### 商品添加业务

请求后端`ItemController.create`接口，传入商品创建的各种信息，封装到`ItemModel`对象，调用，`ItemServiceImpl.createItem`方法，进行入参校验，然后将`ItemModel`转换成`ItemDO`和`ItemStockDO`对象，分别写入数据库。

#### 获取商品业务

请求后端`ItemController.get`接口，传入一个`Id`，通过`ItemServiceImpl.getItemById`先查询出`ItemDO`对象，再根据这个对象查出`ItemStockDO`对象，最后两个对象封装成一个`ItemModel`对象返回。

#### 查询所有商品

请求后端`ItemController.list`接口，跟上面类似，查询所有商品。


----------


### 交易业务

#### 下单业务

请求后端`OrderController.createOrder`接口，传入商品Id`ItemId`和下单数量`amount`。接着在`Session`中获取用户登录信息，如果用户没有登录，直接抛异常。前端接收到错误代码，将页面转移到`login.html`

```java
@RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
@ResponseBody
public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,@RequestParam(name = "promoId",required = false)Integer promoId,@RequestParam(name = "amount")Integer amount) throws BizException {
    Boolean isLogin = (Boolean)httpServletRequest.getSession().getAttribute("IS_LOGIN");
    if(isLogin==null||!isLogin.booleanValue())
        throw new BizException(EmBizError.USER_NOT_LOGIN,"用户还未登录，不能下单");
    //获取用户的登录信息
    UserModel userModel = (UserModel)httpServletRequest.getSession().getAttribute("LOGIN_USER");
    orderService.createOrder(userModel.getId(),itemId,promoId,amount);
    return CommonReturnType.create(null);
}
```

在将订单存入库之前，先要调用`OrderServiceImpl.createOrder`方法，对商品信息、用户信息、下单数量进行校验。

```java
@Override
@Transactional
public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount) throws BizException {
    //1. 校验下单状态。下单商品是否存在，用户是否合法，购买数量是否正确
    ItemModel itemModel=itemService.getItemById(itemId);
    if(itemModel==null)
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
    UserModel userModel=userService.getUserById(userId);
    if(userModel==null)
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
    if(amount<=0||amount>99)
        throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR,"数量信息不存在");
```

此外，还需要**校验库存是否足够**。

```java
boolean result=itemService.decreaseStock(itemId,amount);
if(!result)
    throw new BizException(EmBizError.STOCK_NOT_ENOUGH);
```

最后将订单入库，再让销量增加。


#### 订单ID的生成

订单ID**不能是简单的自增长**，而是**要符合一定的规则**，比如前8位，是年月日；中间6位为自增序列；最后2位为分库分表信息。

有以下几个细节需要注意，在`OrderServiceImpl.generatorOrderNo`方法中可以查看实现细节。

1. 前8位比较好实现，使用`LocalDateTime`，处理一下格式即可。
2. 中间6位自增序列，需要新建一个`sequence_info`表，里面包含`name`、`current_value`、`step`三个字段。这个表及其对应的DO专门用来产生**自增序列**。
3. `generatorOrderNo`方法需要将序列的更新信息写入到`sequence_info`表，而且该方法封装在`OrderServiceImpl.createOrder`方法中。如果`createOrder`执行失败，会进行回滚，默认情况下，`generatorOrderNo`也会回滚。而我们希望**生成ID的事务不受影响**，就算订单创建失败，ID还是继续生成，所以`generatorOrderNo`方法使用了`REQUIRES_NEW`事务传播方式。

----------


### 秒杀业务

#### 秒杀DO/Model和VO

`PromoDO`包含活动名称、起始、结束时间、参与活动的商品id、参与活动的价格。而我们希望在前端显示**活动的状态**，是开始？还是结束？还是正在进行中？所以`PromoModel`对象新加一个`status`字段，通过从数据库的`start_time`和`end_time`字段，与当前系统时间做比较，设置状态。

```java
 //1是还未开始，2是进行中，3是已结束
if(promoModel.getStartDate().isAfterNow()) {
    promoModel.setStatus(1);
}else if(promoModel.getEndDate().isBeforeNow()){
    promoModel.setStatus(3);
}else{
    promoModel.setStatus(2);
}
```

对于`ItemModel`，需要将`PromoModel`属性**添加进去**，这样就完成了商品和活动信息的关联。

在`ItemServiceImpl.getItemById`中，除了要查询商品信息`ItemDO`、库存信息`ItemStockDO`外，还需要查询出`PromoModel`。

```java
public ItemModel getItemById(Integer id) {
    ItemDO itemDO=itemDOMapper.selectByPrimaryKey(id);
    if(itemDO==null) return null;
    //操作获得库存数量
    ItemStockDO itemStockDO=itemStockDOMapper.selectByItemId(itemDO.getId());
    //将dataObj转换成Model
    ItemModel itemModel=convertModelFromDataObject(itemDO,itemStockDO);
    //获取商品的活动信息
    PromoModel promoModel= promoService.getPromoByItemId(itemModel.getId());
    if(promoModel!=null&&promoModel.getStatus()!=3){
        itemModel.setPromoModel(promoModel);
    }
    return itemModel;
}
```

对于`ItemVO`，也是一样的，我们需要把活动的信息（活动进行信息、活动价格等）显示给前端，所以需要在`ItemVO`里面添加`promoStatus`、`promoPrice`等属性。

```java
private String imgUrl;
//商品是否在秒杀活动中，以及其状态
private Integer promoStatus;
private BigDecimal promoPrice;
private Integer promoId;
//开始时间，用来做倒计时展示
private String startDate;
```

```java
//ItemController
private ItemVO convertVOFromModel(ItemModel itemModel){
    if(itemModel==null) return null;
    ItemVO itemVO=new ItemVO();
    BeanUtils.copyProperties(itemModel,itemVO);
    //有秒杀活动，就在ItemVO设置相应信息。
    if(itemModel.getPromoModel()!=null){
        itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
        itemVO.setPromoId(itemModel.getPromoModel().getId());
        itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
               itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
    }else{
        itemVO.setPromoStatus(0);
    }
}
```

后续总结，获取商品信息的完整流程。

#### 升级获取商品业务

之前获取的商品不包含秒杀活动信息，现在需要把活动信息添加进去。

还是先请求`ItemController.list`接口，获取所有商品信息。然后通过点击的商品`Id`，请求`ItemController.get`接口，查询商品详细信息。

首先根据`Id`，调用`ItemServiceImpl.getItemById`查询出**商品信息**、**库存信息**、**秒杀活动信息**，一并封装到`ItemModel`中。然后再调用上面的`convertVOFromModel`，将这个`ItemModel`对象转换成`ItemVO`对象，包含了秒杀活动的信息，最后返回给前端以供显示。

#### 活动商品下单业务

秒杀活动商品的下单，需要单独处理，以“秒杀价格”入下单库。所以`OrderDO`也需要添加`promoId`属性。

```java
public class OrderDO {
    private String id;
    private Integer userId;
    private Integer itemId;
    //若promoId非空，则是秒杀方式下单
    private Integer promoId;
    //若promoId非空，则是秒杀价格
    private Double itemPrice;
    private Integer amount;
    //若promoId非空，则是秒杀总价
    private Double orderPrice;
```

之前活动商品的下单，附带`itemId`、`amount`请求`OrderController.createOrder`接口，现在，会附带一个`promoId`请求接口，这个参数会作为`OrderServiceImpl.createOrder`的参数，进行参数校验。

```java
//校验活动信息
if(promoId!=null){
     //1.校验对应活动是否适用于该商品
     if(promoId.intValue()!=itemModel.getPromoModel().getId()){
          throw new  BizException(EmBizError.PARAMETER_VALIDATION_ERROR,"活动信息不存在");
     //2.校验活动是否在进行中
     }else if (itemModel.getPromoModel().getStatus()!=2){
          throw new  BizException(EmBizError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
     }
}
```

最后，如果`promoId`不为空，那么订单的价格就以活动价格为准。

```java
if(promoId!=null){
    //以活动价格入库
    orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
}else{
    //以非活动价格入库
    orderModel.setItemPrice(itemModel.getPrice());
}
orderModel.setPromoId(promoId);
```

## 改进

- 如何发现容量问题
- 如何使得系统水平扩展
- 查询效率低下
- 活动开始前页面被疯狂刷新
- 库存行锁问题
- 下单操作多、缓慢
- 浪涌流量如何解决

## 优化版本
[性能优化SecKill-ii](https://github.com/PJB0911/SecKill-ii)

