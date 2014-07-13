************
Contributing
************

If you want to contribute, you are more than welcome. There is only one thing you should keep in mind:

1. **Always write code in the development branch. The master branch is for package maintainers only. We will not accept pull requests based on the master branch.**

That's it. If you have no idea where to help out:

* check the issues list,
* write a translation,
* proof read code,
* proof read translations

Together we are be able to write a really good application for all of us.


How to submit bugs / issues
===========================
Bugs are nasty and we hate them as much as you do. Whenever you submit an issue, provide the following information. This is not to annoy you! Without that information, we don't know where to look:

Provide all details concerning your software environment
--------------------------------------------------------
* ownCloud server version: https://yourcloud/index.php/settings/admin
* ownCloud Notes server version
* ownCloud encryption plugin installed and enabled and if so, the version
* Android / CM / whateverdistroyouhave Version

Provide all details concerning the specific issue
-------------------------------------------------
* Update My Own Notes to latest version. If you are using F-Droid, that may take a day or two. Look at `our git repository`_ and browse the tags to see which version is the newest one.
* Enable extensive LogCat messages under "Settings/Debug" in MyOwnNotes.
* Submit the crash report on your phone. This way we can determine the problem in the google developers console (please be aware that you are sending the crash report to google).
* If you don't want to send data to google: send us the Output of `logcat`_
* Or look at /data/anr/traces.txt on your device and submit the crash log


How to submit translations
==========================

New Translations
----------------
To avoid incomplete translations, new ones will be added in a seperate branch. Please submit an issue and we will add this branch for you. Be aware that all new translations should be proof read by at least one other user before being merged into the development branch. If no one complains about the translation in a week's time, the pull request is treated as being ok and will be merged.

Improvements to existing translations
-------------------------------------
Just do your edits in the development branch.

Proof read translations
-----------------------
This is crucial. You really need to proof read your translations!

Critical Translations
---------------------
Translation requests (issues) blocking critical bugfixes will be marked as critical. If they are not provided 48 hours later, the release containing critical bugfixes will be distributed nevertheless. The translation request will be marked "critical, delayed" and should be treated with uttermost priority.


How to submit code
==================

1. Check
2. Double Check
3. Test
4. Test with a second device
5. Beautify
6. Repeat Steps 1-5 as often as necessary
7. `Submit`_
   
Notes regarding the workflow
----------------------------
* New entries in values/strings.xml have to be added in all other languages as well. That way, translators will be able to find new strings easily. Additionally, the code does not break if a string is forgotten.


License
=======
One last note: This application is licensed under `GPL 3`_. All code submitted will be released under the same license. For more information, look at the LICENSE file.

.. _GPL 3: http://www.gnu.org/copyleft/gpl.html
.. _logcat: http://wiki.cyanogenmod.org/w/Doc:_debugging_with_logcat
.. _network graph: https://github.com/aykit/myownnotes-android/network
.. _our git repository: https://github.com/aykit/myownnotes-android
.. _Submit: https://help.github.com/categories/63/articles
