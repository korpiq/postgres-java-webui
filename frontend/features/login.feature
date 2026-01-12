Feature: Login Page
  As a user
  I want to login with my Postgres credentials on the web interface
  So that I can manage my databases

  Scenario: Successful login via web form sets JWT cookie
    Given a temporary Postgres instance is running
    And a Postgres user "db_user" with password "db_pass" exists
    And I am on the login page
    When I enter "db_user" and "db_pass" and click login
    Then a JWT cookie should be set
    And the JWT cookie should be signed with the backend secret key
