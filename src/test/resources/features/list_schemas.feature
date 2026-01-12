Feature: List Schemas in a Database
  As a logged-in user
  I want to list the schemas in a specific database
  So that I can explore the structure of that database

  Background:
    Given a temporary Postgres instance is running
    And a Postgres user "schema_user" with password "schema_pass" exists
    And a database "schema_test_db" exists
    And the user "schema_user" has privilege to see the database "schema_test_db"
    And I am logged in as "schema_user" with password "schema_pass"
    And a schema "allowed_schema" exists in database "schema_test_db"
    And a schema "secret_schema" exists in database "schema_test_db"
    And the user "schema_user" has privilege to see the schema "allowed_schema" in "schema_test_db"

  Scenario: User lists schemas in a database they have access to
    When I request the list of schemas for database "schema_test_db"
    Then the response status should be 200
    And the response should contain schema "allowed_schema"
    And the response should not contain schema "secret_schema"
