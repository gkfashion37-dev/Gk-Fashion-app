GK FASHION - Corrected Android WebView Project

Package: com.gkfashion.app
Website: https://gkfashion.in
Language: Java
Target SDK: 35

This corrected version keeps the website UI/functionality unchanged.
It includes:
- GK FASHION app logo
- GK FASHION splash/Express image
- Android system-bar inset handling for Android 15/16
- Splash waits for page readiness (with safety timeout)
- No WebView rubber/bounce
- WebView cookies/login/session support
- File chooser, camera/microphone WebView permission hook
- Downloads and external links
- Back button WebView history
- UPI/intent URL fallback handling
- Optional Razorpay WebView UPI Intent SDK initialization

IMPORTANT:
Razorpay's official WebView UPI Intent JAR is NOT bundled in this ZIP.
Download it from Razorpay's official UPI Intent documentation and place the JAR in:
app/libs/

Then put your Razorpay KEY ID (not Key Secret) in MainActivity.java:
RAZORPAY_KEY_ID = "YOUR_RAZORPAY_KEY_ID";

Do not put the Razorpay Key Secret in the APK.
