# FTB Teams War Addon

War system addon for **FTB Teams** and **FTB Chunks** (Minecraft Forge 1.20.1).

## Features

### Commands

- **`/war <team>`** - Declare war on another team
  - ⚠️ Requires 39% of target team online
  - Creates war request that must be accepted

- **`/waraccept`** - Accept incoming war request
  - Starts actual war after acceptance

- **`/peace <team>`** - Make peace with enemy team
  - Creates 5-day non-aggression pact
  - Prevents new wars during pact period

- **`/infowar`** - Show all enemies you're currently at war with

- **`/nonaggression pact <team>`** - Create 5-day non-aggression pact
  - Prevents wars with specified team for 5 days
  - Automatically ends any active war with them

### Game Mechanics

#### PvP System
- Players can only attack each other if their teams are at war
- All other PvP attempts are blocked

#### Block Breaking
- Can only break blocks in enemy chunks if your team is at war with them
- Cannot break blocks in neutral or your own chunks

#### Chunk Capture System
- When a player enters an enemy chunk during war:
  - Capture progress starts (only when defenders not present)
  - Takes **5 minutes** to capture
  - Progress resets if defenders return
  - Progress resets if attacker leaves
  
- **Requirements for chunk capture:**
  - ✅ Your team must be at war with owner
  - ✅ Owner team must have 39%+ players online
  - ✅ No defenders in chunk for 5 minutes
  
- When capture completes:
  - Chunk ownership transfers to your team
  - FTB Chunks claim is updated

## Installation

1. Download `warmodadd-1.0.0.jar`
2. Place in your `mods/` folder
3. Ensure you have:
   - FTB Teams installed
   - FTB Chunks installed
4. Launch Minecraft

## Dependencies

- Minecraft 1.20.1
- Forge 47.2.0+
- FTB Teams 2.0+
- FTB Chunks 2.0+

## How Wars Work

### Starting a War

1. Team leader executes: `/war enemy_team`
2. Requires 39% of enemy team online
3. Creates war request
4. Enemy team receives notification
5. Enemy leader executes: `/waraccept`
6. **War begins!** Both teams can:
   - Attack each other (PvP enabled)
   - Break blocks in each other's claims
   - Capture enemy chunks

### Ending a War

Option 1: **Peace Treaty**
- Leader executes: `/peace enemy_team`
- Creates 5-day non-aggression pact
- Both teams forced to peace

Option 2: **Non-Aggression Pact** (Diplomatic)
- Leader executes: `/nonaggression pact enemy_team`
- Creates 5-day pact from start
- Automatically ends any war

### Online Requirement

The **39% online rule** prevents overpowered war declarations:
- Can't declare war if enemy has <39% members online
- Can't capture chunks if enemy owner has <39% online
- Encourages online activity and fair play

## Technical Details

### Chunk Capture Progress

- Displayed as percentage (0-100%)
- 5 minutes = 300 seconds
- ~0.33% progress per second
- Resets to 0 if:
  - Defenders enter chunk
  - Attacker leaves chunk
  - Owner team falls below 39% online

### Storage

All war data is stored server-side:
- Active wars
- War requests
- Non-aggression pacts (with expiry times)
- Chunk capture progress

Data persists across server restarts.

## Configuration

No configuration files needed. All settings are hardcoded:
- War duration: **Unlimited** (until peace)
- Pact duration: **5 days**
- Capture duration: **5 minutes**
- Online threshold: **39%**

## Troubleshooting

**"Team not found"**
- Check exact team name
- Team must exist in FTB Teams

**"Less than 39% online"**
- Wait for more team members to join
- Check team roster with `/ftb teams list`

**Chunks not capturing**
- Ensure you're at war with owner team
- Check owner team has 39%+ online
- Stay in chunk for full 5 minutes

**PvP not working**
- Confirm war is active: `/infowar`
- Check opponent team is listed

## Support

Report bugs or suggest features on GitHub:
https://github.com/aroslava027-prog/warmodadd

## License

MIT License - See LICENSE file
