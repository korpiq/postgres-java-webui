Feature: List Tables in a Schema
  As a logged-in user
  I want to list the tables in a specific schema
  So that I can explore the data in that schema

  Background:
    Given a temporary Postgres instance is running
    And a Postgres user "table_user" with password "table_pass" exists
    And a database "table_test_db" exists
    And the user "table_user" has privilege to see the database "table_test_db"
    And I am logged in as "table_user" with password "table_pass"
    And a schema "table_test_schema" exists in database "table_test_db"
    And the user "table_user" has privilege to see the schema "table_test_schema" in "table_test_db"
    And a table "allowed_table" exists in schema "table_test_schema" in database "table_test_db"
    And a table "secret_table" exists in schema "table_test_schema" in database "table_test_db"
    And the user "table_user" has privilege to see the table "allowed_table" in schema "table_test_schema" in database "table_test_db"

  Scenario: User lists tables in a schema they have access to
    When I request the list of tables for schema "table_test_schema" in database "table_test_db"
    Then the response status should be 200
    And the response should contain table "allowed_table"
    And the response should not contain table "secret_table"
