Feature: Setting up a new game
  The + on the game list opens "New Game": a name, players, and settings. "Save" creates the game and opens it with
  its first round. Going back without saving creates nothing.

  @smoke
  Scenario: Creating a game opens it with one empty round
    Given the players "Calvin, Lindsay, Bob" exist
    And I am on the game list
    When I tap the + button
    Then the title is "New Game"
    And the game name field has the keyboard
    When I type "Setup Test" as the game name
    And I tap "EDIT"
    And I pick "Calvin", "Lindsay", and "Bob"
    And I tap "Save"
    And I tap "Save"
    Then the title is "Setup Test"
    And the game has 1 round
    And the totals are:
      | Calvin | Lindsay | Bob |
      | 0      | 0       | 0   |
    And nobody is leading
    And the button after the rounds is "Add Round"
    When I go back
    Then the title is "Games"

  Scenario: A new game starts with no players
    Given I am on the game list
    When I tap the + button
    Then I see "No players added"
    And I see the message that players are added with the "EDIT" button

  Scenario: The empty players picture also opens the player picker
    Given I am on the game list
    When I tap the + button
    And I tap the picture above "No players added"
    Then the title is "Pick Players"

  Scenario: A game needs a name
    Given the players "Calvin" exist
    And I am on the game list
    When I tap the + button
    And I tap "EDIT"
    And I pick "Calvin"
    And I tap "Save"
    And I tap "Save"
    Then I see "Please enter a name for the game"
    And the title is "New Game"

  Scenario: A name of only spaces isn't a name
    Given the players "Calvin" exist
    And I am on the game list
    When I tap the + button
    And I type "   " as the game name
    And I tap "EDIT"
    And I pick "Calvin"
    And I tap "Save"
    And I tap "Save"
    Then I see "Please enter a name for the game"

  Scenario: A game needs a player
    Given I am on the game list
    When I tap the + button
    And I type "No Players" as the game name
    And I tap "Save"
    Then I see "Please add a player"
    And the title is "New Game"

  Scenario: Going back from a new game creates nothing
    Given the players "Calvin" exist
    And I am on the game list
    When I tap the + button
    And I type "Never Saved" as the game name
    And I tap "EDIT"
    And I pick "Calvin"
    And I tap "Save"
    And I go back
    Then the title is "Games"
    And I don't see "Never Saved"

  @smoke
  Scenario: Every setting is saved with the game
    Given a game "All Settings" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer              | on      |
      | First dealer        | Lindsay |
      | Reverse Scoring     | on      |
      | Show Round Notes    | on      |
      | Calculator Keyboard | off     |
      | Target Score        | 250     |
      | Target Rounds       | 12      |
    When I choose "Edit" from the game menu
    Then the title is "Game Settings"
    And these settings are shown:
      | Dealer              | on  |
      | Reverse Scoring     | on  |
      | Show Round Notes    | on  |
      | Calculator Keyboard | off |
      | Target Score        | 250 |
      | Target Rounds       | 12  |

  Scenario: Tapping a setting's row or its checkbox both toggle it
    Given I am on the game list
    When I tap the + button
    And I tap the row "Reverse Scoring"
    Then "Reverse Scoring" is on
    When I tap the checkbox for "Reverse Scoring"
    Then "Reverse Scoring" is off

  Scenario: New games have a dealer, the first player unless another is picked
    Given the players "Calvin, Lindsay, Bob" exist
    And I am on the game list
    When I tap the + button
    Then "Dealer" is on
    When I tap "EDIT"
    And I pick "Calvin", "Lindsay", and "Bob"
    And I tap "Save"
    Then each player has a dealer radio button
    And "Calvin" is marked "Dealer"
    When I tap the row "Bob"
    Then "Bob" is marked "Dealer"
    And "Calvin" is not marked "Dealer"

  @smoke
  Scenario: The picked dealer deals the first round
    Given a game "Dealer Test" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer       | on      |
      | First dealer | Lindsay |
    Then Lindsay is the dealer

  Scenario: Without a dealer, no one has the dealer border and no radio buttons show
    Given the players "Calvin, Lindsay" exist
    And I am on the game list
    When I tap the + button
    And I tap the row "Dealer"
    Then "Dealer" is off
    When I tap "EDIT"
    And I pick "Calvin" and "Lindsay"
    And I tap "Save"
    Then no player has a dealer radio button
    When I type "No Dealer" as the game name
    And I tap "Save"
    Then nobody has the dealer border

  Scenario: Removing the picked dealer makes the first player the dealer
    Given the players "Calvin, Lindsay, Bob" exist
    And I am on the game list
    When I tap the + button
    And I tap "EDIT"
    And I pick "Calvin", "Lindsay", and "Bob"
    And I tap "Save"
    And I tap the row "Bob"
    And I tap "EDIT"
    And I tap the row "Bob"
    And I tap "Save"
    Then "Calvin" is marked "Dealer"

  Scenario: Dragging a player changes the seat order
    Given the players "Calvin, Lindsay, Bob" exist
    And I am on the game list
    When I tap the + button
    And I type "Seat Order" as the game name
    And I tap "EDIT"
    And I pick "Calvin", "Lindsay", and "Bob"
    And I tap "Save"
    And I drag "Bob" above "Calvin"
    Then the players are "Bob, Calvin, Lindsay"
    When I tap "Save"
    Then the totals are:
      | Bob | Calvin | Lindsay |
      | 0   | 0      | 0       |

  Scenario Outline: A target of 0 means no target
    Given I am on the game list
    When I tap the + button
    And I type "0" into "<field>"
    And I tap the game name field
    Then "<field>" is empty

    Examples:
      | field         |
      | Target Score  |
      | Target Rounds |

  Scenario: A target score can have decimals
    Given a game "Decimal Target" with players "Calvin" and settings:
      | Target Score | 10.5 |
    When I enter "10" for Calvin in round 1
    Then the button after the rounds is "Add Round"
    When I enter "10.5" for Calvin in round 1
    Then the button after the rounds is "Finish Game"

  Scenario: The calculator keyboard setting starts from Settings
    Given I am on the game list
    When I tap the Settings icon
    And I turn off "Calculator Keyboard"
    And I go back
    And I tap the + button
    Then "Calculator Keyboard" is off
    When I go back
    And I tap the Settings icon
    And I turn on "Calculator Keyboard"
    And I go back
    And I tap the + button
    Then "Calculator Keyboard" is on

  @seed @smoke
  Scenario: Picking a previous game's name loads its settings and players
    Given the seed data is loaded
    When I tap the + button
    And I type "Ticket" as the game name
    Then "Ticket to Ride" is suggested once
    When I pick the suggestion "Ticket to Ride"
    Then the players are "Calvin, Lindsay, Marcus, Priya, Sam"
    And these settings are shown:
      | Dealer              | off |
      | Reverse Scoring     | off |
      | Show Round Notes    | on  |
      | Calculator Keyboard | on  |
      | Target Score        |     |
      | Target Rounds       | 3   |
    When I tap "Save"
    Then the title is "Ticket to Ride"
    And the totals are:
      | Calvin | Lindsay | Marcus | Priya | Sam |
      | 0      | 0       | 0      | 0     | 0   |

  @seed
  Scenario: The keyboard's done key closes the name suggestions
    Given the seed data is loaded
    When I tap the + button
    And I type "Ticket" as the game name
    Then "Ticket to Ride" is suggested once
    When I tap the keyboard's done key
    Then no suggestions show
    And the game name is "Ticket"
    And I see "No players added"

  @seed
  Scenario: Previous settings don't replace players already picked
    Given the seed data is loaded
    When I tap the + button
    And I tap "EDIT"
    And I pick "Zed"
    And I tap "Save"
    And I type "Ticket" as the game name
    And I pick the suggestion "Ticket to Ride"
    Then the players are "Zed"
    And "Target Rounds" is empty

  @seed
  Scenario: Previous settings aren't loaded when Load Previous Settings is off
    Given the seed data is loaded
    When I tap the Settings icon
    And I turn off "Load Previous Settings"
    And I go back
    And I tap the + button
    And I type "Hear" as the game name
    And I pick the suggestion "Hearts"
    Then I see "No players added"
    And "Reverse Scoring" is off
