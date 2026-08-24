# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=30000

app.upload.root=E:/images
app.profile-photo.root=E:/images/employee-photo
app.signature.root=E:/images/employee-signature
app.foreign-signature.root=E:/images/foreign-signatures


app.initial-admin-password=Admin@123

app.first-login.exempt-users=${FIRST_LOGIN_EXEMPT_USERS:admin,pd01,dgm,gm,branch,test}
