# Plan de construction — HyperController

## Contexte

HyperController est une application Android root qui permet aux utilisateurs de contrôler finement les restrictions d'applications imposées par MIUI/HyperOS via `com.miui.powerkeeper` et `com.miui.securitycenter`. Le problème : MIUI restreint les apps en arrière-plan (notifications Instagram qui ne passent pas, etc.), forçant les utilisateurs à configurer chaque app une par une dans l'app Sécurité — sans possibilité de définir des defaults pour les nouvelles apps.

L'app cible les power users qui veulent :
- Visualiser en un coup d'œil l'état de toutes leurs apps
- Appliquer des changements en batch
- Créer et appliquer des templates de configuration
- Auto-configurer les nouvelles apps installées

---

## Architecture technique

### Stack

| Couche | Technologie |
|--------|-------------|
| Langage | **Kotlin** |
| UI | **Jetpack Compose** + Material 3 |
| Root | `ProcessBuilder` + `su -c` |
| Base de données | `sqlite3` via shell root (binaire embarqué dans l'APK) |
| Min SDK | **API 33** (Android 13) |
| Target SDK | **API 35** (Android 15) |
| Build | Gradle + Kotlin DSL |
| Naming | `com.walkoud.hypercontroller` |

### Structure du projet

```
app/
├── src/main/java/com/walkoud/hypercontroller/
│   ├── HyperControllerApp.kt          # Application class
│   ├── MainActivity.kt                 # Single activity host
│   │
│   ├── core/
│   │   ├── root/
│   │   │   ├── RootShell.kt            # Exécution de commandes root (su)
│   │   │   ├── RootChecker.kt          # Vérification disponibilité root
│   │   │   └── Sqlite3Manager.kt       # Gestion du binaire sqlite3 embarqué
│   │   ├── db/
│   │   │   ├── PowerKeeperDb.kt        # Accès à cloud_configure.db / user_configure.db
│   │   │   ├── SecurityCenterDb.kt     # Accès aux DB de securitycenter
│   │   │   └── DbExecutor.kt           # Exécution SQL via sqlite3 shell
│   │   ├── safety/
│   │   │   ├── SafetyValidator.kt      # Validation des valeurs SQL / protections
│   │   │   ├── SystemAppGuard.kt       # Protection des apps système critiques
│   │   │   ├── BackupManager.kt        # Backup/restore des DB avant modifications
│   │   │   └── SafetyRules.kt          # Règles de sécurité (values autorisées, etc.)
│   │   ├── model/
│   │   │   ├── AppInfo.kt              # Modèle app + sa restriction
│   │   │   ├── RestrictionState.kt     # Enum des 4 états
│   │   │   ├── Template.kt             # Modèle de template
│   │   │   └── ThermalConfig.kt        # Config thermique
│   │   └── util/
│   │       ├── PackageUtils.kt         # Listing packages installés
│   │       └── AppClassifier.kt        # Classification auto des apps (comms, games, etc.)
│   │
│   ├── service/
│   │   └── AutoConfigService.kt        # Détection nouvelles apps + auto-apply template
│   │
│   ├── ui/
│   │   ├── navigation/
│   │   │   └── NavGraph.kt             # Navigation Compose
│   │   ├── screens/
│   │   │   ├── applist/
│   │   │   │   ├── AppListScreen.kt    # Liste des apps
│   │   │   │   ├── AppListViewModel.kt
│   │   │   │   └── AppListItem.kt      # Composant ligne app
│   │   │   ├── detail/
│   │   │   │   ├── AppDetailScreen.kt  # Détail d'une app
│   │   │   │   └── AppDetailViewModel.kt
│   │   │   ├── templates/
│   │   │   │   ├── TemplateScreen.kt   # Gestion des templates
│   │   │   │   ├── TemplateViewModel.kt
│   │   │   │   └── TemplateEditor.kt   # Création/édition template
│   │   │   └── settings/
│   │   │       ├── SettingsScreen.kt    # Paramètres
│   │   │       └── SettingsViewModel.kt
│   │   └── theme/
│   │       └── Theme.kt                # Thème Material 3
│   │
│   └── res/
│       ├── xml/
│       │   └── root_shell.sh           # Scripts shell utilitaires
│       └── values/
│           └── strings.xml
```

---

## Sécurités — PROTECTION DES APPS SYSTÈME ET DES BASES

Avant toute chose : **HyperController manipule des bases système critiques**. Une erreur peut rendre le téléphone instable, empêcher les notifications, ou forcer un restore. Voici les protections à implémenter.

### S1 : BackupManager — Sauvegarde automatique

**Fichier :** `BackupManager.kt`

- **Avant chaque écriture** dans une DB, créer un backup dans `/data/local/tmp/hypercontroller_backups/`
- Format : `{db_name}_backup_{timestamp}.db`
- Backup automatique au lancement de l'app (état initial)
- Fonction "Restore" dans les paramètres pour revenir à l'état précédent
- Rotation des backups (garder les 5 derniers)

```kotlin
class BackupManager(private val rootShell: RootShell) {
    private val backupDir = "/data/local/tmp/hypercontroller_backups/"
    fun backup(dbPath: String, dbName: String): String
    fun restore(dbName: String, timestamp: String): Boolean
    fun listBackups(dbName: String): List<BackupInfo>
    fun restoreLatest(dbName: String): Boolean
}
```

### S2 : SystemAppGuard — Protection des apps critiques

**Fichier :** `SystemAppGuard.kt`

Liste noire **absolue** d'apps qui ne doivent JAMAIS être modifiées par HyperController :

```kotlin
object SystemAppGuard {
    val CRITICAL_SYSTEM_APPS = setOf(
        "com.android.systemui",            // Interface système
        "com.android.phone",               // Téléphone
        "com.android.settings",            // Paramètres
        "com.miui.home",                   // Launcher MIUI
        "com.miui.securitycenter",         // App Sécurité — PROTÉGÉE
        "com.miui.securityadd",            // Sécurité add-on
        "com.miui.powerkeeper",            // PowerKeeper lui-même
        "com.miui.securitycore",           // Security core
        "com.lbe.security.miui",           // Security core MIUI
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.google.android.gms",          // Google Play Services
        "com.google.android.gsf",
        "com.android.vending",
        "com.xiaomi.xmsf",                 // Xiaomi Services Framework
        "com.android.incallui",
        "com.android.server.telecom",
    )

    val SENSITIVE_SYSTEM_APPS = setOf(
        "com.android.camera", "com.miui.gallery", "com.android.contacts",
        "com.android.mms", "com.android.email", "com.miui.weather2",
        "com.miui.player", "com.miui.notes", "com.miui.videoplayer",
        "com.xiaomi.market"
    )

    fun isCritical(pkgName: String): Boolean
    fun isSensitive(pkgName: String): Boolean
    fun filterCritical(selectedPackages: Set<String>): Set<String>
    fun guardCheckOrThrow(pkgName: String)
}
```

- Apps **critiques** : impossible de les sélectionner ou modifier — grisées dans l'UI
- Apps **sensibles** : dialogue d'avertissement avant modification

### S3 : SafetyValidator — Validation des valeurs SQL

**Fichier :** `SafetyValidator.kt`

Valide TOUTES les valeurs avant écriture :

```kotlin
object SafetyValidator {
    val VALID_BG_CONTROL = setOf("miuiAuto", "noRestrict", "restrictBg", "noBg")
    const val BG_DELAY_MIN = -2
    const val BG_DELAY_MAX = 1440

    val ALLOWED_USER_TABLE_COLUMNS = setOf("bgControl", "bgLocation", "bgDelayMin", "lastConfigured")
    val ALLOWED_CLOUD_APP_COLUMNS = setOf("bgData", "bgLocation", "k_delay", "s_delay", "k_policy", "power_state_id", "i_delay")
    val VALID_K_POLICY = setOf(-1, 0, 896)

    fun validateBgControl(value: String): Boolean
    fun validateDelay(value: Int): Boolean
    fun validateColumn(dbName: String, column: String): Boolean
    fun validateSqlInjection(input: String): Boolean
}
```

- **Anti injection SQL** : toute valeur avec `;`, `DROP`, `DELETE`, `--` est rejetée
- **Colonnes restreintes** : impossible d'écrire dans `_id`, `userId`, etc.
- **Valeurs contrôlées** : seules les valeurs connues sont acceptées

### S4 : SafetyRules — Règles globales

**Fichier :** `SafetyRules.kt`

1. Pas de modification des apps dans `CRITICAL_SYSTEM_APPS`
2. Pas de `DELETE` / `DROP` / `ALTER` / `CREATE` — bloqué par `DbExecutor`
3. Backup automatique avant chaque écriture
4. Journalisation de toutes les modifications
5. Mode **"Dry run"** obligatoire avant application d'un template

```kotlin
// Exemple de requête sécurisée :
fun setAppRestriction(pkgName: String, state: RestrictionState, delayMin: Int) {
    SystemAppGuard.guardCheckOrThrow(pkgName)
    require(SafetyValidator.validateBgControl(state.bgControl))
    require(SafetyValidator.validateDelay(delayMin))
    backupManager.backup(userDbPath, "user_configure")

    val sql = "UPDATE userTable SET bgControl='${state.bgControl}', " +
              "bgDelayMin=$delayMin, lastConfigured=${System.currentTimeMillis()} " +
              "WHERE pkgName='$pkgName' AND userId=0"

    require(SafetyValidator.validateSqlInjection(sql))
    dbExecutor.execSQL(userDbPath, sql)
    SafetyLogger.log("setAppRestriction: $pkgName -> ${state.bgControl}")
}
```

### S5 : Protection de l'app Sécurité

- **com.miui.securitycenter** est en liste critique → jamais modifié, jamais killé
- **Ne pas altérer** les tables `auto_task.db` (routines utilisateur)
- **Ne pas toucher** aux colonnes `userId`, `_id`
- HyperController est une surcouche, pas un remplacement de l'app Sécurité

---

## Étapes de construction

### Étape 1 : Projet Android + Root access + Binaire sqlite3

**Fichiers :** `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, `RootShell.kt`, `RootChecker.kt`, `Sqlite3Manager.kt`

- Projet Gradle Kotlin + Compose
- `RootShell` : `ProcessBuilder("su", "-c", command)` avec timeout
- `RootChecker` : vérifie `su` disponible + accès aux DB cibles
- **Binaire sqlite3 embarqué** : 
  - Toutes les ROMs HyperOS n'incluent pas `sqlite3` nativement
  - Intégrer un binaire `sqlite3` compilé pour `arm64-v8a` dans `res/raw/sqlite3`
  - `Sqlite3Manager.kt` : au premier lancement, copie le binaire vers `/data/data/com.walkoud.hypercontroller/files/sqlite3`, puis `chmod 755` via root
  - `RootShell` utilise ce binaire en priorité : `su -c "/data/data/com.walkoud.hypercontroller/files/sqlite3 <db> \"<sql>\""`
  - Fallback : si le binaire embarqué échoue, tenter `/system/bin/sqlite3` puis `/system/xbin/sqlite3`

### Étape 2 : Couche base de données

**Fichiers :** `DbExecutor.kt`, `PowerKeeperDb.kt`, `SecurityCenterDb.kt`

- Requêtes via `su -c "sqlite3 <path> \"<sql>\""`
- **Commandes bloquées** : `DROP`, `ALTER`, `CREATE`, `DELETE`
- Chemins cibles : `/data/data/com.miui.powerkeeper/databases/*.db`
- Méthodes typées avec sécurités intégrées

### Étape 3 : Modèles de données

**Fichiers :** `AppInfo.kt`, `RestrictionState.kt`, `Template.kt`, `ThermalConfig.kt`

- 4 états : `noRestrict`, `miuiAuto`, `restrictBg`, `noBg`
- `Template` avec `TemplateAction` sealed class
- Pas de cible `SYSTEM_APPS` dans les templates user

### Étape 4 : Listing des apps installées

**Fichiers :** `PackageUtils.kt`, `AppClassifier.kt`

- `PackageManager.getInstalledApplications()`
- Associe chaque app à son état via `PowerKeeperDb`
- Marque les apps critiques/sensibles via `SystemAppGuard`
- Classification : COMMUNICATION, SOCIAL, GAME, MAPS_NAV, MUSIC, VIDEO, BENCHMARK, OTHER

### Étape 5 : UI — Écran liste des apps (page principale)

**Fichiers :** `AppListScreen.kt`, `AppListItem.kt`, `AppListViewModel.kt`

C'est la **page d'accueil** de l'app, celle qui s'affiche au lancement.

**Éléments affichés :**
- **Barre de recherche** en haut — tapez le nom d'une app pour filtrer
- **Filtres rapides** (chips) : Toutes | User | Système | Communication | Jeux | Social | Vidéo | **🔔 Notifications bloquées**
- `🔔 Notifications bloquées` : filtre les apps qui ont la permission notification activée MAIS sont en `restrictBg` ou `noBg` — détecte les apps qui devraient notifier mais ne le peuvent pas à cause de MIUI
- **Tri** : bouton pour trier par nom A-Z / Z-A / par état / par catégorie
- **Compteur** : "147 apps • 32 modifiées"

**Liste des apps (LazyColumn) :**
Chaque ligne (`AppListItem`) contient :
- **Icône** de l'app (ronde, extraite via PackageManager)
- **Nom** de l'app (en gras) + **package name** (en grisé dessous)
- **Badge d'état** : pastille colorée avec le texte :
  - 🟢 `Liberté totale` (noRestrict)
  - 🟠 `Auto MIUI` (miuiAuto)
  - 🔴 `Économie soft` (restrictBg)
  - ⚫ `Kill strict` (noBg)
  - 🔒 `Protégée` (app critique, grisée, non sélectionnable)
- **Délai** affiché à droite si personnalisé (ex: "10 min")
- **Indicateur de notification** (🔔) : icône en forme de cloche à côté du nom. L'app interroge `NotificationManager.areNotificationsEnabled()` pour chaque package. Si l'app a le droit d'envoyer des notifications mais qu'elle est en `restrictBg` ou `noBg`, l'icône clignote en **orange** pour signaler un conflit potentiel (notifications perdues). Filtre dédié : "Apps Notifications bloquées" pour voir d'un coup toutes les apps qui risquent de ne pas notifier.

**Mode sélection / Batch :**
- Long-press sur une app → entre en mode sélection
- Checkbox apparaît sur chaque ligne
- **Barre d'actions batch** en bas :
  - `Appliquer "Liberté totale"` → vert
  - `Appliquer "Économie soft"` → orange
  - `Appliquer "Kill strict"` → rouge
  - `Restaurer "Auto MIUI"` → gris
  - `Sélectionner tout` / `Désélectionner tout`
- Les apps critiques sont automatiquement exclues de la sélection
- **Progress dialog** pendant l'application

**Comportement :**
- Scroll infini (lazy)
- Pull-to-refresh → re-scan la DB + PackageManager
- Tap sur une app → navigation vers l'écran détail

### Étape 6 : UI — Écran détail app

**Fichiers :** `AppDetailScreen.kt`, `AppDetailViewModel.kt`

Page ouverte quand on tape sur une app depuis la liste.

**Section 1 — En-tête :**
- Icône large de l'app
- Nom de l'app + package name
- Type : `App utilisateur` ou `App système`
- Statut critique/sensible avec icône

**Section 2 — État actuel :**
- Carte avec l'état actuel bien mis en évidence
- Progression visuelle de la restriction (de "aucune" à "stricte")
- Infos DB brutes (mode avancé pliable) :
  ```
  power_state_id: 9 (Jeu)
  k_policy: 16
  k_delay: -2
  bgData: true
  bgLocation: true
  ```

**Section 3 — Changer l'état :**
- 4 grandes cartes cliquables (radio) :
  | Carte | Icône | Description |
  |-------|-------|-------------|
  | **Liberté totale** | ✅ | Aucune restriction, l'app tourne librement |
  | **Auto MIUI** | 📱 | Suit les règles par défaut de Xiaomi |
  | **Économie soft** | 💤 | Data/localisation coupées en arrière-plan |
  | **Kill strict** | 🚫 | App frozen/killée au passage en arrière-plan |

- **Slider "Délai avant application"** (minutes) : de -2 (immédiat) à 1440 (24h)
  - Affiché en texte : "Immédiat", "5 min", "30 min", "1 h", etc.
- **Bouton "Appliquer"** — si app sensible, dialogue de confirmation

**Section 4 — Infos complémentaires (lecture seule) :**
- Catégorie détectée (Communication, Jeu, etc.)
- Exemptions : Doze, Kill, Launch Restrict
- Template utilisé si applicable
- Dernière modification : timestamp

**Cas particuliers :**
- App critique : 🚫 Section 3 remplacée par "App système protégée" avec explication
- App sensible : Dialogue "Cette app est une app système sensible. Confirmer ?"

### Étape 7 : UI — Templates

**Fichiers :** `TemplateScreen.kt`, `TemplateViewModel.kt`

Page accessible via l'onglet "Templates" dans la bottom nav.

**Section 1 — Templates intégrés (built-in) :**
3 cartes non-supprimables, avec icône distinctive :

| Template | Icône | Description |
|----------|-------|-------------|
| **Unchained** | 🔓 | Libère les apps sélectionnées → `noRestrict` + exemptions Doze/Kill |
| **Anti-Kill Global** | 🛡️ | Désactive le tueur de tâches agressif de MIUI |
| **Thermal Unlock** | 🌡️ | Repousse les limites thermiques pour les performances |

Chaque carte montre :
- Nom + description + icône
- Nombre d'actions dans le template
- Badge "Built-in"
- Bouton "Appliquer" → lance le dry run

**Section 2 — Templates personnalisés :**
- Bouton "+ Créer un template"
- Liste des templates sauvegardés (carte avec nom, description, nombre d'actions)
- Swipe to delete
- Tap → éditer (ouvre TemplateEditor)

**Section 3 — Dry run (dialog) :**
Avant d'appliquer un template, un dialogue s'affiche :
```
Template : Unchained
Cible : 12 apps sélectionnées
Apps qui seront modifiées :
  ✓ com.instagram.android → noRestrict
  ✓ com.twitter.android → noRestrict
  ✓ com.whatsapp → noRestrict
  ⚠️ com.miui.securitycenter → BLOQUÉE (app critique)

Actions système :
  ✓ Ajout à levelUtimateSpecialApps : 12 apps
  ✓ Ajout à dozeWhiteListApps : 12 apps

[ANNULER] [APPLIQUER]
```

- **Rapport final** après application : "8/8 actions réussies, 1 app ignorée"

### TemplateEditor — Création/Édition de template

**Fichier :** `TemplateEditor.kt`

Page de création de template :

- **Nom du template** (champ texte)
- **Description** (champ texte multiligne)
- **Ajouter des actions** :
  1. **Type d'action** : dropdown (SetRestriction, SetGlobal, SetThermal)
  2. **Cible** (si SetRestriction) : "Toutes les apps user", "Communications", "Jeux", "Packages spécifiques"
  3. **État** (si SetRestriction) : noRestrict / miuiAuto / restrictBg / noBg
  4. **Délai** (si SetRestriction) : slider minutes
  5. **Clé** (si SetGlobal) : dropdown des clés connues (featureStatus, k_policy, etc.)
  6. **Valeur** (si SetGlobal) : champ avec validation
  7. **Param** (si SetThermal) : dropdown (IECKillBatteryTemp, etc.)
  8. **Valeur** (si SetThermal) : champ avec validation

- Liste des actions ajoutées (avec swipe to delete)
- Bouton "Enregistrer"

### Étape 8 : Auto-config pour nouvelles apps

**Fichiers :** `AutoConfigService.kt`

Cette fonctionnalité est configurée depuis les Settings et tourne en arrière-plan.

- `BroadcastReceiver` pour `ACTION_PACKAGE_ADDED` + `ACTION_PACKAGE_REPLACED`
- Notification persistante : "HyperController — Auto-config active"
- Quand une nouvelle app est détectée :
  1. Vérifier si Auto Config est activé (Settings)
  2. Vérifier que l'app n'est pas critique (`SystemAppGuard`)
  3. Charger le template sélectionné dans les Settings
  4. Vérifier si l'app correspond aux critères (toutes / communications / jeux)
  5. **Attendre que la DB soit prête** : Xiaomi peut mettre 500ms-2s avant de créer la ligne dans `userTable`. Boucle d'attente : jusqu'à 5 tentatives espacées de 500ms, avec `SELECT` pour vérifier l'existence
  6. **Utiliser `INSERT OR REPLACE` au lieu de `UPDATE`** : si la ligne n'existe pas encore dans `userTable`, `UPDATE` échoue. `INSERT OR REPLACE` garantit l'écriture dans tous les cas :
     ```sql
     INSERT OR REPLACE INTO userTable (userId, pkgName, bgControl, bgDelayMin, lastConfigured)
     VALUES (0, 'com.instagram.android', 'noRestrict', -1, 1783925352703)
     ```
  7. Backup → Appliquer → Log
  8. Notification toast : "HyperController : Instagram configuré → Liberté totale"

### Étape 9 : UI — Paramètres (Settings)

**Fichiers :** `SettingsScreen.kt`, `SettingsViewModel.kt`

C'est la page "Settings" dans la bottom nav, organisée en sections :

**Section 1 — Root & Système :**
| Élément | Type | Description |
|---------|------|-------------|
| Statut Root | ✅ / ❌ | Vérification en direct |
| Tester l'accès | Bouton | Execute `su -c "id"` |
| Version HyperController | Texte | `v1.0.0` |
| Version MIUI détectée | Texte | `OS3.0.315.0.WBLCNXM` |

**Section 2 — Backup & Restore :**
| Élément | Type | Description |
|---------|------|-------------|
| Backup now | Bouton | Sauvegarde toutes les DB cibles |
| Restore | Liste | Liste des backups avec date + bouton Restore |
| Rotation | Texte | "5 backups max par DB" |

**Section 3 — Auto Config :**
| Élément | Type | Description |
|---------|------|-------------|
| Auto Config | Toggle | ON / OFF |
| Template appliqué | Dropdown | Choisir parmi les templates sauvegardés |
| Cible | Dropdown | "Toutes les apps" / "Communications uniquement" / "Jeux uniquement" |
| Statut | Texte | "Actif • 3 apps configurées automatiquement" |

**Section 4 — Comportement :**
| Élément | Type | Description |
|---------|------|-------------|
| Kill After Apply | Toggle **ON par défaut** | `pkill` forcé après modifs. PowerKeeper garde les états en cache RAM : sans `pkill`, les changements sont invisibles jusqu'au reboot. |
| Dry run avant apply | Toggle (ON par défaut) | Toujours prévisualiser |
| Mode sombre | Toggle | Suivre système / Clair / Sombre |

**Section 5 — Sécurité :**
- Statut des garde-fous :
  ```
  ✅ SystemAppGuard : actif (18 apps critiques protégées)
  ✅ SafetyValidator : actif
  ✅ BackupManager : actif
  ✅ Anti-injection SQL : actif
  ```

**Section 6 — Help & Manual :**
- Guide d'utilisation pas à pas
- Explication de chaque état (noRestrict → noBg)
- Avertissements :
  > ⚠️ Modifier les bases système peut rendre votre téléphone instable.
  > Utilisez les templates built-in en priorité.
  > Un backup est automatique créé avant chaque modification.
- Liens : GitHub, Signal group, contact

### Étape 10 : Splash Screen / Root Check (premier lancement)

**Fichier :** `MainActivity.kt`

Au premier lancement (ou si root perdu) :

```
╔══════════════════════════════╗
║                              ║
║     🔧 HyperController       ║
║     Vérification du root     ║
║                              ║
║     [⟳] Vérification...      ║
║                              ║
║     ✓ Root access : OK       ║
║     ✓ sqlite3 : OK           ║
║     ✓ DB powerkeeper : OK    ║
║     ✓ DB securitycenter : OK ║
║                              ║
║     [CONTINUER]              ║
║                              ║
╚══════════════════════════════╝
```

Si root absent :
```
╔══════════════════════════════╗
║     ❌ Root requis            ║
║                              ║
║     Cette app nécessite      ║
║     un accès root complet.   ║
║                              ║
║     Assurez-vous que :       ║
║     • Magisk / KernelSU      ║
║     • `su` disponible        ║
║     • `sqlite3` disponible   ║
║                              ║
║     [RÉESSAYER]              ║
╚══════════════════════════════╝
```

### Étape 11 : Navigation

**Fichiers :** `NavGraph.kt`, `MainActivity.kt`

- **Bottom Navigation Bar** avec 3 onglets :
  | Onglet | Icône | Page |
  |--------|-------|------|
  | Apps | 📱 | AppListScreen |
  | Templates | 📋 | TemplateScreen |
  | Settings | ⚙️ | SettingsScreen |

- **Navigation hiérarchique** :
  - AppList → AppDetail (tap sur une app)
  - TemplateScreen → TemplateEditor (bouton "+")
  - TemplateScreen → Dry run dialog (bouton "Appliquer")

- **Dialogues globaux** :
  - Dry run preview (avant apply template)
  - Batch progress (pendant application)
  - Confirmation pour apps sensibles

---

## Templates built-in

### Unchained
Apps sélectionnées → `noRestrict` + exempt Doze/Kill.

### Anti-Kill Global
`featureStatus=false`, `k_policy=0`, `miui_idle=false`, `miui_standby=false`.

### Thermal Unlock
`IECKillBatteryTemp=50`, `allowedIECBatteryTemp=350`, `IECScreenOffTimePeriod=0`.

---

## Vérification

1. **Root** : `su -c "echo ok"` → `ok`
2. **Accès DB** : les tables des DB cibles sont listées
3. **Listing apps** : toutes les apps affichées avec leurs états, critiques grisées
4. **Changement simple** : app user → `noRestrict` → DB modifiée
5. **Protection critique** : modification de `com.miui.securitycenter` → bloquée
6. **Batch** : 5 apps sélectionnées → modifiées, critiques ignorées
7. **Template** : dry run → confirmation → appliqué
8. **Backup** : backup créé → restore → état rétabli
9. **Auto Config** : nouvelle app installée → template appliqué sans toucher aux apps système
10. **pkill** : `pkill com.miui.powerkeeper` → process redémarre
