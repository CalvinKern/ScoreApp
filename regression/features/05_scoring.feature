Feature: Entering scores
  Tapping a score opens the calculator keyboard, or the system keyboard when the game's Calculator Keyboard setting
  is off. Totals update as each score is typed. A score shows its result, and shows what was typed again when it's
  edited. Totals show up to two decimals.

  @calculator @smoke
  Scenario: The calculator opens for a score, and = saves it
    Given a game "Calc Basics" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    Then the calculator keyboard shows
    And the system keyboard doesn't show
    When I tap "1", "2", and "="
    Then the calculator keyboard hides
    And the round 1 scores are:
      | Calvin | Lindsay |
      | 12     | 0       |
    And the totals are:
      | Calvin | Lindsay |
      | 12     | 0       |

  @calculator
  Scenario: The total changes while typing, before = is pressed
    Given a game "Live Total" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    And I tap "4" and "0"
    Then the totals are:
      | Calvin | Lindsay |
      | 40     | 0       |
    When I tap "+"
    Then the totals are:
      | Calvin | Lindsay |
      | 40     | 0       |
    When I tap "2"
    Then the totals are:
      | Calvin | Lindsay |
      | 42     | 0       |

  @calculator @smoke
  Scenario Outline: Calculations are saved as their result (<input>)
    Given a game "Calc <result>" with players "Calvin"
    When I enter "<input>" for Calvin in round 1
    Then the round 1 scores are:
      | Calvin   |
      | <result> |

    Examples:
      | input            | result    |
      | 5×4+10+25        | 55        |
      | 2+3×4            | 14        |
      | (15+10)×2        | 50        |
      | 10-2-2           | 6         |
      | -5               | -5        |
      | -2-2             | -4        |
      | 10/4             | 2.5       |
      | 10/3             | 3.33      |
      | 2/3              | 0.67      |
      | 0.1+0.2          | 0.3       |
      | 500+300+300+215  | 1315      |
      | 999999×1000      | 999999000 |
      | 0                | 0         |

  @calculator
  Scenario: Editing a calculated score shows what was typed
    Given a game "Edit Input" with players "Calvin"
    And I enter "5×4+10+25" for Calvin in round 1
    When I tap the round 1 score for Calvin
    Then the score being edited shows "5×4+10+25"
    And the cursor is at the end
    When I tap "+", "5", and "="
    Then the round 1 scores are:
      | Calvin |
      | 60     |

  @calculator
  Scenario: Leaving a score without changing it keeps it exactly
    Given a game "Unchanged" with players "Calvin"
    And I enter "10/3" for Calvin in round 1
    When I tap the round 1 score for Calvin
    And I tap "="
    Then the round 1 scores are:
      | Calvin |
      | 3.33   |
    When I tap the round 1 score for Calvin
    Then the score being edited shows "10/3"

  @calculator
  Scenario: DEL works like backspace
    Given a game "Delete Key" with players "Calvin"
    When I tap the round 1 score for Calvin
    And I tap "1", "2", "3", "DEL", and "="
    Then the round 1 scores are:
      | Calvin |
      | 12     |

  @calculator
  Scenario: Clearing a score saves 0
    Given a game "Clear Score" with players "Calvin"
    And I enter "25" for Calvin in round 1
    When I tap the round 1 score for Calvin
    And I tap "DEL", "DEL", and "="
    Then the round 1 scores are:
      | Calvin |
      | 0      |

  @calculator
  Scenario: A score of 0 is empty while editing
    Given a game "Zero Empty" with players "Calvin"
    When I tap the round 1 score for Calvin
    Then the score being edited is empty

  @calculator @visual
  Scenario Outline: A calculation that can't be finished is saved as 0 and shown in red (<input>)
    Given a game "Bad Input" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    And I type "<input>" on the calculator
    And I tap "="
    Then the round 1 score for Calvin shows "<input>" in red
    And the totals are:
      | Calvin | Lindsay |
      | 0      | 0       |
    When I tap the round 1 score for Calvin
    Then the score being edited shows "<input>"
    When I clear the score and type "20" on the calculator
    And I tap "="
    Then the round 1 score for Calvin shows "20" in the normal color

    Examples:
      | input  |
      | 3×20+  |
      | 50/0   |
      | (5+    |

  @calculator @visual
  Scenario: An unfinished calculation says "Incomplete" while it's being edited
    # The error is a popup next to the score, which a view dump doesn't include, so check it from a screenshot. The
    # game isn't named "Incomplete", so the title can't pass for the error.
    Given a game "Unfinished Calc" with players "Calvin"
    When I tap the round 1 score for Calvin
    And I type "3×" on the calculator
    And I wait 2 seconds
    Then the score being edited shows the error "Incomplete"
    When I tap "4"
    Then the error goes away

  @system-keyboard @smoke
  Scenario: Without the calculator, scores use the system's number keyboard
    Given a game "System Keys" with players "Calvin, Lindsay" and settings:
      | Calculator Keyboard | off |
    When I tap the round 1 score for Calvin
    Then the system number keyboard shows
    And the calculator keyboard doesn't show
    When I type "7.5"
    And I tap the round 1 score for Lindsay
    And I type "-3"
    And I hide the keyboard
    Then the round 1 scores are:
      | Calvin | Lindsay |
      | 7.5    | -3      |
    And the totals are:
      | Calvin | Lindsay |
      | 7.5    | -3      |

  @system-keyboard
  Scenario: The system keyboard only types numbers
    Given a game "Only Numbers" with players "Calvin" and settings:
      | Calculator Keyboard | off |
    When I tap the round 1 score for Calvin
    And I type "12+3x"
    Then the score being edited shows "123"

  @smoke
  Scenario: Totals add up every round
    Given a game "Totals" with players "Calvin, Lindsay, Bob"
    When I play these rounds:
      | Calvin | Lindsay | Bob  |
      | 10     | 20      | 5    |
      | 15     | -5      | 30   |
      | 2.5    | 0       | 0.25 |
    Then the game has 3 rounds
    And the totals are:
      | Calvin | Lindsay | Bob   |
      | 27.5   | 15      | 35.25 |

  @visual @smoke
  Scenario: The highest total is leading
    Given a game "Leader High" with players "Calvin, Lindsay, Bob"
    When I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 30      | 20  |
    Then Lindsay is leading
    When I enter "35" for Bob in round 1
    Then Bob is leading

  @visual
  Scenario: With Reverse Scoring, the lowest total is leading
    Given a game "Leader Low" with players "Calvin, Lindsay, Bob" and settings:
      | Reverse Scoring | on |
    When I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 30      | 20  |
    Then Calvin is leading

  @visual
  Scenario: Players tied for the lead are all leading
    Given a game "Leader Tie" with players "Calvin, Lindsay, Bob"
    When I play these rounds:
      | Calvin | Lindsay | Bob |
      | 30     | 30      | 20  |
    Then Calvin and Lindsay are leading

  @visual
  Scenario: Nobody is leading when everyone is tied
    Given a game "All Tied" with players "Calvin, Lindsay, Bob"
    When I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 10      | 10  |
    Then nobody is leading

  @visual
  Scenario: A one player game has no leader
    Given a game "Solo" with players "Calvin"
    When I enter "50" for Calvin in round 1
    Then nobody is leading

  @visual
  Scenario: Negative totals lead correctly
    Given a game "Negatives" with players "Calvin, Lindsay"
    When I play these rounds:
      | Calvin | Lindsay |
      | -10    | -20     |
    Then Calvin is leading
    And the totals are:
      | Calvin | Lindsay |
      | -10    | -20     |

  Scenario: Scores are saved right away
    Given a game "Saved" with players "Calvin, Lindsay"
    When I enter "42" for Lindsay in round 1
    And I close and reopen the app
    And I open the game "Saved"
    Then the totals are:
      | Calvin | Lindsay |
      | 0      | 42      |

  @seed @calculator
  Scenario: Seeded calculations show their result, and what was typed when edited
    Given the seed data is loaded
    When I open the game "Phase 10"
    Then the round 1 score for alice shows "55"
    When I tap the round 1 score for alice
    Then the score being edited shows "5×4+10+25"

  @seed @system-keyboard
  Scenario: A repeating decimal shows two places
    Given the seed data is loaded
    When I open the game "Trivia Night"
    Then the round 4 score for Calvin shows "3.33"
    And the totals show "24.83" for Calvin

  @seed @visual
  Scenario: Seeded scores that couldn't be calculated are red
    Given the seed data is loaded
    When I open the game "Darts 301"
    Then the round 4 score for Taylor shows "3×20+" in red
    And the round 5 score for Sam shows "50/0" in red
    And the totals are:
      | Sam | Taylor |
      | 253 | 283    |

  @seed
  Scenario: Scores saved before typed input was kept can still be edited
    Given the seed data is loaded
    When I open the game "Phase 10" in row 27
    And I tap the round 1 score for Calvin
    Then the score being edited shows "50"
