# Analyse MIUI/HyperOS — PowerKeeper & SecurityCenter

## Architecture globale

Deux applications système gèrent l'énergie et les permissions :

| App | Rôle |
|-----|------|
| `com.miui.powerkeeper` | Gestionnaire d'énergie (restrictions arrière-plan, thermal, kill policies) |
| `com.miui.securitycenter` | Centre de sécurité (autostart, routines, anti-spam, game turbo) |

---

## com.miui.powerkeeper

### Bases de données

| Base | Rôle |
|------|------|
| `cloud_configure.db` | Configuration cloud — règles par défaut pour chaque app + paramètres globaux |
| `user_configure.db` | Surcharges utilisateur (via Paramètres > Batterie) |
| `powerchecker.db` | Historique des comportements anormaux |
| `thermal.db` | Gestion thermique (throttling, IEC) |
| `customerpowerchecker.db` | Règles client spécifiques |

### cloud_configure.db → cloudAppTable (table principale)

Colonnes et leur signification :

| Colonne | Description |
|---------|-------------|
| `pkgName` | Package de l'application |
| `bgData` | `"true"` = restreindre les données en arrière-plan |
| `bgDataDelayTime` | Délai avant restriction data (minutes) |
| `bgDataDelayCount` | Nombre d'événements data avant restriction |
| `bgDataMinDataKb` | Seuil mini de data (KB) avant restriction |
| `bgDataMaxInactiveCount` | Max d'inactivité avant restriction |
| `bgLocation` | `"true"` = restreindre la localisation arrière-plan |
| `bgLocationDelayTime` | Délai avant restriction localisation |
| `k_delay` | Délai avant kill (‑2 = immédiat, `‑1` = valeur globale, >0 = minutes) |
| `s_delay` | Délai avant standby |
| `l_delay_hot` | Délai localisation "à chaud" |
| `k_delay_hot` | Délai kill "à chaud" |
| `i_delay` | Délai idle |
| `power_state_id` | Catégorie d'application |
| `s_disable_type` | Type de désactivation en standby |
| `k_policy` | Politique de kill (896 = agressive) |
| `c_policy` | Politique complémentaire |

### power_state_id — Catégories d'applications

| ID | Catégorie | Exemples |
|----|-----------|----------|
| -2 | Système critique | WeChat, Maps |
| -1 | Par défaut | — |
| 0 | Communication | QQ |
| 2 | Téléchargement | QQDownloader |
| 7 | Vidéo/Média | YouTube, TikTok |
| 9 | Jeux (classic) | Honor of Kings, PUBG |
| 10 | Navigation | Maps, Baidu Map |
| 11 | Benchmarks | Antutu |
| 13 | PUBG spécifique | PUBG Mobile |
| 16 | Jeux lourds | Fortnite, Onmyoji |
| 17 | Streaming | QQ Live, UCMobile |
| 100 | Apps système | Email, Browser |
| 101 | Communication | WeChat |
| 103 | Caméra | Camera |
| 104 | Actualités | Article lite |
| 105 | Navigateur | Browser |
| 110 | Vidéo longue | Youku, TikTok |
| 111 | Recherche | Baidu |
| 113 | Comms | DuoWan |

### cloud_configure.db → GlobalFeatureTable

Paramètres globaux importants :

| Paramètre | Valeur | Signification |
|-----------|--------|---------------|
| `featureStatus` | `true` | Restriction cloud activée |
| `k_policy` | `896` | Politique de kill globale agressive |
| `k_delay` | `-2` | Kill immédiat (global) |
| `s_delay` | `-1` | Délai standby par défaut |
| `i_delay` | `-1` | Délai idle par défaut |
| `userConfigureStatus` | `enhance` | Mode utilisateur avancé |
| `bgData` | `false` | Restriction data globale désactivée |
| `bgLocation` | `false` | Restriction localisation globale désactivée |
| `bgDataDisableShortTime` | `3` | Temps court désactivation data |
| `bgDataDisableLongTime` | `10` | Temps long désactivation data |
| `bgDataDelayTime` | `3` | Délai avant restriction data (minutes) |
| `bgDataDelayCount` | `20` | Compteur avant restriction data |
| `bgDataMinDataKb` | `100` | Seuil mini data (KB) |
| `bgLocationDelayTime` | `1` | Délai avant restriction localisation |
| `SensorControlStatus` | `true` | Contrôle des capteurs activé |
| `bleScanBlock` | `true` | Blocage scan BLE en arrière-plan |
| `bleScanParam` | `{"parolePeriodArray": [1920000, 5000]}` | Paramètres scan BLE |
| `launchRestrict` | `enable_24_allowlist_...` | Whitelist de 24 apps autorisées à se lancer |
| `levelUtimateSpecialApps` | (longue liste) | Apps protégées (jamais killées) : messageries, musique, maps |
| `dozeWhiteListApps` | (longue liste) | Apps exemptées du Doze |
| `FrozenNewWhiteList` | (liste) | Apps exemptées du gel |
| `default_power_mode` | `enhance` | Mode puissance par défaut |
| `power_mode_optimization` | `{"boost":"true"}` | Optimisation => boost activé |
| `noCoreSystemApps` | (liste) | Apps système non-core |
| `musicApps` | (vide) | Apps musique (whitelist) |
| `alarmAlign` | (vide) | Alignment des alarmes |
| `standbyChainDelay` | `-1` | Délai standby chaîné |
| `bright_frozen` | (vide) | Freeze écran allumé |
| `lightIdleStatus` | `false` | Idle léger désactivé |
| `miui_idle` | `false` | MIUI idle désactivé |
| `miui_standby` | `false` | MIUI standby désactivé |
| `appIdleStatus` | `false` | App idle désactivé |
| `ClusterStatus` | `false` | Clustering désactivé |
| `hotFeedbackFeature` | `false` | Feedback thermique désactivé |
| `networkFeedbackFeature` | `false` | Feedback réseau désactivé |

### user_configure.db → userTable

Surcharges utilisateur pour les restrictions arrière-plan.

| `bgControl` | Signification | Comportement |
|-------------|---------------|--------------|
| `miuiAuto` | Automatique | Suit les règles cloud |
| `noRestrict` | Aucune restriction | L'app fonctionne librement |
| `restrictBg` | Restreindre l'arrière-plan | Réseau/data coupé après délai, mais app reste en mémoire |
| `noBg` | Interdire l'arrière-plan | App complètement frozen/killée après délai |

`bgDelayMin` = délai en minutes avant application de la restriction.

### powerchecker.db → abnormalTable

Enregistre les comportements anormaux :
- `uid`, `package_name` — App concernée
- `type` — Type d'anomalie
- `paction` — Action recommandée
- `priority` — Priorité
- `flag` — 0 = nouveau, 1 = traité

### thermal.db — Gestion thermique

- `ThermalInfo` — Historique températures
- `thermal_duration` — Données de throttling

Paramètres IEC (Intelligent Energy Conservation) :
```json
{
  "IECKillBatteryTemp": "44",
  "IECScreenOffTimePeriod": "600000",
  "allowedIECBatteryTemp": "300"
}
```

---

## com.miui.securitycenter

### Bases de données

| Base | Contenu |
|------|---------|
| `auto_task.db` | Règles d'automatisation (routines) |
| `power_auto_task.db` | Automatisations simplifiées |
| `cloud_no_kill_pkg.db` | Liste cloud d'apps à ne pas tuer (vide) |
| `manual_list` | Liste blanche manuelle (vide) |
| `gamebooster.db` | Configuration Game Turbo |
| `app_predict_data.db` | Prédiction d'ouverture d'apps |
| `preload.db` | Pré-téléchargement de jeux |
| `AntiSpam` | Anti-spam / blocage d'appels |
| `ThirdDesktop` | Apps de bureau tiers |
| `parse.db` | Répertoires de parsing |
| `battery_history.db` | Historique batterie |

### Shared Preferences — Autostart

Fichier : `com.miui.securitycenter_preferences.xml`

| Clé | Valeur | Signification |
|-----|--------|---------------|
| `app_autostart_pref` | `false` | Fonctionnalité autostart désactivée globalement |
| `app_restricted_setting_pref` | `false` | Restrictions d'apps désactivées |
| `app_hiberanations_pref` | `true` | Hibernation des apps activée |

### Où sont stockées les permissions d'autostart ?

**Les permissions individuelles d'autostart par app ne sont PAS dans les bases extraites.** Elles sont stockées au niveau système Android via `AppOpsManager` dans :

```
/data/system/appops.xml
```

Le mécanisme :
```
Paramètres MIUI → Apps → Autostart
         ↓
com.miui.securitycenter (UI)
         ↓
AppOpsManager.setMode(OP_AUTO_START, uid, pkg, mode)
         ↓
/data/system/appops.xml
```

### Routines (auto_task.db)

Table `auto_task_table` :

| task_title | Condition | Action |
|------------|-----------|--------|
| "De 00:00 à 07:00" | Plage horaire (00:00‑07:00) | Mode Avion |
| "Lorsque la batterie atteint 20%" | Batterie ≤ 20% | Mode silencieux + luminosité 0 + Bluetooth off |

Table `autotasks` (power_auto_task.db) : Mêmes données, format simplifié.

---

## Mécanisme de décision de restriction

```
1. User overrides ? (user_configure.db → userTable)
   ├── noRestrict  → Aucune restriction
   ├── noBg        → Interdiction totale après délai
   ├── restrictBg  → Restriction data/location après délai
   └── miuiAuto    → Suivre règles cloud (étape 2)

2. Règles cloud (cloud_configure.db → cloudAppTable)
   ├── bgData / bgLocation → Restrictions data/location
   ├── k_policy            → Politique de kill
   ├── power_state_id      → Catégorie (jeu, vidéo, etc.)
   └── s_disable_type      → Désactivation standby

3. Appliquer délais (k_delay, s_delay, bgDelayMin, etc.)

4. Vérifier listes d'exemption
   ├── levelUtimateSpecialApps → Ne jamais tuer
   ├── dozeWhiteListApps       → Exemption Doze
   ├── launchRestrict          → Apps autorisées à se lancer
   └── FrozenNewWhiteList      → Exemption de gel
```
