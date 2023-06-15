# Toa Extended

A plugin for Tombs of Amascut with extended features.

Designed primarily for solo runs.

### How to build the plugin

Open the project in Intellij and run the gradle `shadowJar` task to compile. 

The plugin jar will be produced in the `./build/libs` directory.

*This repository will never provide precompiled binaries, for safety purposes.*

### How to use the plugin

The plugin is made for the official RuneLite client and is therefore compatible with its API.

Follow these steps to load RuneLite with the plugin conventionally:

1. Build RuneLite from the official github repository.
2. Place the plugin jar in `./runelite/sideloaded-plugins` directory.
3. Run the local build of RuneLite with the `--developer-mode` flag.
4. Enable and configure the plugin in the sidebar.

e.g. `$ java -ea -Drunelite.pluginhub.version=1.10.5 -jar client-1.10.6-SNAPSHOT-shaded.jar --developer-mode`

### Features

Extended features include overlays for:

* prayers (guitar hero)
* graphics animations (lightning, poison, fireballs, boulders)
* counters (ticks, attacks, phase hp)
* dynamic entity hiding (npcs, projectiles, gfx, ground objs)
* boss mechanics (memory blast, orb phase, warden slam direction)

and more...

*Note: The plugin does NOT contain any features that use automation.*

### Configuration

![1](/docs/1.png)

![2](/docs/2.png)

![3](/docs/3.png)

![4](/docs/4.png)

![5](/docs/5.png)

![6](/docs/6.png)

![7](/docs/7.png)

![8](/docs/8.png)

![9](/docs/9.png)

![10](/docs/10.png)

![11](/docs/11.png)

![12](/docs/12.png)

![13](/docs/13.png)

### Known Bugs/Missing Features

* Kephri's attack counter needs fixed (attack rotation behavior is unknown).
* Kephri's dung outline will not show if player's spot animation (fly swarm) is interrupted.
* Scabaras' entrance highlighting is buggy in groups.
* Baba's shockwave overlay for low path levels (groups?) is unsupported.
* Point tracker assumes solo runs only.