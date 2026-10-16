# Ping Trusted (Fabric, Minecraft 1.21.11, client-side)

Look at a block and press **V** (rebindable in Controls > "Ping Trusted").
Every online player on your trusted list gets a private message:

    {user} has pinged {block} at {x, y, z} in {dimension} to everyone trusted

- Same dimension: teammates with the mod also get a gold on-screen marker (with name, block and distance,
  and an arrow at the screen edge when it's off-screen) plus a sound. It disappears after 30 seconds.
- Different dimension: no marker or sound, just the chat message (which names the dimension).
- No distance limit; the ray keeps going until it hits a block. (The client only knows blocks in loaded chunks,
  so in practice that is your render distance.)

Works on any server (it uses `/msg`), nothing server-side needed.

## Commands
- `/pingtrusted add <player>`
- `/pingtrusted remove <player>`
- `/pingtrusted list`

The list is saved in `config/pingtrusted.json`. Each teammate adds the others to their own list.

## Build
Needs JDK 21. Run `./gradlew build` (Windows: `gradlew.bat build`). The jar is in `build/libs/`
(use the one without `-sources`). Put it in `mods/` with Fabric API for 1.21.11.

## No JDK? Build it on GitHub
Push this folder to a new GitHub repo. The included workflow (`.github/workflows/build.yml`) builds automatically:
open the repo's **Actions** tab, click the latest run, and download the `pingtrusted-jar` artifact.
