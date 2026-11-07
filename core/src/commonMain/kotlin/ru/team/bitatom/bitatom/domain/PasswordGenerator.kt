package ru.team.bitatom.bitatom.domain

/**
 * Генератор паролей (требования 10–12). Случайность берётся из C-ядра (CryptoProvider.randomInt),
 * как на диаграмме последовательности «генерация и сохранение».
 * Гарантирует хотя бы один символ из каждого выбранного набора.
 */
class PasswordGenerator(private val crypto: CryptoProvider) {

    fun generate(options: GeneratorOptions): String {
        require(options.validate()) { "Некорректные параметры генератора" }
        val sets = buildList {
            if (options.useLowercase) add(LOWER)
            if (options.useUppercase) add(UPPER)
            if (options.useDigits) add(DIGITS)
            if (options.useSymbols) add(SYMBOLS)
        }
        val all = sets.joinToString("")
        val chars = CharArray(options.length)
        sets.forEachIndexed { i, set -> chars[i] = set[crypto.randomInt(set.length)] }
        for (i in sets.size until chars.size) chars[i] = all[crypto.randomInt(all.length)]
        // Тасование Фишера–Йетса, чтобы обязательные символы не стояли в начале
        for (i in chars.size - 1 downTo 1) {
            val j = crypto.randomInt(i + 1)
            val t = chars[i]; chars[i] = chars[j]; chars[j] = t
        }
        return chars.concatToString()
    }

    companion object {
        const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#$%&*()_?/|\\-+=[]{}"
    }
}
