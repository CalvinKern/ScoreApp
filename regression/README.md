# Regression test plan

Happy path scenarios for every part of Score Pad, written in [Gherkin](https://cucumber.io/docs/gherkin/reference/) so
the same plan can be run by hand, driven over adb by a script or an agent, or wired into an automated runner later
(Cucumber, Maestro, or Espresso step definitions can match the steps below).

The scenarios describe what the app does today. When a change is meant to alter one of these behaviors, update the
scenario in the same change.

## Running it

1. Pick a scope: a tag (`@smoke` before a release), a feature file, or both. `@slow` scenarios can be skipped for a
   quick pass, and `@locale`, `@landscape`, dark mode, and font size scenarios change the device, so run them on
   purpose. The plan runs on the debug build. Before a release, also try the release build R8 makes with
   `scripts/release-check.sh`.
2. Get the device ready:

   ```bash
   scripts/emulator-lock.sh acquire Pixel_9a           # Keep other checkouts off it while you test
   ./gradlew installDebug                              # The build under test
   scripts/emulator-db.sh backup before-regression     # Keep the device's own test data
   ```

   With more than one emulator or phone connected, `export ANDROID_SERIAL=<the one under test>` first. Without it,
   `installDebug` installs on every connected device, and the adb commands fail.

   The lock stops testing from two checkouts (worktrees) at once from installing over each other's build or clearing
   each other's data. The emulator scripts take it themselves and stop when another checkout holds it, and so do
   Gradle's install tasks. If it's held, use another emulator (`scripts/emulator-lock.sh status` shows who has what).

   Before each `@seed` scenario that changes data, `scripts/emulator-db.sh seed` (it's quick, and a clean seed means
   one scenario can't break the next). Before each `@fresh` scenario, `adb shell pm clear com.seakernel.scorepad.debug`.
3. Run each scenario's steps as defined below, and record the result (see [Recording a run](#recording-a-run)).
4. Put the device back: `scripts/emulator-db.sh restore <the before-regression backup>`, and undo anything else that
   changed (rotation, language, font size, dark mode, right to left). `pm clear` also clears the app's settings, so
   note them first if they matter. Then `scripts/emulator-lock.sh release Pixel_9a`.

## Files

| Feature                                                           | Covers                                                                                                     |
|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| [01_first_launch](features/01_first_launch.feature)               | The empty game list, the usage data question in it                                                         |
| [02_game_list](features/02_game_list.feature)                     | Order, row details, opening, deleting, games that need players                                             |
| [03_players](features/03_players.feature)                         | Picking, adding, renaming, deleting and restoring, searching players                                       |
| [04_game_setup](features/04_game_setup.feature)                   | Creating a game, its settings, the dealer, player order, loading previous settings                         |
| [05_scoring](features/05_scoring.feature)                         | Entering scores with the calculator and the system keyboard, totals, the leader                            |
| [06_rounds_and_dealer](features/06_rounds_and_dealer.feature)     | Adding and deleting rounds, moving between scores, the dealer                                              |
| [07_goals](features/07_goals.feature)                             | Target score and rounds, Finish Game, Another Round, confetti                                              |
| [08_standings_and_graph](features/08_standings_and_graph.feature) | Player Standings, the rounds graph and podium                                                              |
| [09_round_notes](features/09_round_notes.feature)                 | Notes for each player's rounds                                                                             |
| [10_edit_game](features/10_edit_game.feature)                     | Changing a game's settings and players while it's being played                                             |
| [11_new_game_from_game](features/11_new_game_from_game.feature)   | Starting the next game with the same players and settings                                                  |
| [12_settings](features/12_settings.feature)                       | Every switch on the Settings screen                                                                        |
| [13_seeded_games](features/13_seeded_games.feature)               | All 30 games from `scripts/test-data/seed.sql`, with their expected totals                                 |
| [14_full_games](features/14_full_games.feature)                   | Real games played from the first round to the standings                                                    |
| [15_device](features/15_device.feature)                           | Rotation, landscape, leaving the app, Android closing it in the background, back gestures, other languages |

## Tags

| Tag                     | Meaning                                                                                                   |
|-------------------------|-----------------------------------------------------------------------------------------------------------|
| `@smoke`                | A quick pass over the main flows, about 40 scenarios. Run it before every release.                        |
| `@seed`                 | Starts from the seed data. Load it with `scripts/emulator-db.sh seed`.                                    |
| `@fresh`                | Starts from a fresh install. Clear the app's data with `adb shell pm clear com.seakernel.scorepad.debug`. |
| `@calculator`           | Uses the calculator keyboard                                                                              |
| `@system-keyboard`      | Uses the system keyboard (the game's Calculator Keyboard setting is off)                                  |
| `@visual`               | Needs a look at the screen (colors, borders, charts, confetti), not just the text on it                   |
| `@landscape`, `@locale` | Changes the device's orientation or language. Put it back afterwards.                                     |
| `@slow`                 | Plays a long game, skip it for a quick pass                                                               |

Scenarios without `@seed` or `@fresh` make their own data from whatever is already there, so they can run after
either.

## Device setup

- The debug build (`./gradlew installDebug`, package `com.seakernel.scorepad.debug`) on an emulator or phone
- An emulator that's been cold booted recently (`scripts/emulator-cold-boot.sh [avd]`, or Device Manager › Cold Boot
  Now). Its graphics slow down over days of heavy use until screens take seconds to open, and
  `adb reboot` doesn't fix that since it only restarts Android inside the same emulator. Cold boot before a long run,
  and again if it gets slow partway through.
- English (United States), so dates read like `Oct 5, 2026` and decimals use a `.`. Only `@locale` scenarios change it.
- Portrait, unless a scenario rotates it
- The usage data question answered (either way) unless a scenario says otherwise, so its card isn't in the game list

## Steps

Steps use the words on the screen, quoted exactly (`"Add Round"`, `"Goal Reached!"`), and a step always means the
same thing. These are the ones most scenarios share; any other step means just what it says. Player names in steps
are the names shown in the game, unquoted when a step names a player (`Lindsay is leading`).

### Setting up

| Step                                                          | How                                                                                                                                                                                                              |
|---------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Given a fresh install`                                       | Clear the app's data and open it. The game list is empty and nothing is asked.                                                                                                                                   |
| `Given a fresh install with the usage data question answered` | Clear the app's data, open it, tap the Settings icon, tap the row "Send Crash Reports" twice (on, then off), and go back. Changing a switch answers the question, so its card never shows in the game list.      |
| `Given the seed data is loaded`                               | `scripts/emulator-db.sh seed`, then open the app on the game list                                                                                                                                                |
| `Given I am on the game list`                                 | Open the app, or go back until the "Games" screen shows                                                                                                                                                          |
| `Given the players "A, B" exist`                              | Add any of them that aren't already in the player list (from any game's player picker)                                                                                                                           |
| `Given a game "<name>" with players "A, B"`                   | Create it: + on the game list, type the name, "Edit", pick the players in that order (adding any that don't exist), save the players, set the settings in the table if one follows, then "Save". The game opens. |
| `Given I am picking players for a new game`                   | On the game list, tap +, then "Edit"                                                                                                                                                                             |

The settings table under a game uses the setting names from the New Game screen. Settings left out keep their
defaults:

| Setting               | Values                                                                                    | Default                                     |
|-----------------------|-------------------------------------------------------------------------------------------|---------------------------------------------|
| `Dealer`              | on, off                                                                                   | on                                          |
| `First dealer`        | a player's name                                                                           | the first player                            |
| `Reverse Scoring`     | on, off                                                                                   | off                                         |
| `Show Round Notes`    | on, off                                                                                   | off                                         |
| `Calculator Keyboard` | on, off                                                                                   | on (follows Settings › Calculator Keyboard) |
| `Target Score`        | a number (decimals and negatives allowed, a negative one is reached going down), or blank | blank                                       |
| `Target Rounds`       | a number, or blank                                                                        | blank                                       |

### Doing things

| Step                                                                | How                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `When I tap "<text>"`                                               | Tap the button, menu item, or row with that text                                                                                                                                                                                                                                                                                                                                                                                      |
| `When I choose "<item>" from the game menu`                         | "Standings" is the trophy icon. "Show Graph", "Edit", "Delete Rounds", and "New Game" are in the ⋮ menu.                                                                                                                                                                                                                                                                                                                              |
| `When I open the game "<name>"`                                     | Tap its row on the game list (the top one when two have the same name, unless a row number is given)                                                                                                                                                                                                                                                                                                                                  |
| `When I enter "<input>" for <player> in round <n>`                  | Tap the score, move the cursor to the end (a tap puts it where the finger landed) and clear it with DEL, type the input, then press `=` on the calculator, or with the system keyboard tap the toolbar beside the title (other places outside the score do things: a player's name opens their notes, and the space under the table can be "Add Round"). Write `×` for multiply; the system keyboard only types digits, `-`, and `.`. |
| `When I play these rounds:`                                         | A table with a column per player and a row per round. Each row is entered into the current round, then "Add Round" is tapped before the next row (not after the last). An empty cell is skipped and stays 0.                                                                                                                                                                                                                          |
| `When I tap the round <n> score for <player>`                       | Tap that player's score in that round (round 1 is the top row)                                                                                                                                                                                                                                                                                                                                                                        |
| `When I type "<input>" on the calculator`                           | Tap the calculator keys for each character, without pressing `=`                                                                                                                                                                                                                                                                                                                                                                      |
| `When I type these on the calculator, tapping "->" after each: ...` | Type each value, then tap `->`, which moves to the next score, and adds a round after the last one                                                                                                                                                                                                                                                                                                                                    |
| `When I long press <target>`                                        | Hold it for about a second (`adb shell input swipe x y x y 800`)                                                                                                                                                                                                                                                                                                                                                                      |
| `When I tap the rows "A" and "B" quickly`                           | Tap both within about a second (`adb shell "input tap X1 Y1; input tap X2 Y2"`), before the first pick saves                                                                                                                                                                                                                                                                                                                          |
| `When I go back`                                                    | The system back gesture or button (`adb shell input keyevent KEYCODE_BACK`)                                                                                                                                                                                                                                                                                                                                                           |
| `When I close and reopen the app`                                   | Swipe it away from recents (or `adb shell am force-stop`), then open it again                                                                                                                                                                                                                                                                                                                                                         |
| `When I force stop the app`                                         | `adb shell am force-stop com.seakernel.scorepad.debug`, without closing any screen first                                                                                                                                                                                                                                                                                                                                              |
| `When Android closes the app in the background`                     | Press Home, kill the app while it's in the background, and open it again from its launcher icon (see [Driving the app with adb](#driving-the-app-with-adb)). Android does this on its own to free memory, and the app comes back on the same screen, rebuilt from what it saved. Unlike `I close and reopen the app`, which throws the saved screens away and starts on the game list.                                                |

### Checking things

| Step                                                                              | How                                                                                                                                                                                                                            |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Then I see "<text>"` / `Then I don't see "<text>"`                               | The text is (or isn't) on the screen                                                                                                                                                                                           |
| `Then the title is "<text>"`                                                      | The toolbar title                                                                                                                                                                                                              |
| `Then the subtitle is "<text>"` / `Then there's no subtitle`                      | The smaller line under the toolbar title                                                                                                                                                                                       |
| `Then the totals are:`                                                            | The totals row at the bottom of the game, one column per player, as shown                                                                                                                                                      |
| `Then the totals read "A 1, B 2"`                                                 | The same, written on one line in seat order                                                                                                                                                                                    |
| `Then the round <n> scores are:`                                                  | The scores in that round's row, as shown when not being edited                                                                                                                                                                 |
| `Then <player> is leading` / `Then A and B are leading`                           | Their totals have the leading (winner) border, and no one else's does                                                                                                                                                          |
| `Then nobody is leading`                                                          | No total has the leading border                                                                                                                                                                                                |
| `Then the leading players are "A, B"`                                             | The same as the leading steps above, or "nobody"                                                                                                                                                                               |
| `Then the round <n> score for <player> shows "<text>"`                            | That score as shown when not being edited. "in red" means the error color, used for calculations that couldn't be finished.                                                                                                    |
| `Then the score being edited shows "<text>"`                                      | The text in the focused score, which is what was typed for it (like `5×4+10+25`)                                                                                                                                               |
| `Then the game has <n> rounds`                                                    | The number of rows in the score table                                                                                                                                                                                          |
| `Then the button after the rounds is "<text>"`                                    | "Add Round" or "Finish Game"                                                                                                                                                                                                   |
| `Then <player> is the dealer`                                                     | The current (last) round's score for that player has the dealer border                                                                                                                                                         |
| `Then <player> dealt round <n>`                                                   | That round's score for that player has the dealer border (a lighter one for past rounds)                                                                                                                                       |
| `Then the current dealer is "<player>"`                                           | The same as `<player> is the dealer`, or "nobody" when no score has the dealer border                                                                                                                                          |
| `Then the standings are:`                                                         | The Player Standings dialog lists these places, players, and totals in order. Tied players share a place and can be in either order.                                                                                           |
| `Then the calculator keyboard shows` / `doesn't show`                             | The calculator's keys (`calculatorKeyboard`) are on the screen, or aren't                                                                                                                                                      |
| `Then the system number keyboard shows` / `Then the system keyboard doesn't show` | `adb shell dumpsys input_method` has `mInputShown=true` (or `false`). Check that instead of looking, since an emulator with a hardware keyboard attached never draws the system keyboard, even while it's showing for a score. |

### Finding things on screen

For tools that read the view tree (`adb shell uiautomator dump`, Espresso, Maestro), these view ids find what the
steps talk about. Each is under `com.seakernel.scorepad.debug:id/`.

| Screen                  | Ids                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Game list               | `gameRecycler`, each row's `gameNameHolder`, `gameDateHolder`, `gamePlayersHolder`, `gameRoundsHolder`; `fab` (+); `gameListEmptyGroup`                                                                                                                                                                                                                                                                                                                                                                                   |
| New Game, Game Settings | `gameNameEdit`, `playersHeaderEdit` ("Edit"), `playerRecycler` with `playerNameHolder` and `playerDealerBox`; `hasDealerCheckbox`, `reversedScoringCheckbox`, `showNotesCheckbox`, `useCalculatorCheckbox`, `maxScoreEdit`, `maxRoundsEdit`                                                                                                                                                                                                                                                                               |
| Pick Players            | `playerRecycler` with `playerNameHolder`, `playerCheckHolder`, `playerNameSettings` (edit icon); `fab` (+); `actionSearch`; the player dialog's `playerNameEdit`, `playerColorEdit` (hex, without the #), and `playerColorSwatches` (a swatch per graph color, named by color, like "Green")                                                                                                                                                                                                                              |
| Game                    | `nameRow` with `playerNameHeader`; `scoreRows` with a `playerScore` per score (left to right, then top to bottom); `addRoundButton`, `finishGameButton`; `totalsRow` with a `playerScore` per total; `calculatorKeyboard` with `calculatorKeyOne` … `calculatorKeyZero`, `calculatorKeyPlus`, `calculatorKeyMinus`, `calculatorKeyMultiply`, `calculatorKeyDivide`, `calculatorKeyDecimal`, `calculatorKeyOpenParen`, `calculatorKeyCloseParen`, `calculatorKeyDelete`, `calculatorKeyEquals`, `calculatorKeyNext` ("->") |
| Player Standings        | `dialogPlayerRoundSubheader`, `dialogPlayerRoundRecycler` with `playerStandingHolder`, `playerNameHolder`, `playerScoreHolder`                                                                                                                                                                                                                                                                                                                                                                                            |
| Notes                   | `dialogPlayerNotesRecycler` with `playerRoundLabel` and `playerRoundValue`                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Graph                   | `graphToolbar`, `gameChart` (rounds), `podiumChart`                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Settings                | `autoLoadSettingsSwitch`, `calculatorDefaultSwitch`, `dynamicColorSwitch`, `trueBlackSwitch`, `shareUsageDataSwitch`, `shareCrashReportsSwitch`, `versionText`                                                                                                                                                                                                                                                                                                                                                            |

Borders, colors, confetti, and the charts' lines and bars aren't in the view tree, so `@visual` checks need a
screenshot. The charts describe their data for screen readers, so their content description lists each player's
running totals and the podium order.

### Driving the app with adb

For scripts and agents. In Git Bash, `export MSYS_NO_PATHCONV=1` first so device paths aren't rewritten. With more
than one emulator or phone connected, also `export ANDROID_SERIAL=<one from adb devices>`, or every adb command fails.

| To                                                   | Run                                                                                                                                                                                                                                        |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Open the app                                         | `adb shell am start -n com.seakernel.scorepad.debug/com.seakernel.android.scoreapp.ui.MainActivity`                                                                                                                                        |
| Open the app from its launcher icon                  | `adb shell monkey -p com.seakernel.scorepad.debug -c android.intent.category.LAUNCHER 1`. Use this to come back after Android closes the app: `am start -n` opens the screens in a separate task instead of the ones Android saved.        |
| Read the screen                                      | `adb shell uiautomator dump /sdcard/ui.xml >/dev/null && adb exec-out cat /sdcard/ui.xml`                                                                                                                                                  |
| Tap                                                  | `adb shell input tap X Y`, the middle of a node's `bounds="[x1,y1][x2,y2]"`                                                                                                                                                                |
| Long press                                           | `adb shell input swipe X Y X Y 800`                                                                                                                                                                                                        |
| Drag a player                                        | `adb shell input draganddrop X1 Y1 X2 Y2 1500`                                                                                                                                                                                             |
| Scroll                                               | `adb shell input swipe X1 Y1 X2 Y2 300`                                                                                                                                                                                                    |
| Type on the system keyboard                          | `adb shell input text "Quinn"` (`%s` for a space)                                                                                                                                                                                          |
| Back gesture                                         | `adb shell input swipe 2 1300 600 1300 250` (a swipe in from the left edge)                                                                                                                                                                |
| Back button, Home, Recents                           | `adb shell input keyevent KEYCODE_BACK` / `KEYCODE_HOME` / `KEYCODE_APP_SWITCH`                                                                                                                                                            |
| Screenshot                                           | `adb exec-out screencap -p > name.png`                                                                                                                                                                                                     |
| Force stop, kill in background                       | `adb shell am force-stop <package>`, `adb shell am kill <package>`                                                                                                                                                                         |
| Close the app the way Android does in the background | `KEYCODE_HOME`, wait about two seconds, then `adb shell am kill <package>`. It only kills an app that's in the background, so check `adb shell pidof <package>` prints nothing, or end it with `adb shell run-as <package> kill -9 <pid>`. |
| Rotate                                               | `adb shell wm user-rotation lock 1` (landscape) or `lock 0` (portrait), and `wm user-rotation free` to turn auto-rotate back on. Check `adb shell wm user-rotation` first to know what to put back.                                        |
| Dark mode                                            | `adb shell cmd uimode night yes` / `no`                                                                                                                                                                                                    |
| Font size                                            | `adb shell settings put system font_scale 1.3` (`1.0` to reset)                                                                                                                                                                            |
| Right to left                                        | `adb shell settings put global debug.force_rtl 1` (`0` to reset), then restart the app                                                                                                                                                     |
| Language                                             | Settings › System › Languages on the device. Put English (United States) back after.                                                                                                                                                       |

Calculator keys are tapped by id (`calculatorKeyFive`, `calculatorKeyMultiply`, ...), not typed. The system keyboard
only types digits, `-`, and `.` into a score. One dump gives the bounds of everything on screen, so several taps can
follow it until the screen changes (a new screen, a dialog, scrolling, a new round).

Things that trip up a run:

- Screens slide in, so wait about a second after navigating before reading the screen or tapping, and about two
  seconds before typing into a field that just opened (typing sooner can scramble the text).
- Tapping a score puts the cursor where the tap landed, and DEL only clears the part before the cursor. With the
  system keyboard, send `adb shell input keyevent KEYCODE_MOVE_END` before deleting. With the calculator,
  `KEYCODE_MOVE_END` and `KEYCODE_DEL` from adb don't reach the score, so tap near the right edge of the score (past
  the text, so the cursor lands at the end) and clear it with the calculator's DEL key.
- The keyboard covers the bottom of the screen (including "Edit" on New Game). Hide it with `KEYCODE_BACK`, but only
  once it's showing (`adb shell dumpsys input_method | grep mInputShown=true`), or back leaves the screen instead.
- The game name suggestions are a popup window that `uiautomator dump` doesn't include. Tap them from a screenshot.
  While it's open it covers the settings below the name (and "Edit" once the keyboard is hidden), and the first
  `KEYCODE_BACK` only closes the popup, so check the keyboard again before tapping on. The keyboard's done key closes
  it too. Names that start like an earlier game's (like "Calc 0" after "Calc 0.3") open it too.
- Scrolling with a swipe while the keyboard is up can land on the keyboard and glide-type into the focused field.
  Hide the keyboard first.
- `input swipe` waits for the app to take each event, and when the app is slow to take the press (a slow emulator
  still drawing the last tap) it skips every move and lifts at the end. The app sees a tap that lifts far away, which
  most rows count as a tap on where the swipe started. Pick Players' rows ignore it, other screens don't, so wait a
  couple of seconds after a tap before scrolling. A long press can turn into a tap the same way, so check that its
  dialog opened.
- The "Deleted <name>" snackbar on Pick Players goes away after about two seconds, so tap "Undo" from the same dump
  that found it. On a slow emulator a dump can take longer than the snackbar lasts. Then dump and tap on the device
  instead: `adb shell 'for i in 1 2 3 4 5 6 7 8; do uiautomator dump /sdcard/u.xml >/dev/null; if grep -q "Deleted
  Taylor" /sdcard/u.xml; then input tap X Y; break; fi; done'`, with X Y from a screenshot of the snackbar.
- `uiautomator dump` sometimes fails with "null root node" (often while a screen or dialog is still drawing). Wait a
  second and dump again.
- `input text` sometimes drops characters on a slow emulator. Read the field back from a dump, and clear it
  (`KEYCODE_MOVE_END`, then `KEYCODE_DEL`s) and type again when it doesn't match.
- `input text` can't type characters outside ASCII (like "✓" or "李雷"), so type an ASCII stand-in and note it in the
  results. Text starting with `-` has to be quoted inside the shell command, `adb shell input text "'-50'"`, or
  `input` reads it as an option.
- Toasts (like "Please enter a name for the game") aren't in the dump. Take a screenshot right after the tap that
  shows one, since it's gone in about two seconds.
- The rotation lock doesn't survive `uiautomator dump`: afterward `wm user-rotation` reads `free` again and the screen
  goes back to portrait. Lock it again before each landscape check, and read landscape screens from screenshots.
- Rows partly under the gesture bar at the bottom don't reliably take taps or long presses. Scroll them up first.
- In the player dialog, the swatch that's picked has `selected="true"` (not `checked`), and an empty color field
  reads as its label and hint (`Color, 1976D2`).
- Only the rows on screen are in the dump. With the keyboard up, the newest round is the lowest row of `playerScore`
  cells inside `scoreRows`.
- Confetti lasts about a second. Catch it with a burst of on-device screenshots started just before the step:
  `adb shell 'for i in 1 2 3 4 5 6 7 8; do screencap -p /sdcard/c$i.png; done' &`. Totals update while a score is
  typed, so it bursts as soon as a total reaches the goal, on the key that gets it there (like the last digit of
  `50+20+6`), not when `=` is pressed. Start the burst before that key. When `=` closes the keypad within about half
  a second of that key, the burst waits for the keypad to hide and comes from where the total ends up. Clearing a score with DEL counts too: going
  from `-60` back to `-6` can bring a total back over the target, so start the burst before clearing. Reaching the
  target bursts only once a round, so a missed burst can't be caught again by editing the same round. Play it again in
  a later round or a new game.

## Recording a run

Write each run to `regression-results/<date>-<scope>.md` (ignored by git) with the build version (Settings › Version),
the device, and a line per scenario:

- **Pass** when every `Then` matches.
- **Fail** when the app does something else. Stop that scenario at the first failing step, and note the step, what
  was expected, what happened, and a screenshot. Then move on to the next scenario.
- **Blocked** when a step couldn't be done (the device can't do it, the screen couldn't be reached). Say why.
- **Plan issue** when the scenario, not the app, looks wrong. Explain it, and fix the scenario after the run.

A step that isn't defined above means what it says. Note its wording in the results so it can be added here.

## Keeping it current

- A new feature or setting gets a scenario here in the same change.
- When `seed.sql` changes, recompute the expected values in `13_seeded_games.feature` (the seed's README explains the
  games).
- New strings or renamed buttons need the quoted text here updated too.
