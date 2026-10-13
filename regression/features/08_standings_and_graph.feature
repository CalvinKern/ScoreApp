Feature: Standings and the graph
  The trophy ("Standings") shows every player's place at any time. "Show Graph" shows each player's running total by
  round, and a podium of everyone's totals, best first.

  @smoke
  Scenario: Standings list players from best to worst
    Given a game "Standings High" with players "Calvin, Lindsay, Bob"
    And I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 30      | 20  |
      | 5      | 5       | 5   |
    When I choose "Standings" from the game menu
    Then I see "Player Standings"
    And I see "2 Rounds Played"
    And the standings are:
      | place | player  | total |
      | #1    | Lindsay | 35    |
      | #2    | Bob     | 25    |
      | #3    | Calvin  | 15    |

  Scenario: With Reverse Scoring, the lowest total is first
    Given a game "Standings Low" with players "Calvin, Lindsay, Bob" and settings:
      | Reverse Scoring | on |
    And I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 30      | 20  |
    When I choose "Standings" from the game menu
    Then the standings are:
      | place | player  | total |
      | #1    | Calvin  | 10    |
      | #2    | Bob     | 20    |
      | #3    | Lindsay | 30    |

  Scenario: Tied players share a place, and the next place is skipped
    Given a game "Standings Tie" with players "Calvin, Lindsay, Bob, Zed"
    And I play these rounds:
      | Calvin | Lindsay | Bob | Zed |
      | 20     | 20      | 10  | 5   |
    When I choose "Standings" from the game menu
    Then the standings are:
      | place | player  | total |
      | #1    | Calvin  | 20    |
      | #1    | Lindsay | 20    |
      | #3    | Bob     | 10    |
      | #4    | Zed     | 5     |

  Scenario: Standings count one round for a new game
    Given a game "Standings New" with players "Calvin"
    When I choose "Standings" from the game menu
    Then I see "1 Round Played"
    And the standings are:
      | place | player | total |
      | #1    | Calvin | 0     |

  Scenario: Closing the standings returns to the game
    Given a game "Standings Close" with players "Calvin"
    When I choose "Standings" from the game menu
    And I tap "Close"
    Then the title is "Standings Close"

  Scenario: Show Graph from the standings opens the graph
    Given a game "Standings Graph" with players "Calvin"
    When I choose "Standings" from the game menu
    And I tap "Show Graph"
    Then the title is "Standings Graph"
    And I see "Statistics"
    And I see "Score by rounds"

  @smoke @visual
  Scenario: The graph shows running totals by round
    Given a game "Graph Lines" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 5       |
      | -5     | 15      |
      | 20     | 0       |
    When I choose "Show Graph" from the game menu
    Then the title is "Graph Lines"
    And I see "Statistics"
    And the "Score by rounds" graph has a line for "Calvin" through 10, 5, 25
    And it has a line for "Lindsay" through 5, 20, 20
    And the rounds along the bottom are numbered from 1
    And the legend names "Calvin" and "Lindsay"

  @visual
  Scenario: Tapping a point shows that player's totals
    Given a game "Graph Tap" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 5       |
      | 10     | 5       |
    When I choose "Show Graph" from the game menu
    And I tap Calvin's point for round 2
    Then the values "10" and "20" show on Calvin's line
    And no values show on Lindsay's line

  @smoke @visual
  Scenario: The podium shows totals best first, in the players' colors
    Given a game "Podium" with players "Calvin, Lindsay, Bob"
    And I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 30      | -20 |
    When I choose "Show Graph" from the game menu
    Then I see "Podium"
    And the podium bars are Lindsay 30, Calvin 10, Bob -20, from left to right
    And each bar is the same color as that player's line

  @visual
  Scenario: With Reverse Scoring, the podium puts the lowest total first
    Given a game "Podium Low" with players "Calvin, Lindsay" and settings:
      | Reverse Scoring | on |
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 30      |
    When I choose "Show Graph" from the game menu
    Then the podium bars are Calvin 10, Lindsay 30, from left to right

  Scenario: Back from the graph returns to the game
    Given a game "Graph Back" with players "Calvin"
    When I choose "Show Graph" from the game menu
    And I go back
    Then the title is "Graph Back"

  @seed @visual
  Scenario: Many players wrap the legend instead of cutting it off
    Given the seed data is loaded
    When I open the game "Trivia Night"
    And I choose "Show Graph" from the game menu
    Then the legend shows all 13 player names
    And the first 10 players have solid lines in different colors, Grandma Jo in the green she picked
    And the 11th to 13th players (Riley, Padfoot, Jordan) repeat the first colors with dashed lines

  @seed @visual
  Scenario: A player's own color replaces their seat's, and others skip it
    Given the seed data is loaded
    When I open the game "Phase 10"
    And I choose "Show Graph" from the game menu
    Then Grandma Jo's line and bar are green (2E7D32), the color she picked
    And the others skip the picked green: Calvin is cyan, Lindsay purple, alice orange, and Bob blue
    And no two players share a color

  @seed
  Scenario Outline: Seeded standings (<game>)
    Given the seed data is loaded
    When I open the game "<game>"
    And I choose "Standings" from the game menu
    Then I see "<rounds played>"
    And the standings are "<standings>"

    Examples:
      | game          | rounds played   | standings                                         |
      | Hearts        | 8 Rounds Played | #1 Zed 51, #1 Priya 51, #3 Calvin 57, #4 Bob 101  |
      | Planted       | 3 Rounds Played | #1 alice 33, #1 Zoë 33, #3 🎲 Dice Goblin 31      |
      | Golf          | 9 Rounds Played | #1 Lindsay 50, #2 Sam 59, #3 Zed 64, #4 Jordan 77 |
      | Mille Bornes  | 4 Rounds Played | #1 Calvin 5125, #2 Taylor 3225, #3 José 3000      |
      | Rummy 500     | 6 Rounds Played | #1 alice 305, #2 Zoë 280, #3 José 115             |
      | Agricola Solo | 1 Round Played  | #1 Calvin 43                                      |
