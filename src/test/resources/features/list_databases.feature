Feature: List Databases in Postgres Instance
  As a logged-in user
  I want to list the databases I have access to
  So that I can manage my databases through the web interface

  Background:
    Given a temporary Postgres instance is running
    And a Postgres user "db_user" with password "db_pass" exists
    And a database "allowed_db" exists
    And a database "secret_db" exists
    And the user "db_user" has privilege to see the database "allowed_db"
    And I am logged in as "db_user" with password "db_pass"

  Scenario: User lists databases they have access to
    When I request the list of databases
    Then the response status should be 200
    And the response should contain "allowed_db"
    And the response should not contain "secret_db"
