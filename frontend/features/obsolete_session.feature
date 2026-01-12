Feature: Obsolete session handling
  As a user with an obsolete session
  I want to be redirected to the login page
  So that I can log in again instead of seeing an error message

  Scenario: User with obsolete session is redirected to login
    Given the frontend application is started
    And an obsolete JWT cookie is set
    When I access the frontend root page
    Then I should be redirected to the login page
