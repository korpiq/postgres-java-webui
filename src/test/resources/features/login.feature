Feature: User Login with Postgres Credentials
  As a user of the web application
  I want to login with my Postgres database credentials
  So that I can access the database through the web interface

  Background:
    Given a temporary Postgres instance is running
    And a Postgres user "testuser" with password "testpass" exists

  Scenario: Successful login with valid Postgres credentials
    When I send a POST request to "/api/login" with credentials:
      | username | testuser |
      | password | testpass |
    Then the response status should be 200
    And the response should contain a valid JWT token
    And the JWT token should contain a session ID
    And the JWT token should be signed with the private key

  Scenario: Failed login with invalid password
    When I send a POST request to "/api/login" with credentials:
      | username | testuser    |
      | password | wrongpasswd |
    Then the response status should be 401
    And the response should contain an error message

  Scenario: Failed login with non-existent user
    When I send a POST request to "/api/login" with credentials:
      | username | nonexistent |
      | password | anypassword |
    Then the response status should be 401
    And the response should contain an error message
