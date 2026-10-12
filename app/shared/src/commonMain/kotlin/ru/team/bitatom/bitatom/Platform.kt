package ru.team.bitatom.bitatom

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform