# Simple Service that simulates Transferring Money Between Accounts

## Used Technologies
- [Kotlin 1.3](https://kotlinlang.org) - As nice Java replacement
- [Ktor](https://github.com/ktorio/ktor) - Kotlin async web framework
- [Exposed](https://github.com/JetBrains/Exposed) - Kotlin SQL framework
- [H2](https://github.com/h2database/h2database) - Embeddable database
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - High performance JDBC connection pooling
- [Netty](https://github.com/netty/netty) - Async web server
- [Jackson](https://github.com/FasterXML/jackson) - JSON serialization/deserialization

## Hot to run
`mvn clean package`
`java -jar ./target/example-0.0.1-jar-with-dependencies.jar`
or just
`java -jar money-transfer.jar`

## Available routes
| HTTP METHOD | PATH | DESCRIPTION | SUCCESS STATUS |
| -----------| ------ | ------ | ------ |
| POST | /accounts | creates new account | 201 - CREATED |
| PUT | /accounts/{id}/deposit/{amount} | deposits money to account | 204 - NO CONTENT | 
| GET | /accounts/{id} | get account by id |  202 - ACCEPTED | 
| POST | /transfer | perform money transferring between two accounts |  200 - OK | 

### Usage example

Lets create two accounts
```
POST http://localhost:8080/accounts
HTTP/1.1 201 Created

1
```
```
POST http://localhost:8080/accounts
HTTP/1.1 201 Created

2
```

Put some money to these newly created accounts:
```
PUT http://localhost:8080/accounts/1/deposit/101.98
HTTP/1.1 204 No Content

<Response body is empty>
```
```
PUT http://localhost:8080/accounts/2/deposit/456.33
HTTP/1.1 204 No Content

<Response body is empty>
```

Then check each accounts balance:
```
GET http://localhost:8080/accounts/1
HTTP/1.1 202 Accepted
Content-Type: application/json; charset=UTF-8

{
  "id": 1,
  "amount": 101.98000
}
```

```
GET http://localhost:8080/accounts/2
HTTP/1.1 202 Accepted
Content-Type: application/json; charset=UTF-8

{
  "id": 2,
  "amount": 456.33000
}
```

Now we ready to transfer some money:
```
POST http://localhost:8080/transfer
Content-Type: application/json

{
  "from": 1,
  "to": 2,
  "amount": 42.26
}

HTTP/1.1 200 Ok
<Response body is empty>
```

Now lets check both account balance again:
```
GET http://localhost:8080/accounts/1
HTTP/1.1 202 Accepted
Content-Type: application/json; charset=UTF-8

{
  "id": 1,
  "amount": 59.72000
}
```
```
GET http://localhost:8080/accounts/2
HTTP/1.1 202 Accepted
Content-Type: application/json; charset=UTF-8

{
  "id": 2,
  "amount": 498.59000
}
```