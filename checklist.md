# Checklist — HyperController

> Statut : ✅ Fait | 🔄 En cours | ⏳ Pas commencé | ❌ Bloqué

---

## Phase 1 : Projet Android & Build System ✅

- [x] ✅ `settings.gradle.kts` — projet + repositories
- [x] ✅ `build.gradle.kts` root — plugins AGP 8.5.0 + Kotlin 2.0.0
- [x] ✅ `app/build.gradle.kts` — Compose BOM 2024.06, Material 3, minSdk 33, targetSdk 35
- [x] ✅ `gradle.properties` — Java 17, AndroidX
- [x] ✅ `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.7
- [x] ✅ `AndroidManifest.xml` — sans permissions spéciales (root suffit)
- [x] ✅ `.gitignore` — règles Android standard

---

## Phase 2 : Core — Root & Base de données ✅

### Root
- [x] ✅ `RootShell.kt` — exécution `su -c` avec timeout 15s
- [x] ✅ `RootChecker.kt` — détection SU paths + accès DB + version MIUI
- [x] ✅ `Sqlite3Manager.kt` — déploiement binaire sqlite3 (`res/raw/` → `filesDir`)

### Base de données
- [x] ✅ `DbExecutor.kt` — exécution SQL via sqlite3 shell, blocage DROP/DELETE/ALTER
- [x] ✅ `PowerKeeperDb.kt` — accès typé cloud_configure.db + user_configure.db + thermal.db
- [x] ✅ `SecurityCenterDb.kt` — accès à auto_task.db + no_kill_pkg.db

---

## Phase 3 : Core — Sécurité & Modèles ✅

### Sécurité
- [x] ✅ `SafetyValidator.kt` — validation bgControl, delay, colonnes, anti-injection SQL
- [x] ✅ `SystemAppGuard.kt` — 19 apps critiques + 10 sensibles, filtres
- [x] ✅ `BackupManager.kt` — backup/restore DB, rotation 5 max, `restoreAll()`
- [x] ✅ `SafetyRules.kt` — SafetyLogger avec events typés

### Modèles
- [x] ✅ `RestrictionState.kt` — enum 4 états (noRestrict, miuiAuto, restrictBg, noBg)
- [x] ✅ `AppInfo.kt` — data class avec état, catégorie, conflit notification
- [x] ✅ `AppRestriction.kt` — data class pour les retours DB
- [x] ✅ `Template.kt` — template + sealed class TemplateAction + 3 builtins
- [x] ✅ `ThermalConfig.kt` — paramètres thermiques
- [x] ✅ `AutoTask.kt` — modèle pour auto_task_table

---

## Phase 4 : Core — Utilitaires ✅

- [x] ✅ `PackageUtils.kt` — listing apps via PackageManager
- [x] ✅ `AppClassifier.kt` — 8 catégories (COMMUNICATION, SOCIAL, GAME, etc.)

---

## Phase 5 : UI — Pages ✅

### AppList
- [x] ✅ `AppListViewModel.kt` — state + filtres (catégorie/état/recherche) + sélection + batch
- [x] ✅ `AppListItem.kt` — icône, badge état, 🔔 conflit notification
- [x] ✅ `AppListScreen.kt` — LazyColumn, FAB batch, dialog action groupée

### AppDetail
- [x] ✅ `AppDetailViewModel.kt` — chargement + mise à jour + config cloud
- [x] ✅ `AppDetailScreen.kt` — 4 radio boutons état, slider délai, carte config cloud avancée

### Templates
- [x] ✅ `TemplateViewModel.kt` — CRUD templates + apply + persistance JSON
- [x] ✅ `TemplateScreen.kt` — cartes built-in + user + apply dialog
- [x] ✅ `TemplateEditor.kt` — formulaire édition avec sélecteur d'actions

### Settings
- [x] ✅ `SettingsViewModel.kt` — root check, auto-config, kill after apply, thème
- [x] ✅ `SettingsScreen.kt` — root status, configuration, journal de sécurité, à propos

### Navigation & Theme
- [x] ✅ `NavGraph.kt` — bottom nav 3 tabs (Apps/Templates/Settings) + detail routes
- [x] ✅ `Theme.kt` — Material 3 clair/sombre
- [x] ✅ `MainActivity.kt` — Single Activity + déploiement sqlite3 au lancement
- [x] ✅ `HyperControllerApp.kt` — Application class avec sqlite3Manager

---

## Phase 6 : Services ✅

- [x] ✅ `AutoConfigService.kt` — foreground service, apply template on PACKAGE_ADDED
- [x] ✅ `AutoConfigReceiver.kt` — BroadcastReceiver, démarre le service

---

## Phase 7 : Ressources & Build 🔄

- [x] ✅ `strings.xml` — textes français
- [x] ✅ Application d'icône / drawables (adaptive icon vectoriel)
- [x] ✅ `styles.xml`, `colors.xml` — thème XML + couleurs
- [ ] ⏳ `binaire sqlite3 arm64-v8a` → `res/raw/sqlite3`
- [ ] ⏳ Build APK de debug

---

## Phase 8 : Vérification ⏳

- [ ] ⏳ Test : `su -c "echo ok"` → ok
- [ ] ⏳ Test : accès aux bases powerkeeper + securitycenter
- [ ] ⏳ Test : listing des apps avec états corrects
- [ ] ⏳ Test : modification simple d'une app user
- [ ] ⏳ Test : blocage app critique (com.miui.securitycenter)
- [ ] ⏳ Test : batch 5 apps
- [ ] ⏳ Test : template apply
- [ ] ⏳ Test : backup → restore
- [ ] ⏳ Test : auto-config d'une nouvelle app
- [ ] ⏳ Test : pkill powerkeeper → process redémarre

---

## Légende

| Symbole | Signification |
|---------|---------------|
| ⏳ | Pas commencé |
| 🔄 | En cours |
| ✅ | Fait |
| ❌ | Bloqué / Problème |
