Feature: Schema Listing Page
  As a user
  I want to see list of schemas in a selected database
  So that I can see the structure of the database.

  Scenario: Accessing schema listing page shows schemas
    Given a temporary Postgres instance is running
    And a Postgres user "testuser" with password "testpass" exists
    And a database "testdb" owned by "testuser" exists
    And I am logged in as "testuser" with password "testpass"
    And a schema "testschema" exists in database "testdb"
    When I access the schemas page for "testdb"
    Then I should see "testschema" on the page
    And I should see "public" on the page

  Scenario: Navigating from root page to schemas page
    Given a temporary Postgres instance is running
    And a Postgres user "testuser" with password "testpass" exists
    And a database "testdb" owned by "testuser" exists
    And I am logged in as "testuser" with password "testpass"
    And a schema "testschema" exists in database "testdb"
    When I access the frontend root page
    And I click on the link for database "testdb"
    Then I should see "testschema" on the page
