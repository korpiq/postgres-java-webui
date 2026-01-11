## To do

1. [x] Setup script to create and/or start empty local postgres instance
    - [x] ensure it creates a user with all privileges and known credentials
    - [x] create configuration file for java app containing the database address and credentials
    - [x] exclude configuration file from version control
2. [x] Create a public/private key pair for the Java app to sign JWTs with
    - [x] store it in the configuration file
2. [ ] Create Gherkin test for Java API endpoint to login with credentials of a Postgres user
    - [ ] test suite must create and teardown temporary postgres instance
    - [ ] test that the login endpoint returns a valid JWT
        - [ ] containing a session Id
            - [ ] mapped to a Postgres connection logged in with the given credentials
        - [ ] signed by a private key that
3. [ ] Implement Java API endpoint that produces a JWT associating session with Postgres user
4. [ ] Create Gherkin test for Java API endpoint for listing databases in the Postgres instance
    - [ ] test should setup and teardown
      - [ ] a database in the test postgres instance
      - [ ] a user in the test postgres instance
      - [ ] a privilege for the user to see the database in postgres
    - [ ] test should ensure that user sees database only when given privilege for that.
6. [ ] Implement above API endpoint
    - [ ] ensure it passes the test
8. [ ] Create a Gherkin test to verify that a webapp answers at a local port
9. [ ] Create a minimal empty ReactJs app to make previous test pass
10. [ ] Create a Gherkin test for a username+password login page
    - [ ] test must verify that the endpoint sets a JWT cookie
11. [ ] Implement the login page as specified by above test.
12. [ ] Create a Gherkin test to verify that accessing webapp root page without valid JWT goes to login page
13. [ ] Create a Gherkin test to verify that accessing webapp root page with a valid JWT lists databases the logged in user has access to.
14. [ ] Implement webapp root page so that above tests pass.
