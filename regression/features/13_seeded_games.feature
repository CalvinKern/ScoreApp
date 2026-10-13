@seed
Feature: Seeded games
  Every game in scripts/test-data/seed.sql opens with the right players, rounds, totals, leader, dealer, and goal
  state. The games are realistic for each game's rules; scripts/test-data/README.md explains what each one tests.

  The expected values were computed from the seed with the app's rules: totals are every round added up, the best
  total leads (lowest with Reverse Scoring) unless everyone is tied, and "Finish Game" shows once a total reaches the
  Target Score or the game has Target Rounds rounds. Recompute them when the seed changes.

  Background:
    Given the seed data is loaded

  Scenario Outline: Row <row>, <game>
    When I open the game in row <row>
    Then the title is "<game>"
    And the game has <rounds> rounds
    And the totals read "<totals>"
    And the leading players are "<leading>"
    And the button after the rounds is "<button>"
    And the current dealer is "<dealer>"

    @smoke
    Examples: One of each kind
      | row | game            | rounds | totals                                                     | leading    | button      | dealer |
      | 2   | Phase 10        | 8      | Calvin 145, Lindsay 150, alice 225, Bob 255, Grandma Jo 80 | Grandma Jo | Add Round   | alice  |
      | 9   | Hearts          | 8      | Calvin 57, Bob 101, Zed 51, Priya 51                       | Zed, Priya | Finish Game | Zed    |
      | 15  | Agricola Solo   | 1      | Calvin 43                                                  | nobody     | Finish Game | nobody |
      | 22  | Darts 301       | 7      | Sam 253, Taylor 283                                        | Taylor     | Add Round   | nobody |
      | 30  | Old Hearts Save | 3      | Calvin 15, Bob 14, Zed 13                                  | Zed        | Add Round   | nobody |

    Examples: The rest
      | row | game                   | rounds | totals                                                                                                                                                                                                | leading                     | button      | dealer                      |
      | 1   | Yahtzee                | 1      | alice 0, Zoë 0, 🎲 Dice Goblin 0                                                                                                                                                                      | nobody                      | Add Round   | nobody                      |
      | 3   | Hand and Foot          | 4      | Team Red (Calvin & Lindsay) 3550, Team Blue (Bob & alice) 2730                                                                                                                                        | Team Red (Calvin & Lindsay) | Finish Game | Team Blue (Bob & alice)     |
      | 4   | Uno                    | 5      | Calvin 0, Lindsay 221, alice 0, Bob 0, Zed 139, José 0, Zoë 0, 李雷 104, Priya 0, Marcus 0                                                                                                            | Lindsay                     | Add Round   | Zoë                         |
      | 5   | Trivia Night           | 5      | Calvin 24.83, Lindsay 30.5, alice 30.5, Bob 23, Bartholomew Fitzgerald-Montgomery III 34, 🎲 Dice Goblin 18.5, Grandma Jo 37, Sam 26.5, Taylor 27.5, Morgan 18.5, Riley 31, Padfoot 26.5, Jordan 22.5 | Grandma Jo                  | Add Round   | nobody                      |
      | 6   | Gin Rummy              | 6      | Calvin 94, Lindsay 92                                                                                                                                                                                 | Calvin                      | Add Round   | Calvin                      |
      | 7   | Wingspan               | 6      | Lindsay 107, Priya 95, Morgan 101, Riley 93                                                                                                                                                           | Lindsay                     | Finish Game | nobody                      |
      | 8   | Ticket to Ride         | 3      | Calvin 85, Lindsay 67, Marcus 78, Priya 105, Sam 19                                                                                                                                                   | Priya                       | Finish Game | nobody                      |
      | 10  | Skull King             | 6      | Bob 150, Zed 80, Priya 40, Marcus 120, Padfoot 50                                                                                                                                                     | Bob                         | Add Round   | Bob                         |
      | 11  | Alhambra               | 3      | Lindsay 85, Bob 87, Zoë 78, Marcus 77                                                                                                                                                                 | Bob                         | Finish Game | nobody                      |
      | 12  | Mille Bornes           | 4      | Calvin 5125, José 3000, Taylor 3225                                                                                                                                                                   | Calvin                      | Finish Game | Calvin                      |
      | 13  | Planted                | 3      | alice 33, Zoë 33, 🎲 Dice Goblin 31                                                                                                                                                                   | alice, Zoë                  | Finish Game | nobody                      |
      | 14  | Agricola               | 1      | Calvin 40, Lindsay 35, Bob 9, Grandma Jo 62                                                                                                                                                           | Grandma Jo                  | Finish Game | nobody                      |
      | 16  | Golf                   | 9      | Lindsay 50, Zed 64, Sam 59, Jordan 77                                                                                                                                                                 | Lindsay                     | Finish Game | Zed                         |
      | 17  | Five Crowns            | 6      | Calvin 54, alice 64, Bob 51, Zoë 33, Priya 51, José 90                                                                                                                                                | Zoë                         | Add Round   | José                        |
      | 18  | Spades                 | 6      | Team Red (Calvin & Lindsay) 284, Team Blue (Bob & alice) 65                                                                                                                                           | Team Red (Calvin & Lindsay) | Add Round   | Team Blue (Bob & alice)     |
      | 19  | Rummy 500              | 6      | alice 305, José 115, Zoë 280                                                                                                                                                                          | alice                       | Add Round   | José                        |
      | 20  | Scrabble               | 25     | Lindsay 559, alice 499, 李雷 562                                                                                                                                                                      | 李雷                        | Add Round   | nobody                      |
      | 21  | Mexican Train          | 13     | Calvin 343, Grandma Jo 361, Sam 379, Taylor 458, Morgan 415                                                                                                                                           | Calvin                      | Finish Game | nobody                      |
      | 23  | Catan                  | 1      | Calvin 10, Lindsay 8, Bob 7, Marcus 9                                                                                                                                                                 | Calvin                      | Finish Game | nobody                      |
      | 24  | Cribbage               | 12     | Grandma Jo 123, Calvin 114                                                                                                                                                                            | Grandma Jo                  | Finish Game | Calvin                      |
      | 25  | Euchre                 | 8      | Team Red (Calvin & Lindsay) 8, Team Blue (Bob & alice) 4                                                                                                                                              | Team Red (Calvin & Lindsay) | Add Round   | Team Red (Calvin & Lindsay) |
      | 26  | Ticket to Ride         | 1      | Calvin 53, alice 36, Bob 54                                                                                                                                                                           | Bob                         | Add Round   | nobody                      |
      | 27  | Phase 10               | 6      | Calvin 245, Lindsay 260, Grandma Jo 275, Sam 205                                                                                                                                                      | Sam                         | Add Round   | Sam                         |
      | 28  | Quick Game (no rounds) | 1      | Morgan 0, Riley 0, Padfoot 0                                                                                                                                                                          | nobody                      | Add Round   | nobody                      |

  Scenario Outline: Row <row>, <game>, uses the <keyboard> and <notes icon>
    When I open the game in row <row>
    And I tap the first score in the last round
    Then the <keyboard> shows
    And the notes icon next to each name is <notes icon>

    Examples:
      | row | game                   | keyboard                | notes icon |
      | 1   | Yahtzee                | calculator keyboard     | shown      |
      | 2   | Phase 10               | calculator keyboard     | shown      |
      | 4   | Uno                    | calculator keyboard     | hidden     |
      | 5   | Trivia Night           | system number keyboard  | hidden     |
      | 9   | Hearts                 | system number keyboard  | hidden     |
      | 17  | Five Crowns            | system number keyboard  | shown      |
      | 21  | Mexican Train          | system number keyboard  | shown      |
      | 22  | Darts 301              | calculator keyboard     | hidden     |
      | 27  | Phase 10               | system number keyboard  | hidden     |
      | 30  | Old Hearts Save        | system number keyboard  | hidden     |

  Scenario: Every player's scores, even the ones scrolled out of view, add up in a wide game
    When I open the game "Trivia Night"
    And I scroll the scores all the way to the right
    Then I see "Jordan"
    And the totals show "22.5" for Jordan

  Scenario: The game whose players were all deleted opens Game Settings instead
    When I open the game in row 29
    Then the title is "Game Settings"
    And the game name field shows "Game Night (players gone)"
    And I see "No players added"

  Scenario: A deleted player's scores stay hidden in an old game
    When I open the game "Old Hearts Save"
    Then the players are "Calvin, Bob, Zed"
    And I don't see "Old Roommate"
