Feature: Settings
  The gear on the game list opens Settings. Each switch is kept after the app is closed.

  @smoke
  Scenario: Settings lists every section
    Given I am on the game list
    When I tap the Settings icon
    Then the title is "Settings"
    And I see "Games", "Appearance", "Privacy", and "About"
    And I see "Load Previous Settings"
    And I see "Calculator Keyboard"
    And I see "True Black"
    And I see "Share Usage Data"
    And I see "Send Crash Reports"
    And I see "Changelog", "Rate App", and "Version"

  Scenario: The version matches the installed build
    Given I am on the game list
    When I tap the Settings icon
    Then "Version" shows the installed version name and code, like "1.6.7 (123)"

  Scenario: New installs start with these switches
    Given a fresh install
    When I tap the Settings icon
    Then "Load Previous Settings" is on
    And "Calculator Keyboard" is on
    And "True Black" is on
    And "Wallpaper Colors" is on, on Android 12 and newer

  Scenario Outline: Each switch can be turned off and on by its row or the switch, and is kept (<setting>)
    Given I am on the game list
    And I tap the Settings icon
    When I tap the row "<setting>"
    Then "<setting>" has changed
    When I close and reopen the app
    And I tap the Settings icon
    Then "<setting>" is still changed
    When I tap the switch for "<setting>"
    Then "<setting>" is back as it was

    Examples:
      | setting                |
      | Load Previous Settings |
      | Calculator Keyboard    |
      | Share Usage Data       |
      | Send Crash Reports     |

  Scenario: Usage data and crash reports are separate switches
    Given I am on the game list
    And I tap the Settings icon
    And I turn on "Share Usage Data" and "Send Crash Reports"
    When I tap the row "Share Usage Data"
    Then "Share Usage Data" is off
    And "Send Crash Reports" is on

  @visual
  Scenario: True Black switches dark mode between pure black and dark gray
    Given the device is in dark mode
    And I am on the game list
    And I tap the Settings icon
    And "True Black" is on
    Then the background is pure black
    When I tap the row "True Black"
    Then the screen redraws and stays on Settings
    And the background is dark gray
    When I go back
    Then the game list's background is dark gray
    When I tap the Settings icon
    And I tap the row "True Black"
    Then the background is pure black

  @visual
  Scenario: True Black doesn't change light mode
    Given the device is in light mode
    And I am on the game list
    And I tap the Settings icon
    When I tap the row "True Black"
    Then the background stays light

  @visual
  Scenario: Wallpaper Colors follows the wallpaper on Android 12 and newer
    Given the device runs Android 12 or newer
    And I am on the game list
    And I tap the Settings icon
    When I tap the row "Wallpaper Colors"
    Then the screen redraws and stays on Settings
    And the app's colors switch between ones taken from the wallpaper and the app's own

  Scenario: Wallpaper Colors isn't offered before Android 12
    Given the device runs Android 11 or older
    And I am on the game list
    When I tap the Settings icon
    Then I don't see "Wallpaper Colors"

  Scenario: Changelog opens the release notes
    Given I am on the game list
    And I tap the Settings icon
    When I tap "Changelog"
    Then the GitHub releases page for ScoreApp opens in a browser
    And going back returns to Settings

  Scenario: Rate App opens the store
    Given I am on the game list
    And I tap the Settings icon
    When I tap "Rate App"
    Then Google Play's review sheet for Score Pad shows, or Score Pad's Google Play page opens
    And going back returns to Settings

  Scenario: Back from Settings returns to the game list
    Given I am on the game list
    And I tap the Settings icon
    When I go back
    Then the title is "Games"
