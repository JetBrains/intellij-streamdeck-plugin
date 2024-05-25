TContributing to Stream Deck IDE Plugin
=======================================

<!-- TOC -->

* [Contributing to Stream Deck IDE Plugin](#contributing-to-stream-deck-ide-plugin)
* [Contributing](#contributing)
  * [Setting up the project](#setting-up-the-project)
    * [Prerequisites](#prerequisites)
  * [Project Structure](#project-structure)
    * [Setup](#setup)
  * [Resources](#resources)
  * [Tips](#tips)
<!-- TOC -->

# Contributing

Before proceeding please read section ["Setting up the project"](#setting-up-the-project) if you haven't yet.

It's strongly recommended to get familiar with [IntelliJ Platform SDK documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html).
It contains most of the information required to develop IntelliJ-based plugins.

If you want change the Stream Deck JS plugin, it's strongly recommended to read this https://docs.elgato.com/sdk/plugins/overview first.

## Setting up the project

### Prerequisites
In order to take part in Scala plugin development, you need:

1. IntelliJ IDEA 2022.3 or higher
2. JDK 17 (you can [download it via IntelliJ IDEA](https://www.jetbrains.com/help/idea/sdk.html#define-sdk) or using the IDEA's built-in JBR 17)
3. (optional but **recommended**) Enable [internal mode](https://plugins.jetbrains.com/docs/intellij/enabling-internal.html) in IDEA to get access to helpful internal actions and debug information
4. The **streamdeck-plugin** requires node.js installed.

```
cd ./streamdeck-plugin
npm install
npm run build
```

## Project Structure

1. **[idea-plugin](idea-plugin)** IDEA Plugin source code, written with Java and Gradle IJ. To test it, first install the plugin and then open a browser and input: http://localhost:21420/action/NewProject
2. **[streamdeck-plugin](streamdeck-plugin)** Stream Deck Plugin source code, written with TypeScript

### Setup

1. Clone this repository to your computer

```sh
git clone https://github.com/JetBrains/intellij-streamdeck-plugin.git
```

2. Select the `Run Plugin` run configuration and select the `Run` or `Debug` button to build and start a
   development version of IDEA with the Stream Deck IDEA plugin. Select the `Build Plugin` run configuration to package the IDEA plugin.
3. For the Stream Deck plugin, please using

```sh
refreshPlugin.sh
```

to auto package and deploy the plugin bundle to the Stream Deck app. Read [Debugging Your JavaScript Plugin](https://streamdecklabs.com/debugging-your-javascript-plugin/) for how to debug the Stream Deck Js plugin. 
Debug the stream deck plugin: http://localhost:23654/

Logs are saved to disk per plugin in the folder `~/Library/Logs/ElgatoStreamDeck/` on macOS and `%appdata%\Elgato\StreamDeck\logs\` on Windows.
Note that the log files are rotated each time the Stream Deck application is relaunched.

Run the below script to view Stream Deck JS plugin logs under Mac:
```sh
viewLog.sh
```

## Resources

1. Stream Deck [Colors](https://docs.elgato.com/sdk/plugins/style-guide#colors) [Sizes](https://docs.elgato.com/sdk/plugins/style-guide#sizes)

Action Image: `#EFEFEF` - `rgb(239, 239, 239)`
Category Icon: `#DFDFDF` - `rgb(223, 223, 223)`

| Icon          | Size       | @2x        |
| ------------- | ---------- | ---------- |
| Action Image  | 20x20 px   | 40x40 px   |
| Category Icon | 28x28 px   | 56x56 px   |
| Key Icon      | 72x72 px   | 144x144 px |
| Plugin Icon   | 288x288 px | 512x512 px |

2. [Preview Images](https://docs.elgato.com/sdk/plugins/style-guide#preview-images) Requirements

* **Quantity**: 1-3 images named sequentially (1-preview.png, 2-preview.png, etc.)
* **Size**: 1920px by 960px
* **Format**: PNG
* **Content**: Non-transparent

3. Packaging and Distribution

To save repo space, you need download tools from https://docs.elgato.com/sdk/plugins/packaging page.

https://docs.elgato.com/sdk/plugins/distribution

Run the below command to package the JS plugin, it will be saved to [releases/com.jetbrains.ide.streamDeckPlugin](releases/com.jetbrains.ide.streamDeckPlugin):
```shell
cd streamdeck-plugin
npm run build
npm run distribution
```

## Tips

How to quickly add an action button to the Stream Deck JS plugin:

1. Find icons from https://jetbrains.design/intellij/resources/icons_list/ and in IDEA, change the size then choose context menu `Convert to PNG`.
2. Find Action ID in IDEA by using IDE's [Internal Mode | UI Inspector | Action](https://plugins.jetbrains.com/docs/intellij/internal-ui-inspector.html#action)
3. Input this ID to  Stream Deck button's Action settings to see if it works.
4. Edit [manifest.json](com.jetbrains.ide.sdPlugin/manifest.json), copy the Actions definition;
5. Reference [newProject.ts](streamdeck-plugin/src/actions/newProject.ts) to add a new Action TypeScript class.
6. Before release, please check [Resources](#Resources) section.
