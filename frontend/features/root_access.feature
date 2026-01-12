Feature: Root Page Access
  As a user
  I want to be redirected to the login page if I'm not authenticated
  So that my data remains secure

  Scenario: Accessing root page without valid JWT redirects to login page
    Given the frontend application is started
    When I access the frontend root page
    Then I should be redirected to the login page

  Scenario: Accessing root page with a valid JWT lists databases
    Given a temporary Postgres instance is running
    And a Postgres user "testuser" with password "testpass" exists
    And a database "testdb" owned by "testuser" exists
    And I am logged in as "testuser" with password "testpass"
    When I access the frontend root page
    Then I should see "testdb" on the page
