package app.olauncher.data

object Constants {

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
        const val FOLDER = "folder"
    }

    /** Package marker used in a home slot that shows a folder (shortcutId holds the folder id). */
    const val FOLDER_PACKAGE = "app.olauncher.folder"

    object Dialog {
        const val ABOUT = "ABOUT"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
        const val HIDDEN = "HIDDEN"
        const val KEYBOARD = "KEYBOARD"
        const val PRO_MESSAGE = "PRO_MESSAGE"
    }

    object UserState {
        const val START = "START"
        // Persisted value of the removed daily-wallpaper prompt stage; kept only so that
        // installs still parked in it can be moved forward.
        const val LEGACY_WALLPAPER = "WALLPAPER"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
    }

    object DateTime {
        const val OFF = 0
        const val ON = 1
        const val DATE_ONLY = 2

        fun isTimeVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON
        }

        fun isDateVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
        }
    }

    object SwipeDownAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
    }

    object CharacterIndicator {
        const val SHOW = 102
        const val HIDE = 101
    }

    val CLOCK_APP_PACKAGES = arrayOf(
        "com.google.android.deskclock", //Google Clock
        "com.sec.android.app.clockpackage", //Samsung Clock
        "com.oneplus.deskclock", //OnePlus Clock
        "com.miui.clock", //Xiaomi Clock
    )


//    const val THEME_MODE_DARK = 0
//    const val THEME_MODE_LIGHT = 1
//    const val THEME_MODE_SYSTEM = 2

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_HOME_APP_1 = 1
    const val FLAG_SET_HOME_APP_2 = 2
    const val FLAG_SET_HOME_APP_3 = 3
    const val FLAG_SET_HOME_APP_4 = 4
    const val FLAG_SET_HOME_APP_5 = 5
    const val FLAG_SET_HOME_APP_6 = 6
    const val FLAG_SET_HOME_APP_7 = 7
    const val FLAG_SET_HOME_APP_8 = 8

    const val FLAG_SET_SWIPE_LEFT_APP = 11
    const val FLAG_SET_CLOCK_APP = 13
    const val FLAG_SET_CALENDAR_APP = 14
    const val FLAG_SET_DOCK_1 = 21
    const val FLAG_SET_DOCK_2 = 22
    const val FLAG_SET_DOCK_3 = 23

    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val HINT_RATE_US = 15

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 30f

    const val URL_ABOUT_OLAUNCHER = "https://tanujnotes.substack.com/p/olauncher-minimal-af-launcher?utm_source=olauncher"
    const val URL_OLAUNCHER_PRIVACY = "https://tanujnotes.notion.site/Olauncher-Privacy-Policy-dd6ac5101ddd4b3da9d27057889d44ab"
    const val URL_OLAUNCHER_GITHUB = "https://www.github.com/tanujnotes/Olauncher"
    const val URL_OLAUNCHER_PLAY_STORE = "https://play.google.com/store/apps/details?id=app.olauncher"
    const val URL_OLAUNCHER_PRO = "https://play.google.com/store/apps/details?id=app.prolauncher"
    const val URL_PLAY_STORE_DEV = "https://play.google.com/store/apps/dev?id=7198807840081074933"
    const val URL_TWITTER_TANUJ = "https://x.com/tanujnotes"
    const val URL_INSTA_TANUJ = "https://instagram.com/tanuj_notes"
    const val URL_NTS = "https://play.google.com/store/apps/details?id=com.makenotetoself"
    const val URL_PENTASTIC = "https://play.google.com/store/apps/details?id=app.pentastic"
    const val URL_DUCK_SEARCH = "https://duck.co/?q="

}