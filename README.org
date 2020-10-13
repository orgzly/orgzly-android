#+BEGIN_HTML
<a title="Crowdin" target="_blank" href="https://crowdin.com/project/orgzly"><img src="https://d322cqt584bo4o.cloudfront.net/orgzly/localized.svg"></a>
#+END_HTML

* Orgzly

Orgzly is an outliner for taking notes and managing to-do lists.

You can keep notebooks stored in plain-text and have them synchronized
with a directory on your mobile device, SD card, WebDAV server or Dropbox.

Notebooks are saved in /Org mode/'s file format. “Org mode is for
keeping notes, maintaining TODO lists, planning projects, and
authoring documents with a fast and effective plain-text system.” See
http://orgmode.org for more information.

#+BEGIN_HTML
<a href="https://play.google.com/store/apps/details?id=com.orgzly">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play" height="70">
</a>
<a href="https://f-droid.org/app/com.orgzly">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="70">
</a>
#+END_HTML

** End-user documentation

Find out more here: https://github.com/orgzly/documentation

** Building & testing

If you don't use Android Studio and wish to [[https://developer.android.com/studio/build/building-cmdline.html][build]] and [[https://developer.android.com/studio/test/command-line.html][test]] the app
from command line, the standard set of Gradle tasks is available.  For
example:

- ~./gradlew build~ builds the project and generates APK files
- ~./gradlew connectedAndroidTest~ runs instrumented unit tests

Make sure you [[https://developer.android.com/training/testing/espresso/setup][turn off animations]] for the device you're testing on.

Java 8 is required for command line usage. Later versions are currently not compatible.

*** Dropbox

Dropbox integration, and associated tests, are disabled in the default configuration.
To enable, follow the instructions in sample.app.properties.

** License

The project is licensed under the [[https://github.com/orgzly/orgzly-android/blob/master/LICENSE][GNU General Public License version 3 (or newer)]].
