# CalendarView

The calendar view with ability to mark and click dates. Recommended usage with ViewPager to display multiply months.
Also this package includes weekdays view and the abstract class to implement similar views.

## Usage

Execute this command under your project root:

    git submodule add https://github.com/KivApple/CalendarView.git calendarview

Add these lines to your Android project files:

`/settings.gradle`

    include ':app', ..., ':calendarview'

`/build.gradle`

    buildscript {
        ext.kotlin_version = '1.3.50'
        ext.appCompatVersion = '1.0.2'
        ext.jodaTimeVersion = '2.10.2'
        ...
    }

`/app/build.gradle`

    dependencies {
        ...
        implementation project(':calendarview')
        ...
    }
