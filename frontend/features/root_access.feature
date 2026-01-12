Feature: Root Page Access
  As a user
  I want to be redirected to the login page if I'm not authenticated
  So that my data remains secure

  Scenario: Accessing root page without valid JWT redirects to login page
    Given the frontend application is started
    When I access the frontend root page
    Then I should be redirected to the login page
