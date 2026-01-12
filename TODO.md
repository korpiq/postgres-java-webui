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
7. [x] Create a method for us to start frontend React app
    - [x] from command-line
    - [x] in frontend test setup and teardown
    - [x] initially showing an empty front page
8. [x] Create a Gherkin test to verify that our webapp answers at a local port
9. [x] Create a Gherkin test for a username+password login page
    - [x] test must verify with the public key from .secrets that the endpoint sets a JWT cookie signed by the backend secret key
10. [x] Implement the login page as specified by above test.
11. [x] Create a Gherkin test to verify that accessing webapp root page without valid JWT goes to login page
12. [x] Create a Gherkin test to verify that accessing webapp root page with a valid JWT lists databases the logged in user has access to.
    - [x] frontend test suite should setup and teardown specific user and database for use in this and future tests.
13. [x] Implement webapp root page so that above tests pass.
    - [x] use database listing endpoint of the API
      - [x] pass JWT from cookie as Authorization header.
14. [x] Add feature test and implementation for an API endpoint that lists schemas in a database
    - [x] require JWT in Authorization header, use db connection of associated session to limit to schemas seen by that user.
15. [x] Add feature test and implementation for a frontend page '/db/<dbName>' that lists schemas of database `dbName` from above API endpoint.
16. [x] Modify front page so that each database name `dbName` is a link to `/db/<dbName>` page.
17. [x] Add feature test and implementation for an API endpoint that lists tables in a database schema
    - [x] again use JWT from Authorization header for session db connection
    - [x] again ensure that user does not see tables they have no permission to access
18. [ ] Add feature test and implementation for a frontend page `/db/<dbName>/<schemaName>` that lists tables in that schema from the API.
19. [ ] Add information whether the user has permission to create new database to the database list response from API.
    - [ ] Adjust existing code and tests to work correctly after the change.
20. [ ] Add feature test and implementation for API endpoint to create a database
21. [ ] Add feature test and implementation for a form on frontend database list page to create a database
    - [ ] only shown to users who have the privilege to create a database
