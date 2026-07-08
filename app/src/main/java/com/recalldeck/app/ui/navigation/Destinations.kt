package com.recalldeck.app.ui.navigation

/**
 * Navigation routes for the 9 screens.
 */
object Destinations {
    const val HOME = "home"
    const val SUBJECT_DETAIL = "subject/{subjectId}"
    const val CARD_BROWSER = "browser?subjectId={subjectId}&categoryId={categoryId}"
    const val CARD_EDITOR = "editor?cardId={cardId}&categoryId={categoryId}"
    const val STUDY_SETUP = "studySetup?subjectId={subjectId}&categoryId={categoryId}"
    const val STUDY = "study"
    const val IMPORT = "import"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun subjectDetail(subjectId: Long) = "subject/$subjectId"
    fun cardBrowser(subjectId: Long? = null, categoryId: Long? = null) =
        "browser?subjectId=${subjectId ?: -1}&categoryId=${categoryId ?: -1}"
    fun cardEditor(cardId: Long? = null, categoryId: Long? = null) =
        "editor?cardId=${cardId ?: -1}&categoryId=${categoryId ?: -1}"
    fun studySetup(subjectId: Long? = null, categoryId: Long? = null) =
        "studySetup?subjectId=${subjectId ?: -1}&categoryId=${categoryId ?: -1}"
}
