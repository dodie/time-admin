Feature: Authentication
  In order to have access to the right features
  As an Administrator, Client or future user of the Timeadmin
  I want to have authentication and authorization

  Scenario: Unauthenticated user redirected to the login page
    Given I am an Unauthenticated user
    When I go to Timeadmin
    Then I see the Login page

  Scenario: Unauthenticated user can't access to Timeadmin features
    Given I am an Unauthenticated user
    Then I can not see the Client pages
    And I can not see the Admin pages
    And I can not see the User pages
    But I can see the Registration page

  Scenario: Registration
    Given I am an Unauthenticated user
    When I register an account
    Then I see a nice welcome message
    And I can not see the Client pages
    And I can not see the Admin pages
    But I can see the User pages

  Scenario: Becoming a Client
    Given I am a New user
    When an administrator grant me Client permission
    And I log in
    Then I can see the Client pages

  Scenario: Becoming an Admin
    Given I am a New user
    When an administrator grant me Admin permission
    And I log in
    Then I can see the Admin pages
