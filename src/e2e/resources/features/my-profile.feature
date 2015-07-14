Feature: My profile
  In order to update my personal details or login credentials
  As a User
  I want to have access to my Profile

  Scenario: Update first and last name
    Given I am a User
    When I change my first and last name
    Then my first and last name should be updated

  Scenario: Change the language of the user interface
    Given I am a User
    When I change the localization to Hungarian
    Then the text on the user interface should appear in that language

  Scenario: Update e-mail to log in with a different address
    Given I am a User
    When I change my e-mail address
    Then I can log in with my new e-mail address

  Scenario: Modify password to log in with a different password
    Given I am a User
    When I change my password
    Then I can log in with my new password
