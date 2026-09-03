/*
 Legacy entry point retained for documentation.

 Database creation and schema installation are now automatic at application
 startup. The executable initial schema is maintained as the Flyway migration:

   src/main/resources/db/migration/V1__initial_schema.sql

 Flyway then applies V2 and every later migration in version order. Do not run
 this file manually.
*/
