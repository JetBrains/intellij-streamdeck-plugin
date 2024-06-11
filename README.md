![Official](https://jb.gg/badges/official-plastic.svg)

An IntelliJ IDE plugin and a paired Stream Deck JS plugin to support developing with pleasure
using  [Stream Deck](https://developer.elgato.com/documentation/stream-deck/), see
also https://youtrack.jetbrains.com/issue/IDEA-280508/Support-for-Elgato-Stream-Deck.

<!-- TOC -->
* [Quick Start](#quick-start)
  * [Prerequisites](#prerequisites)
  * [Install](#install)
  * [Configuration](#configuration)
    * [Quick Search Action ID in IDE since IDE plugin 2023.2.1](#quick-search-action-id-in-ide-since-ide-plugin-202321)
    * [In Stream Deck Store plugin](#in-stream-deck-store-plugin)
    * [in IDE Settings](#in-ide-settings)
* [Features and Useful Actions](#features-and-useful-actions)
* [Remote Control](#remote-control)
* [Contributing](#contributing)
* [Limitations](#limitations-1)
<!-- TOC -->

# Quick Start

## Prerequisites

Before using this plugin, you can either install the free
[STREAM DECK MOBILE(subscription needed)](https://www.elgato.com/us/en/s/stream-deck-mobile) or buy a stream deck
hardware.

Install an IDE 2022.3+ if you don't have one from [Jetbrains](https://www.jetbrains.com)
or [Android Studio](https://developer.android.com/sdk/installing/studio.html). This plugin supports all major Jetbrains
IDEs include IDEA Community, IDEA Ultimate, WebStorm, Rider, Android Studio, PhpStorm, RubyMine, GoLand etc.
Due to the limitation of Stream Deck software, only **Windows** and **macOS** is supported so far.

## Install

1. Open https://marketplace.elgato.com/product/jetbrains-ide-f20a34c4-8965-4e21-8c6a-2454786d748b or search `JetBrains` from the Stream Deck Store to install
   Stream Deck plugin. You may also clone this repository then
   double-click `releases/com.jetbrains.ide.streamDeckPlugin` to install the latest plugin to Stream Deck desktop app
2. Install plugin by search `Stream Deck` in at your IDE's **Settings / Preferences | Plugins** page or manually install
   file `releases/com.jetbrains.ide.streamdeck.plugin-1.0.zip`. Restart your IDE if prompted. Please follow the steps
   here if you didn't
   familiar with the JetBrains IDE: https://www.jetbrains.com/help/idea/managing-plugins.html
3. Add the action from `JetBrains IDE` section to Stream Deck
4. Click the action button at Stream Deck and see it acts in your IDE

## Configuration

This plugin follows a zero-config design, so it works right out of the box. You can also change some
configurations in the IDE's Settings page and Stream Deck's Action Settings page.

### Quick Search Action ID in IDE since IDE plugin 2023.2.1

Quick find action ids var menu **Help | Open Action Browser** : Open Action Browser to view and copy action id infos.
![](screenshot/action_ids_browser.png)

We also have a page of [IDEA Actions Page](IDEA_actions.md).

### Action Icon Lookup

To find the corresponding action icon, use the Action ID to locate it on our [IntelliJ Icon Collection](https://intellij-icons.jetbrains.design/).

### In Stream Deck Store plugin

Click on the action and enter the following parameters:

| Param name                             | Optional | Description                                                        |
|----------------------------------------|----------|--------------------------------------------------------------------|
| Title Visible                          | N        | Toggle all buttons' title visibility                               |
| Password for client connection         | Y        | The password to connect to the IDE(see [section](#in-IDE-Settings) |
| Host                                   | Y        | Connect to a remote running IDE, default to localhost              |
| Remote Port                            | Y        | Connect to a remote running IDE port                               |
| Customize Action                       | N        | The Action ID for the IDE(see [Limitations](#Limitations)          |
| Run/Debug Configuration Name(Optional) | Y        | The Run/Debug Configuration to be used in the IDE                  |

**🆕New Built in Actions since 1.0.3:**

**Run / Debug**: Allow specific run/debug configuration by name.

**Open Action Browser** ： to view and copy action id in the IDE

**Customized Action** : A placeholder action for run customized action id


> ## Limitations
> for IDEA plugin 2023.2.0 and Stream Deck Store Plugin 1.0.0
>
> You can try to invoke any `Action ID` found in the
> IDE's [IDEA Actions Page](IDEA_actions.md),
> note that not every action will work, for example `Compare.SameVersion` will not work.
> Due to a bug but in Stream Deck Store Plugin 1.0.0, you must input host string 127.0.0.1
> to make the customized action to work.
> This plugin doesn't support Fleet.
>

### in IDE Settings

Please open it via the menu **File | Settings | Tools | Stream Deck**.

| Param name                                      | Optional | Description                                                                                                                                                       |
|-------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Password                                        | Y        | The Stream Deck client plugin requires a password to control this IDE, recommended when you enable remote HTTP action server                                      |
| Enable Stream Deck service                      | N        | If disabled, the IDE will not respond when a Stream Deck action button is pressed                                                                                 |
| Perform actions only when IDE window is focused | N        | Once enabled, StreamDeck actions are performed only when the IDE window is focused. For instance, if two IDEs are running, both will execute the button's action. |
| Remote Port                                     | Y        | Default value is 21420                                                                                                                                            |



> **Note**
>
> You can also be noticed there is an `IDE built-in server port` value, however, this port is dynamic and only can be
> used for issue triage.
>
> The Stream Deck plugin only uses port ranges 63342-63352.
>
>

# Features and Useful Actions

For some useful action IDs, such as toggling the visibility of the tool window, 
see this document [Tips](Tips.md).

Once installed the both plugins, you'll be able to using Stream Deck control up to 10 simultaneously local running IDEs
on your computer or one remote running IDE, the plugin
will auto-detect your current IDE window then react to the action.

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

# Remote Control
From version IDE plugin 2023.2.2, supports remote control of the IDE action, 
not enabled by default(Note: doesn't support JetBrains Gateway/Client so far).
please update to the latest [Stream Deck plugin](releases/com.jetbrains.ide.streamDeckPlugin).

Input host and port(21420 by default in the Stream deck button config).
![](screenshot/StreamDeckRemoteControl.png)
# Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md)

# Limitations

This plugin doesn't support JetBrains Gateway/Client so far.
