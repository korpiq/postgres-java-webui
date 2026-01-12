Feature: Login Page
  As a user
  I want to login with my Postgres credentials on the web interface
  So that I can manage my databases

  Scenario: Successful login via multi-step web form sets database-specific cookie
    Given a temporary Postgres instance is running
    And a Postgres user "db_user" with password "db_pass" exists
    And a database "testdb" exists
    And I am on the login page
    When I enter "db_user" and "db_pass" and click "List Databases"
    Then I should see a list of databases including "testdb"
    When I select "testdb" and click "Connect"
    Then I should be redirected to "/db/testdb"
    And a cookie "pogrejab_testdb" should be set
