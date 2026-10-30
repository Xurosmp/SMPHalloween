# SMPHalloween

Halloween-Event als **Zusatz-Plugin für SMPCore** (Paper 1.21.4+, Java 21). SMPCore selbst muss nicht verändert werden.

## Was es macht
- Überall in der Welt erscheinen **Halloween-Kürbisse** (echte Blöcke mit Partikeln, Namensschild und Action-Bar-Hinweis mit Pfeil + Entfernung).
- Es gibt **31 Kürbisse in 5 Seltenheiten**: Gewöhnlich, Selten, Episch, Legendär, Mythisch (alles in `pumpkins.yml` änderbar).
- Sammeln per **Abbauen oder Rechtsklick** (Bedrock-freundlich).
- **Kürbis-Index** (`/halloween`): Jeder Kürbis ist erst `???`. Beim **ersten Fund** wird er im Index freigeschaltet und du bekommst **Geld + Stars** (Stars = SMPCore-Kristalle).
- Doppelte Funde geben einen kleinen Geld-Bonus (Standard 15 %).
- **Set-Bonus** pro Seltenheit und **Komplett-Bonus** für den ganzen Index.
- Erste Entdeckung seltener Kürbisse wird im Chat verkündet, Legendäre/Mythische mit Feuerwerk.
- `/halloween top` = Bestenliste.

## JAR über GitHub bekommen (alles über die build.yml)
1. Neues GitHub-Repo anlegen und den Inhalt dieses Ordners hochladen (Push auf `main`).
2. Tab **Actions** -> Workflow **Build SMPHalloween** läuft automatisch.
3. Fertige JAR: **Releases -> Latest Build -> SMPHalloween.jar** (wird bei jedem Push neu gebaut).
4. Für feste Versionen: `git tag v1.0.0 && git push --tags` -> eigenes Release.

## Installation
`SMPHalloween.jar` zusammen mit `SMPCore.jar` in `plugins/` legen und Server neu starten (kein `/reload`).
Das Plugin erkennt SMPCore automatisch und vergibt Geld/Stars über dessen Economy.

## Befehle
| Befehl | Beschreibung |
|---|---|
| `/halloween` (`/hw`, `/kuerbis`, `/pumpkins`) | Index öffnen |
| `/halloween top` | Bestenliste |
| `/halloween hints` | Action-Bar-Hinweise an/aus |
| `/halloween spawn <id>` | Admin: Kürbis vor dir spawnen |
| `/halloween clear` | Admin: alle Kürbisse entfernen |
| `/halloween toggle` | Admin: Event an/aus |
| `/halloween status` | Admin: Status |
| `/halloween reload` | Admin: Config + Kürbisse neu laden |
| `/halloween grant <Spieler> <id>` | Admin: Kürbis für Spieler freischalten (mit Belohnung) |
| `/halloween reset <Spieler>` | Admin: Index zurücksetzen |

Rechte: `halloween.use` (alle), `halloween.admin` (OP).

## Konfiguration
- `config.yml`: Spawn-Rate, Radius, Maximalzahl, Lebensdauer, erlaubte Welten (`spawning.worlds`), Event-Zeitraum (`event.start/end`), Boni, Broadcast-Seltenheit.
- `pumpkins.yml`: alle Kürbisse (Name, Seltenheit, Spawn-Gewicht, Block, Geld, Stars, Partikel).

## Hinweise
- Kürbisse sind **global**: Wer zuerst klickt, bekommt ihn. Nicht eingesammelte verschwinden nach `lifetime-seconds`.
- Die Blöcke ersetzen nur Luft/Gras und werden beim Entfernen wiederhergestellt. Nach einem Crash räumt das Plugin beim nächsten Start auf (`active.yml`).
- Explosionen, Kolben und Feuer können die Event-Blöcke nicht zerstören.
- Stars/Geld landen in den SMPCore-Spielerdaten und werden von SMPCore mit gespeichert.
