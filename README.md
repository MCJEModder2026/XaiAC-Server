# Xai-AC

## Setup

Add the paper plugin or fabric mod to the server. Start it, then adjust config\\xaiacserver.json to your likings. Some mods come with bundled mods (e.g. cloth-basic-math) that get flagged if not added. Use the config-generator mod to easily create a config (run it with your modpack client-side).

## Commands

All commands require op

### Config

Use **/xaiac config load** to reload the config from file without restarting the server.

Use **/xaiac config punishment_mode (Checkname/ALL) get** to set the current punishment is set to (kick, ban or log)

Use **/xaiac config punishment_mode (Checkname/ALL) (ban/log/kick)** to set the new punishment mode for that/all checks.

### Bypass

Use **/xaiac bypass add/remove (username or UUID)** to add/remove a player from the bypass list (they dont need to use the client-side mod)

Use **/xaiac bypass list** to see all currently bypassed players.

### Enforce

Can be used in combination with log punishment mode for ban waves (also triggers for kick/ban tho).

Use **/xaiac enforce (check-name, gets auto-suggested if populated) view** to see every player that got flagged for that specific check.

Use **/xaiac enforce (check-name, gets auto-suggested if populated) (ban/banip)** to automatically ban EVERY player that has ever triggered this flag.

Use **/xaiac enfore player (Name or UUID) view** to see all violations of a specific player with timestamps.

## Disclaimer

Client-Side Anticheats are NOT perfect. XaiAC uses encryption to make it far more secure than similar mods, but i still recommend it to be used with a server-side anticheat.

