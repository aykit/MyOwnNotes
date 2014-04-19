MyOwnNotes-android
==================

Android implementation of ownCloud Notes Application. Uses the (RESTful) API of ownCloud notes extension/app found here: https://github.com/owncloud/notes/wiki/API-0.2 

### Install
to use this app you will need:

+ ownCloud server version 6 (http://doc.owncloud.org/)
+ the [ownCloud-"App Framework"](http://doc.owncloud.org/server/6.0/admin_manual/configuration/configuration_apps.html) activated.
+ the serverside [ownCloud notes app](http://apps.owncloud.com/content/show.php/Notes?content=160567) installed and activated.
+ Android 4.0 or newer installed
+ this app

### FAQ
#### I can't connect via SSL. How do I add a (self-signed) certificate?
Use the android Security Settings to add self-signed certificates. Open your Settings, browse to Security and add them there. You can find more information on this feature at google dev: https://code.google.com/p/android/issues/detail?id=11231#c107


### Building the application
build using official android sdk.
get it here: http://developer.android.com/sdk/index.html

+ import as Android/Existing Android Code into Workspace
+ then run as "Android Application"


