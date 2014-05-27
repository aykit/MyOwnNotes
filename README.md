MyOwnNotes-android
==================

Android App for the ownCloud Notes Application. Uses the (RESTful) API of ownCloud notes extension/app found here: https://github.com/owncloud/notes/wiki/API-0.2


### Install
to use this app you will need:

+ ownCloud server Version >= 6.0.3 (http://doc.owncloud.org/)
+ [ownCloud notes app](http://apps.owncloud.com/content/show.php/Notes?content=160567) Version >= 0.9 installed and activated on your server
+ Mobile device using Android Version >= 4.0
+ SSL certificate for your server. For more information please read the FAQ below.
+ This app


### FAQ

#### I can't connect. Do I really have to use SSL/TLS?
Short answer: yes. Please read our comment on [Entwicklerbier.org](https://blog.entwicklerbier.org/2014/05/securing-the-internet-of-things-how-about-securing-the-internet-first/) for more information.

#### I can't connect via SSL. How do I add (self-signed) certificates?
Use the android Security Settings to add self-signed certificates. Open your Settings, browse to Security and add them there. You can find more information on this feature at google dev: https://code.google.com/p/android/issues/detail?id=11231#c107

If you still need help, feel free to [contact](mailto:z-o48hohw4l9qla@ay.vc) us. Please be aware that this mailadress changes as we want to keep spam to a minimum and that it may take a while for us to help out.

#### Do you know any cheap SSL certificates?
If you don't want to use self-signed certificates you can get one for free (for non-profit purposes only) at [StartSSL](https://startssl.com). If you can spend money, we suggest you to take a look at [CheapSSLsecurity](https://cheapsslsecurity.com). Please note that we are not affiliated with those companies in any way. We just want to help you finding cheap certificates.


### Building the application
Download the official android sdk here: http://developer.android.com/sdk/index.html

+ Import "Android/Existing Android Code" into Workspace
+ Run "Android Application"
