package com.walkoud.hypercontroller.core.util

import com.walkoud.hypercontroller.core.model.AppCategory

object AppClassifier {

    private val communicationPackages = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "org.telegram.messenger", "org.telegram.plus",
        "com.twitter.android", "com.snapchat.android",
        "com.skype.raider", "com.google.android.apps.messaging",
        "com.facebook.orca", "com.facebook.mlite",
        "com.imo.android.imoim", "com.viber.voip",
        "com.linecorp.main", "com.signal",
        "com.discord", "com.slack",
        "com.microsoft.teams", "com.google.android.apps.meetings",
        "com.zoom.us", "com.tencent.mm",
        "com.tencent.mobileqq", "jp.naver.line.android"
    )

    private val socialPackages = setOf(
        "com.instagram.android", "com.facebook.katana",
        "com.facebook.lite", "com.twitter.android",
        "com.tiktok.trill", "com.zhiliaoapp.musically",
        "com.pinterest", "com.reddit.frontpage",
        "com.tumblr", "com.linkedin.android",
        "com.twitch", "com.miui.notes"
    )

    private val gamePackages = setOf(
        "com.tencent.ig", "com.dts.freefireth",
        "com.mobile.legends", "com.supercell.clashofclans",
        "com.supercell.brawlstars", "com.nianticlabs.pokemongo",
        "com.epicgames.fortnite", "com.microsoft.minecraft",
        "com.activision.callofduty", "com.garena.game.codm",
        "com.roblox.client", "com.king.candycrushsaga",
        "com.spotify.music"
    )

    private val mapsPackages = setOf(
        "com.google.android.apps.maps",
        "com.waze", "com.mapswithme.maps.pro",
        "com.sygic.aura", "com.here.app.maps",
        "com.tomtom.gplay.nav"
    )

    private val musicPackages = setOf(
        "com.spotify.music",
        "com.apple.music",
        "com.youtube.music",
        "com.soundcloud.android",
        "com.deezer.android",
        "com.shazam.android",
        "com.pandora.android"
    )

    private val videoPackages = setOf(
        "com.netflix.mediaclient",
        "com.google.android.youtube",
        "com.amazon.avod.thirdparty",
        "com.hulu.plus",
        "com.disney.disneyplus",
        "com.crunchyroll.crunchyroll",
        "com.hbo.hbonow",
        "com.primevideo"
    )

    private val benchmarkPackages = setOf(
        "com.antutu.ABenchMark", "com.antutu.benchmark.full",
        "com.futuremark.dmandroid.application",
        "com.primatelabs.geekbench", "com.primatelabs.geekbench5",
        "com.primatelabs.geekbench6",
        "com.workpoint.pcmark", "com.greenecomputing.a1sd",
        "com.bojsoftware.basemarkgpu",
        "com.antutu.atomic.gpustandard"
    )

    fun classify(pkgName: String): AppCategory {
        return when {
            pkgName in communicationPackages -> AppCategory.COMMUNICATION
            pkgName in socialPackages -> AppCategory.SOCIAL
            pkgName in gamePackages -> AppCategory.GAME
            pkgName in mapsPackages -> AppCategory.MAPS_NAV
            pkgName in musicPackages -> AppCategory.MUSIC
            pkgName in videoPackages -> AppCategory.VIDEO
            pkgName in benchmarkPackages -> AppCategory.BENCHMARK
            else -> AppCategory.OTHER
        }
    }

    fun getCategoryPackages(category: AppCategory): Set<String> {
        return when (category) {
            AppCategory.COMMUNICATION -> communicationPackages
            AppCategory.SOCIAL -> socialPackages
            AppCategory.GAME -> gamePackages
            AppCategory.MAPS_NAV -> mapsPackages
            AppCategory.MUSIC -> musicPackages
            AppCategory.VIDEO -> videoPackages
            AppCategory.BENCHMARK -> benchmarkPackages
            AppCategory.TOOLS -> emptySet()
            AppCategory.OTHER -> emptySet()
        }
    }
}
