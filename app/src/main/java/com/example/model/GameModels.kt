package com.example.model

enum class GameType(
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val shortDesc: String
) {
    HIGHER_LOWER(
        title = "Більше чи Менше",
        subtitle = "Числова дуель на здогадливість",
        iconEmoji = "🎯",
        shortDesc = "Один загадує число, інший вгадує за підказками «Більше» чи «Менше»"
    ),
    GUESS_MIRACLE(
        title = "Вгадай чудо",
        subtitle = "Таємна людина або тварина",
        iconEmoji = "🦄",
        shortDesc = "Один вибирає людину або тварину та дає ім'я, а інший відгадує хто це"
    ),
    GOOD_MEMORY(
        title = "Гарна памʼять",
        subtitle = "0.5 секунди на запам'ятовування",
        iconEmoji = "⚡",
        shortDesc = "Запам'ятай 3 випадкові цифри, які з'являться рівно на 0.5 секунди!"
    ),
    BOOM_BOOM(
        title = "Ой-ой бум бум",
        subtitle = "Знешкодь бомбу за 1 хвилину!",
        iconEmoji = "💣",
        shortDesc = "Друг вибирає комбінацію поломки. У тебе 60 сек, а за кожну помилку -10 сек!"
    )
}

/**
 * 4 specific actions for «Ой-ой бум бум» as directly requested by the user:
 * 1. Торкнутися цифри 5
 * 2. Перерізати провод (Буде тільки один)
 * 3. Нажимати багато раз на бомбу
 * 4. Викинути бомбу
 */
enum class BombAction(
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    TOUCH_FIVE(
        title = "Торкнутися цифри 5",
        description = "Натиснути кнопку '5' на цифровій панелі бомби",
        iconEmoji = "5️⃣"
    ),
    CUT_WIRE(
        title = "Перерізати провод",
        description = "Перерізати єдиний провід живлення",
        iconEmoji = "✂️"
    ),
    TAP_MANY_TIMES(
        title = "Натискати багато раз на бомбу",
        description = "Швидко клікати на бомбу 10 разів",
        iconEmoji = "👆"
    ),
    THROW_BOMB(
        title = "Викинути бомбу",
        description = "Скинути бомбу у захисний люк безпеки",
        iconEmoji = "🚀"
    )
}

data class Player(
    val id: Int,
    val name: String,
    val avatar: String,
    val score: Int = 0
)
