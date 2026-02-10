# BML-Backend 授权与安全设计 V2.0

> **版本**: v2.0 | **项目**: bml-backend | **日期**: 2026-02-09

---

## 1. License授权机制

### 1.1 授权架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           License授权流程                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─ 授权生成 (离线工具) ──────────────────────────────────────────────┐ │
│  │                                                                    │ │
│  │  1. 收集客户信息 (名称、有效期、用户数、模块)                        │ │
│  │  2. 收集机器码 (客户提供)                                           │ │
│  │  3. 生成授权内容JSON                                                │ │
│  │  4. RSA-2048私钥签名                                                │ │
│  │  5. Base64编码输出License Key                                       │ │
│  │                                                                    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                     │                                   │
│                                     ▼                                   │
│  ┌─ 授权验证 (系统启动时) ────────────────────────────────────────────┐ │
│  │                                                                    │ │
│  │  1. 读取License Key                                                │ │
│  │  2. Base64解码                                                     │ │
│  │  3. RSA公钥验签                             ──── 失败 ───▶ 拒绝启动 │ │
│  │  4. 解析授权内容JSON                                                │ │
│  │  5. 验证机器码              ──────────────── 不匹配 ───▶ 拒绝启动  │ │
│  │  6. 验证有效期              ──────────────── 过期 ─────▶ 拒绝启动  │ │
│  │  7. 加载模块权限                                                    │ │
│  │  8. 验证通过,启动系统                                               │ │
│  │                                                                    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 License结构

```json
{
  "licenseId": "550e8400-e29b-41d4-a716-446655440000",
  "customer": {
    "name": "XX科技有限公司",
    "contact": "张三"
  },
  "product": {
    "name": "BML-Backend",
    "version": "1.0.0"
  },
  "license": {
    "type": "ENTERPRISE",
    "issueDate": "2026-01-01",
    "expireDate": "2027-01-01"
  },
  "limits": {
    "maxUsers": 500,
    "enabledModules": ["system", "hr", "oa", "crm"]
  },
  "machine": {
    "code": "8A4F-3B2E-9C1D-5F78"
  }
}
```

### 1.3 机器码生成

```java
public class MachineCodeGenerator {

    public static String generate() {
        StringBuilder sb = new StringBuilder();
        
        // MAC地址
        sb.append(getMacAddress());
        sb.append("|");
        
        // CPU序列号
        sb.append(getCpuSerial());
        sb.append("|");
        
        // 硬盘序列号
        sb.append(getDiskSerial());
        
        // MD5后取16位
        return DigestUtils.md5Hex(sb.toString())
            .substring(0, 16)
            .toUpperCase();
    }
}
```

### 1.4 核心验证代码

```java
@Component
public class LicenseValidator {

    @Value("${bml.license.public-key}")
    private String publicKey;

    public LicenseInfo validate(String licenseKey) throws LicenseException {
        // 1. Base64解码
        byte[] decoded = Base64.decode(licenseKey);
        
        // 2. 分离签名和内容
        byte[] signature = Arrays.copyOf(decoded, 256);
        byte[] content = Arrays.copyOfRange(decoded, 256, decoded.length);
        
        // 3. RSA验签
        if (!RSA.verify(content, signature, publicKey)) {
            throw new LicenseException(ErrorCode.LICENSE_INVALID);
        }
        
        // 4. 解析内容
        LicenseInfo license = JSON.parseObject(content, LicenseInfo.class);
        
        // 5. 验证机器码
        String localMachineCode = MachineCodeGenerator.generate();
        if (!license.getMachineCode().equals(localMachineCode)) {
            throw new LicenseException(ErrorCode.LICENSE_MACHINE_ERROR);
        }
        
        // 6. 验证有效期
        if (LocalDate.now().isAfter(license.getExpireDate())) {
            throw new LicenseException(ErrorCode.LICENSE_EXPIRED);
        }
        
        return license;
    }
}
```

---

## 2. 密码安全

### 2.1 密码存储

| 项目 | 规范 |
|------|------|
| 算法 | BCrypt |
| 强度 | 12 (2^12次迭代) |
| 盐值 | BCrypt自动生成 |

```java
// 密码加密
String encoded = passwordEncoder.encode(rawPassword);

// 密码验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### 2.2 密码策略

| 策略项 | 默认值 | 配置键 |
|--------|--------|--------|
| 最小长度 | 6 | sys.password.minLength |
| 最大长度 | 20 | sys.password.maxLength |
| 必须包含数字 | 是 | sys.password.requireDigit |
| 必须包含字母 | 是 | sys.password.requireLetter |
| 必须包含特殊字符 | 否 | sys.password.requireSpecial |
| 有效期(天) | 90 | sys.password.expireDays |
| 历史密码不重复 | 3次 | sys.password.historyCount |

```java
@Component
public class PasswordValidator {

    public void validate(String password) {
        if (password.length() < minLength) {
            throw new BusinessException("密码长度不能少于" + minLength + "位");
        }
        if (requireDigit && !password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }
        if (requireLetter && !password.matches(".*[a-zA-Z].*")) {
            throw new BusinessException("密码必须包含字母");
        }
    }
}
```

---

## 3. JWT认证

### 3.1 Token结构

```
AccessToken:
┌──────────────────────────────────────────────────────────────┐
│ Header                                                       │
│ { "alg": "HS256", "typ": "JWT" }                            │
├──────────────────────────────────────────────────────────────┤
│ Payload                                                      │
│ {                                                            │
│   "sub": "1",               // 用户ID                        │
│   "username": "admin",      // 用户名                        │
│   "roles": ["ADMIN"],       // 角色列表                      │
│   "iat": 1707474000,        // 签发时间                      │
│   "exp": 1707481200,        // 过期时间                      │
│   "jti": "uuid"             // Token ID                      │
│ }                                                            │
├──────────────────────────────────────────────────────────────┤
│ Signature: HMACSHA256(base64(header) + "." + base64(payload))│
└──────────────────────────────────────────────────────────────┘
```

### 3.2 双Token机制

| Token类型 | 有效期 | 用途 |
|-----------|--------|------|
| AccessToken | 2小时 | 接口认证 |
| RefreshToken | 7天 | 刷新AccessToken |

```java
// 颁发Token
public TokenVO createToken(LoginUser user) {
    String accessToken = Jwts.builder()
        .subject(user.getId().toString())
        .claim("username", user.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessExpire))
        .signWith(secretKey)
        .compact();

    String refreshToken = Jwts.builder()
        .subject(user.getId().toString())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshExpire))
        .signWith(secretKey)
        .compact();

    // 存储到Redis
    redisService.setLoginUser(accessToken, user, accessExpire);

    return new TokenVO(accessToken, refreshToken);
}
```

### 3.3 Token刷新

```java
@PostMapping("/auth/refresh")
public Result<TokenVO> refreshToken(@RequestParam String refreshToken) {
    // 验证RefreshToken
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(refreshToken)
        .getPayload();

    Long userId = Long.parseLong(claims.getSubject());
    LoginUser user = userService.getLoginUser(userId);

    // 生成新的AccessToken
    return Result.ok(tokenService.createToken(user));
}
```

---

## 4. 数据加密

### 4.1 敏感数据加密

| 数据类型 | 加密方式 |
|---------|---------|
| 密码 | BCrypt (不可逆) |
| 身份证 | AES-256-GCM |
| 手机号 | AES-256-GCM |
| 银行卡 | AES-256-GCM |

```java
@Component
public class CryptoService {

    @Value("${bml.crypto.aes-key}")
    private String aesKey;

    public String encrypt(String plainText) {
        return AES.encryptGCM(plainText, aesKey);
    }

    public String decrypt(String cipherText) {
        return AES.decryptGCM(cipherText, aesKey);
    }
}
```

### 4.2 传输安全

| 项目 | 要求 |
|------|------|
| 协议 | HTTPS强制 |
| TLS版本 | TLS 1.2+ |
| 证书 | Let's Encrypt 或企业CA |

---

## 5. 客户定制策略

### 5.1 定制方式

```
┌─────────────────────────────────────────────────────────────┐
│                    客户定制架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   标准版 (bml-modules)                                      │
│   ┌───────────────────────────────────────────────────────┐│
│   │  UserService (标准实现)                                ││
│   │  RoleService (标准实现)                                ││
│   │  MenuService (标准实现)                                ││
│   └───────────────────────────────────────────────────────┘│
│                          │                                  │
│                          ▼ @ConditionalOnMissingBean       │
│   定制版 (bml-customization/customer-xxx)                   │
│   ┌───────────────────────────────────────────────────────┐│
│   │  UserServiceCustom extends UserService                 ││
│   │  (覆盖特定方法)                                        ││
│   └───────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 配置驱动

```yaml
# application-customer-xxx.yml
bml:
  customer:
    code: XXX
    features:
      custom-login: true
      extended-user-fields: true
      special-workflow: true
```
