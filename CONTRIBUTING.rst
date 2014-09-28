************
Contributing
************

If you want to contribute, you are more than welcome. There is only one thing you should keep in mind:

1. **Always write code in the development branch. The master branch is for package maintainers only. We will not accept pull requests based on the master branch.**
2. **!!! Your submissions will be GPL3. If you are not ok with this, don't submit your code !!!**

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


How to submit feature requests
==============================
New features are great, but feature requests can be annoying as hell. Here is how to write feature requests that may be fulfilled sometimes:

- Ask yourself if you really need this feature.
- Look into github. Has someone else submitted the same feature request? Please look at the closed issues as well!
- Ask yourself why you need this feature although no one else asked for it.
- Look at the app. Maybe the feature is already in there! If it's there but it took you too long to find it, please submit an issue. It means that our UX design is bad.
- Are you the only person in the world who may need this feature? E.g. "I invented a new format and My Own Notes should support it."
- We ignore every feature request not provided in Gherkin. See below for how to correctly submit Gherkin style feature requests.


Gherkin?
--------
Gerkhin is a language for application testing. When you submit your feature request in Gherkin, you make sure that your feature can be automatically tested when it arrives. Big plus for code quality!

Take a look at `Gherkin documentation`_ for how-to write it. Here is an `Real World Gherkin Example`_:

.. code-block:: cucumber

    Feature: Share with My Own Notes
	  Text in other applications should have a Share-With My Own notes option.
	  This creates a new note where the content of the new note is the text selected
	  in the application.

	  Scenario: Showing Share with My Own Notes option
	   Given that there is text selected in any application not being My Own Notes
	    When I press on the system wide Share-To Button
	    Then I see the option "Share with My Own Notes"

	  Scenario: Sharing text with My Own Notes
	   Given that there is text selected in any application not being My Own Notes
	     And Share-To has been pressed
	    When I select Share With My Own Notes
	    Then the application My Own Notes is opened
	     And a new note is created
	     And the selected text is the text of the new note


How to submit translations
==========================

New Translations
----------------
To avoid incomplete translations, new ones will be added in a seperate branch. Please submit an issue and we will add this branch for you. Be aware that all new translations should be proof read by at least one other user before being merged into the development branch. If no one complains about the translation in a week's time, the pull request is treated as being ok and will be merged.

Improving existing translations
-------------------------------
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

.. _Gherkin documentation: https://github.com/cucumber/cucumber/wiki/Gherkin
.. _GPL 3: http://www.gnu.org/copyleft/gpl.html
.. _logcat: http://wiki.cyanogenmod.org/w/Doc:_debugging_with_logcat
.. _network graph: https://github.com/aykit/myownnotes-android/network
.. _our git repository: https://github.com/aykit/myownnotes-android
.. _Real World Gherkin Example: https://github.com/aykit/myownnotes-android/issues/89
.. _Submit: https://help.github.com/categories/63/articles
