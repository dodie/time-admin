Feature: User management
  In order to manage users
  As an Administrator
  I want to be able to list and edit users

  Scenario: List users
    Given that there are Registered users
    When I am on the Users page
    Then I see the registered users

  Scenario: View user
    Given there is a Registered user
    When I am on the Users page
    And I select the User
    Then I can see the details of that user

  Scenario: Edit user
    Given there is a Registered user
    When I am on the Users page
    And I select the User
    And I modify the User
    Then the user data should be modified

  Scenario: Grant Client role
    Given there is a New user
    When I am on the Users page
    And I add Client role to the User
    Then the user can see the Client pages

  Scenario: Grant Admin role
    Given there is a New user
    When I am on the Users page
    And I add Admin role to the User
    Then the user can see the Admin pages

  Scenario: Revoke Client role
    Given there is a Client user
    When I am on the Users page
    And I revoke the User's Client role
    Then the user can not see the Client pages

  Scenario: Revoke Admin role
    Given there is an Admin user
    When I am on the Users page
    And I revoke the User's Admin role
    Then the user can not see the Admin pages
