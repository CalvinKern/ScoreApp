Feature: Picking players
  "Pick Players" opens from "EDIT" on a game's setup. Players are shared by every game, and listed by name ignoring
  case. Deleting a player archives them: they leave the list and every game, and their scores are kept.

  "I am picking players for a new game" means: from the game list, tap +, then "EDIT".

  @smoke
  Scenario: Adding a new player picks them right away
    Given I am picking players for a new game
    When I tap the + button
    Then I see "New Player"
    And "OK" is disabled
    When I type "Quinn"
    Then "OK" is enabled
    When I tap "OK"
    Then "Quinn" is in the list and checked
    When I tap "Save"
    Then the title is "New Game"
    And the players are "Quinn"

  Scenario: Cancelling a new player adds no one
    Given I am picking players for a new game
    When I tap the + button
    And I type "Nobody"
    And I tap "Cancel"
    Then I don't see "Nobody"

  Scenario: Tapping outside the new player dialog doesn't close it
    Given I am picking players for a new game
    When I tap the + button
    And I tap outside the dialog
    Then I see "New Player"

  @seed
  Scenario: Players are sorted by name ignoring case, and deleted players are hidden
    Given the seed data is loaded
    And I am picking players for a new game
    Then the first players listed are:
      | alice                                 |
      | Avery                                 |
      | Bartholomew Fitzgerald-Montgomery III |
      | Bob                                   |
      | Calvin                                |
    And the last players listed are:
      | 李雷           |
      | 🎲 Dice Goblin |
    And I see "Never Played"
    And I don't see "Old Roommate"
    And I don't see "Moved Away"
    And I don't see "Ex-Coworker"

  Scenario: A new player is placed in name order
    Given the players "Amy, Carl" exist
    And I am picking players for a new game
    When I add the player "bea"
    Then "bea" is listed between "Amy" and "Carl"

  @smoke
  Scenario: Tapping a row or its checkbox picks and unpicks the player
    Given the players "Calvin, Kim" exist
    And I am picking players for a new game
    When I tap the row "Calvin"
    Then "Calvin" is checked
    When I tap the checkbox for "Calvin"
    Then "Calvin" is not checked

  Scenario: Players keep their order, and new picks go at the end
    Given the players "Calvin, Kim, Bob" exist
    And I am picking players for a new game
    When I pick "Kim"
    And I pick "Calvin"
    And I tap "Save"
    Then the players are "Kim, Calvin"
    When I tap "EDIT"
    Then "Kim" and "Calvin" are checked
    When I pick "Bob"
    And I tap "Save"
    Then the players are "Kim, Calvin, Bob"

  Scenario: Unpicking a player removes them from the game being set up
    Given the players "Calvin, Kim" exist
    And I am picking players for a new game
    When I pick "Calvin" and "Kim"
    And I tap "Save"
    And I tap "EDIT"
    And I tap the row "Kim"
    And I tap "Save"
    Then the players are "Calvin"

  Scenario: Going back from the player picker keeps the players the game had
    Given the players "Calvin, Kim" exist
    And I am picking players for a new game
    When I pick "Calvin"
    And I tap "Save"
    And I tap "EDIT"
    And I pick "Kim"
    And I go back
    Then the title is "New Game"
    And the players are "Calvin"

  Scenario Outline: Renaming a player (<how>)
    Given the players "Renamee" exist
    And I am picking players for a new game
    When I <how> "Renamee"
    Then I see "Player Name"
    And the name field shows "Renamee"
    When I replace the name with "Zelda Renamed"
    And I tap "OK"
    Then I see "Zelda Renamed"
    And I don't see "Renamee"
    And "Zelda Renamed" is listed in name order

    Examples:
      | how                            |
      | long press the row             |
      | tap the edit icon next to      |

  Scenario: The rename dialog's OK is disabled for an empty name
    Given the players "Calvin" exist
    And I am picking players for a new game
    When I long press the row "Calvin"
    And I clear the name
    Then "OK" is disabled

  @seed
  Scenario: A renamed player is renamed in every game
    Given the seed data is loaded
    And I am picking players for a new game
    When I long press the row "Grandma Jo"
    And I replace the name with "Grandma Josephine"
    And I tap "OK"
    And I go back to the game list
    And I open the game "Cribbage"
    Then the players are "Grandma Josephine, Calvin"
    And the totals are:
      | Grandma Josephine | Calvin |
      | 123               | 114    |

  @smoke
  Scenario: Deleting a player can be undone, and they stay picked
    Given the players "Calvin, Undo Me" exist
    And I am picking players for a new game
    When I pick "Undo Me"
    And I long press the row "Undo Me"
    And I tap "Delete"
    Then I see "Deleted Undo Me"
    And I don't see the row "Undo Me"
    When I tap "Undo"
    Then "Undo Me" is in the list and checked

  @seed
  Scenario: A deleted player leaves their games
    Given the seed data is loaded
    And I am picking players for a new game
    When I long press the row "Taylor"
    And I tap "Delete"
    And I go back to the game list
    Then the game "Darts 301" shows "1 Player"
    When I open the game "Darts 301"
    Then the players are "Sam"
    And the totals are:
      | Sam |
      | 253 |

  @seed
  Scenario: Undoing a delete puts the player back in their games with their scores
    Given the seed data is loaded
    And I am picking players for a new game
    When I long press the row "Taylor"
    And I tap "Delete"
    And I tap "Undo"
    And I go back to the game list
    And I open the game "Darts 301"
    Then the totals are:
      | Sam | Taylor |
      | 253 | 283    |

  Scenario: Searching filters the players by any part of the name, ignoring case
    Given the players "Calvin, Kim, Marcus" exist
    And I am picking players for a new game
    When I tap the search icon
    And I type "A"
    Then I see "Calvin"
    And I see "Marcus"
    And I don't see the row "Kim"

  Scenario: A search with no matches says so
    Given the players "Calvin" exist
    And I am picking players for a new game
    When I tap the search icon
    And I type "zzzz"
    Then I see "No players match"
    When I clear the search
    Then I see "Calvin"

  Scenario: A player added during a search shows when the name matches it
    Given I am picking players for a new game
    When I tap the search icon
    And I type "Zan"
    And I add the player "Zander"
    Then "Zander" is in the list and checked
