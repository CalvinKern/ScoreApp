Feature: Round notes
  Tapping a player's name opens their notes, one per round. The rounds are listed in order, opening at the newest at
  the bottom. Notes are only saved with "Save". Show Round Notes only adds a notes icon next to each name; the notes
  open either way.

  @smoke
  Scenario: Writing a note for a round
    Given a game "Notes" with players "Calvin, Lindsay" and settings:
      | Show Round Notes | on |
    And I tap "Add Round"
    When I tap the name "Lindsay"
    Then I see "Notes for Lindsay"
    And I see "Round 1:" and "Round 2:"
    And "Round 2:" is at the bottom
    When I type "Phase 3 done" into the "Round 2:" note
    And I tap "Save"
    Then I don't see "Notes for Lindsay"
    When I tap the name "Lindsay"
    Then the "Round 2:" note is "Phase 3 done"
    And the "Round 1:" note is empty

  Scenario: Notes belong to one player
    Given a game "Own Notes" with players "Calvin, Lindsay"
    And I tap the name "Lindsay"
    And I type "Lindsay's note" into the "Round 1:" note
    And I tap "Save"
    When I tap the name "Calvin"
    Then the "Round 1:" note is empty

  Scenario: Cancel throws away changes
    Given a game "Cancel Notes" with players "Calvin"
    When I tap the name "Calvin"
    And I type "Not saved" into the "Round 1:" note
    And I tap "Cancel"
    And I tap the name "Calvin"
    Then the "Round 1:" note is empty

  Scenario: The notes stay open until Save or Cancel
    Given a game "Sticky Notes" with players "Calvin"
    When I tap the name "Calvin"
    And I type "Half written" into the "Round 1:" note
    And I go back
    Then I see "Notes for Calvin"
    When I tap outside the dialog
    Then I see "Notes for Calvin"

  @visual
  Scenario Outline: The notes icon follows Show Round Notes, and notes open either way (<setting>)
    Given a game "Notes Icon <setting>" with players "Calvin" and settings:
      | Show Round Notes | <setting> |
    Then the notes icon next to "Calvin" is <icon>
    When I tap the name "Calvin"
    Then I see "Notes for Calvin"

    Examples:
      | setting | icon    |
      | on      | shown   |
      | off     | hidden  |

  Scenario: Notes are kept when the game is reopened
    Given a game "Kept Notes" with players "Calvin"
    And I tap the name "Calvin"
    And I type "Remember me" into the "Round 1:" note
    And I tap "Save"
    When I close and reopen the app
    And I open the game "Kept Notes"
    And I tap the name "Calvin"
    Then the "Round 1:" note is "Remember me"

  Scenario: Deleting a round deletes its notes, and the later notes move up a round
    Given a game "Note Rounds" with players "Calvin"
    And I tap "Add Round"
    And I tap the name "Calvin"
    And I type "First" into the "Round 1:" note
    And I type "Second" into the "Round 2:" note
    And I tap "Save"
    When I choose "Delete Rounds" from the game menu
    And I check "Round 1"
    And I tap "Delete"
    And I tap the name "Calvin"
    Then I see "Round 1:"
    And I don't see "Round 2:"
    And the "Round 1:" note is "Second"

  @seed
  Scenario: Seeded notes
    Given the seed data is loaded
    When I open the game "Phase 10"
    And I tap the name "Calvin"
    Then the "Round 1:" note is "Phase 1 ✓, went out"
    And the "Round 2:" note is "Phase 2 ✓"
    And the "Round 8:" note is empty

  @seed
  Scenario: Seeded notes in a game played as scoring categories
    Given the seed data is loaded
    When I open the game "Wingspan"
    And I tap the name "Lindsay"
    Then the notes for rounds 1 to 6 are "Birds", "Bonus cards", "End-of-round goals", "Eggs", "Cached food", and "Tucked cards"
