# MySQL 설치와 또박 데이터베이스 만들기 (Windows)

또박 백엔드는 MySQL에 데이터를 저장해요. PC마다 한 번만 하면 돼요. (테스트는 메모리 DB를 써서 MySQL 없이도 돌아가요)

## 1. MySQL 설치

1. https://dev.mysql.com/downloads/mysql/ 에서 **MySQL Community Server**의 **8.4 LTS**를 고르고, Windows용 **MSI Installer**를 받아요. (Oracle 로그인은 "No thanks, just start my download"로 건너뛸 수 있어요)
2. 설치 형식은 **Typical**로 설치해요.
3. 설치가 끝나면 **MySQL Configurator**가 열려요. 아래만 확인하고 나머지는 그대로 **Next**:
   - Type and Networking: 포트 **3306**
   - Accounts and Roles: **root 비밀번호**를 정해서 입력 (잊지 않게 따로 적어 두기)
   - Windows Service: "Configure MySQL Server as a Windows Service", "Start the MySQL Server at System Startup" 켜 둔 그대로 (PC를 켜면 MySQL도 자동으로 켜져요)
   - 마지막 **Execute** → **Finish**

## 2. 또박 데이터베이스와 계정 만들기

시작 메뉴에서 **MySQL 8.4 Command Line Client**를 열고 root 비밀번호를 입력한 뒤, 아래를 붙여 넣어요.
`여기에_DB_비밀번호`는 새로 정하는 비밀번호로 바꾸세요. (root 비밀번호와 달라도 돼요)

```sql
CREATE DATABASE ddobak DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'ddobak'@'localhost' IDENTIFIED BY '여기에_DB_비밀번호';
GRANT ALL PRIVILEGES ON ddobak.* TO 'ddobak'@'localhost';
```

`Query OK`가 세 번 나오면 끝이에요. 표는 백엔드가 켜질 때 자동으로 만들어요(`ddl-auto: update`).

## 3. 백엔드에 비밀번호 알려 주기

백엔드 폴더의 `secret.yaml`(GitHub에 안 올라감)에 추가해요:

```yaml
spring:
  datasource:
    password: "여기에_DB_비밀번호"
```

## 안 될 때

| 백엔드 창에 나오는 말 | 이유 | 해결 |
| --- | --- | --- |
| `Communications link failure` | MySQL이 꺼져 있음 | Windows 검색 → "서비스" → `MySQL84` 시작 |
| `Access denied for user 'ddobak'@'localhost'` | 비밀번호가 다름 | `secret.yaml`의 비밀번호 확인 |
| `Unknown database 'ddobak'` | 2번을 안 함 | 2번의 SQL 실행 |

## 안을 들여다보고 싶으면

Command Line Client에서:

```sql
USE ddobak;
SHOW TABLES;
SELECT id, email, nickname, LEFT(password_hash, 20) FROM users;
```
