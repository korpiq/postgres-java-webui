## To do

1. [x] Setup script to create and/or start empty local postgres instance
    - [x] ensure it creates a user with all privileges and known credentials
    - [x] create configuration file for java app containing the database address and credentials
    - [x] exclude configuration file from version control
2. [x] Create a public/private key pair for the Java app to sign JWTs with
    - [x] store it in the configuration file
3. [x] Create Gherkin test for Java API endpoint to login with credentials of a Postgres user
    - [x] test suite must create and teardown temporary postgres instance
    - [x] test that the login endpoint returns a valid JWT
        - [x] containing a session Id
        - [x] signed by a private key that matches the public key
4. [x] Implement Java API endpoint that produces a JWT associating session with Postgres user
5. [x] Create Gherkin test for Java API endpoint for listing databases in the Postgres instance
6. [x] Implement above API endpoint
    - [x] ensure it passes the test
7. [ ] Create a Gherkin test to verify that a webapp answers at a local port
8. [ ] Create a minimal empty ReactJs app to make previous test pass
9. [ ] Create a Gherkin test for a username+password login page
    - [ ] test must verify that the endpoint sets a JWT cookie
10. [ ] Implement the login page as specified by above test.
11. [ ] Create a Gherkin test to verify that accessing webapp root page without valid JWT goes to login page
12. [ ] Create a Gherkin test to verify that accessing webapp root page with a valid JWT lists databases the logged in user has access to.
13. [ ] Implement webapp root page so that above tests pass.
