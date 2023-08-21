![Official](https://jb.gg/badges/official-plastic.svg)

# Stream Deck Plugin for IntelliJ IDE

An IntelliJ IDE plugin and a paired Stream Deck JS plugin to support developing with pleasure
using  [Stream Deck](https://developer.elgato.com/documentation/stream-deck/), see
also https://youtrack.jetbrains.com/issue/IDEA-280508/Support-for-Elgato-Stream-Deck.

<!-- TOC -->
* [Stream Deck Plugin for IntelliJ IDE](#stream-deck-plugin-for-intellij-ide)
* [Features](#features)
* [Quick Start](#quick-start)
  * [Prerequisites](#prerequisites)
  * [Install](#install)
  * [Configuration](#configuration)
    * [in IDE Settings](#in-ide-settings)
    * [in Stream Deck](#in-stream-deck)
* [Contributing](#contributing)
<!-- TOC -->

# Features

Once installed the both plugins, you'll able to using Stream Deck control up to 10 simultaneously running IDEs on your computer, the plugin
will auto detect your current IDE window then react to the action.

Below is a list of tested actions and their supported IDEs:

|                   | IDEA, Android Studio | WebStorm, Rider, PyCharm etc |
|-------------------|----------------------|------------------------------|
| Update Project    | ✔                    | ✔                            |
| New Project       | ✔                    |                              |
| Project Structure | ✔                    |                              |
| Run               | ✔                    | ✔                            |
| Debug             | ✔                    | ✔                            |
| Step Over         | ✔                    | ✔                            |
| Step Out          | ✔                    | ✔                            |
| Resume            | ✔                    | ✔                            |
| Pause             | ✔                    | ✔                            |
| Stop              | ✔                    | ✔                            |
| Search Everywhere | ✔                    | ✔                            |

> ## Limitations
>
> You can try invoke any `Action ID` found in the
> IDE's [Internal Mode | UI Inspector | Action](https://plugins.jetbrains.com/docs/intellij/internal-ui-inspector.html#action),
> note that not every action will work, for example `Compare.SameVersion` will not work.
> This plugin doesn't support Fleet.

# Quick Start

## Prerequisites

Install an IDE 2022.3+ if you don't have one from [Jetbrains](https://www.jetbrains.com)
or [Android Studio](https://developer.android.com/sdk/installing/studio.html). This plugin supports all major Jetbrains
IDEs include IDEA Community, IDEA Ultimate, WebStorm, Rider, Android Studio, PhpStorm, RubyMine, GoLand etc.
Due to the limitation of Stream Deck software, only **Windows** and **macOS** is supported so far.

## Install

1. Open https://apps.elgato.com/plugins/com.jetbrains.ide or search `JetBrains` from the Stream Deck Store to install Stream Deck plugin. You may also clone this repository then double-click `releases/com.jetbrains.ide.streamDeckPlugin` to install the latest plugin to Stream Deck desktop app
2. Install plugin by search `Stream Deck` in at your IDE's **Settings / Preferences | Plugins** page or manually install file `releases/com.jetbrains.ide.streamdeck.plugin-1.0.zip`. Restart your IDE if prompted. Please follow the steps here if you didn't
   familiar with the JetBrains IDE: https://www.jetbrains.com/help/idea/managing-plugins.html
3. Add the action from `JetBrains IDE` section to Stream Deck
4. Click the action button at Stream Deck and see it acts in your IDE

## Configuration

This plugin follows a zero-config design, so it works right out of the box. You can also change some
configurations in the IDE's Settings page and Stream Deck's Action Settings page.

### in IDE Settings

Please open it via the menu **File | Settings | Tools | Stream Deck**.

| Param name                                      | Optional | Description                                                                                                                                                        |
|-------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Client Connection Password                      | Y        | The Stream Deck client requires a password for connection                                                                                                          |
| Stream Deck Service Activation                  | N        | If disabled, the IDE will not respond when a Stream Deck action button is pressed                                                                                  |
| Perform actions only when IDE window is focused | N        | If disabled, the IDE will execute the action even if the IDE window is not in focus. For instance, if two IDEs are running, both will execute the selected action. |

> **Note**
>
> You can also be noticed there is an `IDE built-in server port` value, however, this port is dynamic and only can be used for issue triage.
>
> The Stream Deck plugin only uses port ranges 63342-63352.
>
>

### in Stream Deck
Click on the action and enter the following parameters:

| Param name                     | Optional | Description                                                        |
|--------------------------------|----------|--------------------------------------------------------------------|
| Title Visible                  | N        | Toggle all buttons' title visibility                               |
| Password for client connection | Y        | The password to connect to the IDE(see [section](#in-IDE-Settings) |
| Host                           | Y        | Connect to a remote running IDE                                    |
| Customize Action               | N        | The Action ID for the IDE(see [Limitations](#Limitations)          |

# Contributing
Please see [CONTRIBUTING.md](CONTRIBUTING.md)
