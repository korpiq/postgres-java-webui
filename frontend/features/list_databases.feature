Feature: Root Page Access
  As a user
  I want to see list of databases I am eligible to connect to
  So that I can choose which database to work with.

  Scenario: Accessing root page with a valid JWT lists databases
    Given a temporary Postgres instance is running
    And a Postgres user "testuser" with password "testpass" exists
    And a database "testdb" owned by "testuser" exists
    And I am logged in as "testuser" with password "testpass"
    When I access the frontend root page
    Then I should see "testdb" on the page
