******************
MyOwnNotes-android
******************
Android App for the ownCloud Notes Application. Uses the (RESTful) API of ownCloud notes extension/app found here: https://github.com/owncloud/notes/wiki/API-0.2


Contribute
==========
Please take a look at CONTRIBUTE.rst for information regarding extending this application.

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

I can't connect. Do I really have to use SSL/TLS?
-------------------------------------------------
Short answer: yes. Please read our comment on `Entwicklerbier.org`_ for more information.

I can't connect via SSL. How do I add (self-signed) certificates?
-----------------------------------------------------------------
Use the android Security Settings to add self-signed certificates. Open your Settings, browse to Security and add them there. You can find more information on this feature at `google dev`_.

If you still need help, feel free to `contact us`_. Please be aware that this mailadress changes as we want to keep spam to a minimum and that it may take a while for us to help out.

Do you know any cheap SSL certificates?
---------------------------------------
If you don't want to use self-signed certificates you can get one for free (for non-profit purposes only) at `StartSSL`_. If you can spend money, we suggest you to take a look at `CheapSSLsecurity`_. Please note that we are not affiliated with those companies in any way. We just want to help you finding cheap certificates.


Building the application
------------------------
Download the official android sdk here: http://developer.android.com/sdk/index.html

+ Import "Android/Existing Android Code" into Workspace
+ Run "Android Application"




.. _CheapSSLsecurity: https://cheapsslsecurity.com
.. _contact us: mailto:z-o48hohw4l9qla@ay.vc
.. _Entwicklerbier.org: https://blog.entwicklerbier.org/2014/05/securing-the-internet-of-things-how-about-securing-the-internet-first/
.. _google dev: https://code.google.com/p/android/issues/detail?id=11231#c107
.. _My Own Notes App: https://github.com/aykit/myownnotes-android
.. _ownCloud Docs: http://doc.owncloud.org/
.. _ownCloud Notes App: http://apps.owncloud.com/content/show.php/Notes?content=160567
.. _StartSSL: https://startssl.com
