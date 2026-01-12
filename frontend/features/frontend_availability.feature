Feature: Frontend Availability
  As a developer
  I want to ensure the frontend application is running
  So that users can access the web interface

  Scenario: Frontend answers at local port
    Given the frontend application is started
    When I access the frontend root page
    Then I should see "Postgres Java WebUI" on the page
