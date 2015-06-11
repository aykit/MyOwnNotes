****
News
****
We are having a hackathon around My Own Notes 12. and 13. June, at OSCEdays Vienna 2015. So yes, there will be updates very very soon: http://oscedays.org/vienna/



******************
MyOwnNotes-android
******************
Android App for the ownCloud Notes Application. Uses the (RESTful) API of ownCloud notes extension/app found here: https://github.com/owncloud/notes/wiki/API-0.2


The right README
================
Make sure you read the README of the appropriate branch. The one you are seeing right now might not be the one you are looking for.


Contribute
==========
**Use the development branch** and please take a look at CONTRIBUTING.rst for information regarding extending this application.

The information there applies to **translations** and **issue submissions** as well.


Install
=======
to use this app you will need:

+ ownCloud server Version >= 6.0.3 see `ownCloud Docs`_
+ `ownCloud Notes App`_ Version >= 0.9 installed and activated on your server
+ Mobile device using Android Version >= 4.0
+ SSL certificate for your server. For more information please read the `FAQ`_ below.
+ The `My Own Notes App`_ 


.. _`FAQ`:

FAQ
===

What's it with "Installing ownCloud Notes app: No app name specified"?
----------------------------------------------------------------------
the app name as specified in info.xml of the notes app was not exactly the same as the directory name I was using for the app. This happens a lot when unzipping the file downloaded from github. Just rename notes-x.x to notes.

How do I get ownCloud?
----------------------
There are many resources on the internet, showing you how to run your own copy of ownCloud. The following article provides a quick roundup: https://blog.entwicklerbier.org/2014/06/setting-up-owncloud-on-speed/

I get a JSON error - what do I do?
----------------------------------
In ownCloud 7.0.2 the Notes App is not installed by default. You have to install and activate it. Try this and if you still get the error please submit a ticket.

I can't connect. Do I really have to use SSL/TLS?
-------------------------------------------------
Short answer: yes. Please read our comment on `Entwicklerbier.org`_ for more information.

I can't connect via SSL. How do I add (self-signed) certificates?
-----------------------------------------------------------------
Use the android Security Settings to add self-signed certificates. Open your Settings, browse to Security and add them there. You can find more information on our `My Own Notes Website`_ and at `google dev`_.

If you still need help, feel free to `contact us`_. Please be aware that this mailadress changes as we want to keep spam to a minimum and that it may take a while for us to help out.

I heard you don't support ownCloud's encryption app. Is this true?
------------------------------------------------------------------
Since ownCloud 7.0.4, MyOwnNotes is working with the encryption plugin enabled. However, please look at `our blog entry about ownCloud's encryption app`_ and at `our comment in issue #9392`_ 

Do you know any cheap SSL certificates?
---------------------------------------
If you don't want to use self-signed certificates you can get one for free (for non-profit purposes only) at `StartSSL`_. If you can spend money, we suggest you to take a look at `CheapSSLsecurity`_. Please note that we are not affiliated with those companies in any way. We just want to help you finding cheap certificates.

I have my own certificate. However, it is not working.
------------------------------------------------------
One of the most common errors is a wrong certificate chain. Please use `SSL Labs`_ to check if your certificate chain is in order. If it isn't look up the manual of your webserver. We also wrote an article for `Setting up owncloud on Speed`_.

I want to move my certificate to a custom folder
------------------------------------------------
Moving certificates can be tedious and insecure, depending on your selinux settings. We do not recommend you to move your certificates. If you really must, github user `eephyne`_ documented a way to achieve this: `Moving Certificates`_
Oh, and by the way: in contrary to others, we think it's really stupid to disable selinux. Only disable for testing purposes.

Building the application
------------------------
Download the official android sdk here: http://developer.android.com/sdk/index.html

+ Import "Android/Existing Android Code" into Workspace
+ Run "Android Application"


Contributors
============

Here is  a list of all contributers, including ourselves. A big thank you to all the people who help developing this application. Please be aware that all contributions are GPL3 licensed.

Maintainer
----------
* `aykit`_ : Non-profit organisation supporting art, culture and science

Developers
----------
* Main Developer: `steppenhahn`_ 

Translators
-----------
* French: `flo1`_ , `gityeti`_ 
* Serbian: `pejakm`_ 
* Spanish: `tmelikoff`_ 
* Turkish: `wakeup`_ 

Testers
-------
Unfortunately, we are not able to greet everyone in person. Without your feedback, we wouldn't be able to improve My Own Notes. Please keep up testing and providing valuable information regarding your issues. We promise we will keep up fixing and improving as best as we can.


License
=======
My Own Notes and all contributions are licensed as `GPL3`_ 

.. _CheapSSLsecurity: https://cheapsslsecurity.com
.. _contact us: mailto:z-o48hohw4l9qla@ay.vc
.. _Entwicklerbier.org: https://blog.entwicklerbier.org/2014/05/securing-the-internet-of-things-how-about-securing-the-internet-first/
.. _google dev: https://code.google.com/p/android/issues/detail?id=11231#c107
.. _GPL3: https://github.com/aykit/myownnotes-android/blob/master/LICENSE
.. _Moving Certificates: https://github.com/aykit/myownnotes-android/issues/72
.. _My Own Notes App: https://github.com/aykit/myownnotes-android
.. _My Own Notes Website: https://aykit.org/sites/myownnotes.html
.. _our blog entry about ownCloud's encryption app: https://blog.entwicklerbier.org/2014/09/misconceptions-of-owncloud-encryption/
.. _our comment in issue #9392: https://github.com/owncloud/core/issues/9392#issuecomment-56274074
.. _ownCloud Docs: http://doc.owncloud.org/
.. _ownCloud Notes App: https://github.com/owncloud/notes
.. _SSL Labs: https://www.ssllabs.com/ssltest/
.. _StartSSL: https://startssl.com
.. _Setting up owncloud on Speed: https://blog.entwicklerbier.org/2014/06/setting-up-owncloud-on-speed/

.. _aykit: https://aykit.org
.. _eephyne: https://github.com/eephyne
.. _flo1: http:// https://github.com/flo1
.. _gityeti: https://github.com/gityeti
.. _pejakm: https://github.com/pejakm
.. _steppenhahn: https://github.com/steppenhahn
.. _tmelikoff: http://https://github.com/tmelikoff
.. _wakeup: https://github.com/wakeup
