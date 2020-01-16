Drone Concert Photographer Android app
======================================


-----------------------
EPFL Course: EE-490g – Lab On Apps Development For Tablets, Smartphones And Smartwatches

-----------------------
Students: Bourban Emile (249833), Guinchard Anthony (237689), Houeix Acid Yann (314046)

-----------------------
Project supervisors: Najibi Halima, Sopic Dionisije



-----------------------
Instructions:

First of all, the drone needs to be calibrated thanks to the default Parrot app "FreeFlight Pro". If we had more time and if it were explicitly required in the project description, we would have tried to implement this functionality by ourselves.

At launch, the first step is to discover the drone that is going to be used. For that, the user can open his "Wifi Settings" directly from a menu icon. Once connected to the drone wifi, the user is then invited to click on the button "Discover Drone" in order to pair the drone with the app.

Once the user has selected the Bebop Drone, the app opens and the watch automatically connects to the tablet if the user opens the app on the watch as well. The watch then automatically transmits acceleration measurements and GPS locations to the tablet. Make sure that "Location" is turned off on the tablet, otherwise, the tablet won't get the GPS location of the watch! After the connection step, the user can take off and control the drone in order to bring it at a reasonable distance from the artist. Once in place, the user pushes either the button “Yaw Auto” or “Proximity Auto” to enable the autonomous modes. The former activates the tracking of the artist (i.e. it automatically orients towards him as long as the distance between the drone and the artist wearing the watch is sufficiently large, for example more than 7 meters) and the latter enables the attractive/repulsive behaviour of the drone based on the activity of the performer.  This “proximity” behaviour is active as long as the distance between the drone and the watch is situated between 7 and 20 meters, otherwise the drone stops. Be aware that these autonomous modes rely strongly on a good GPS reception.

The user can optionally select a path for his drone. The drone can either shift along a horizontal line (Path 1), a vertical line (Path 2) or along a square (Path 3). For each path, the user can enter the number of cycles he wants the drone to run. By default this number of cycles is set to 2. The button "Exit Path" allows the user to exit a path and makes the drone hover at any time. For some artistic reasons, it is possible to combine several paths. The commands sent to the drone will then simply superimpose. For example, it is possible to make the drone moving along a diagonal axis by pressing once on the button "Path 1" and "Path 2" successively.

The main purpose of the app is finally to record the performance of the artist. At any time, the user can simply choose to take either a picture, a video or a timelapse. The time interval for the timelapse has to be set by hand and its unit is in second. By default, the time interval is 0 second, which simply corresponds the record of a continuous standard video.

The Bebop drone 2 has an autonomy of around 25 minutes. Once the user has finished recording his media, he can regain control of the drone by quitting the autonomous mode (simply by clicking on the button "Autonomous Mode") and drive the drone in a safe area for landing.

In a second time, the user can retrieve his media by connecting the drone to his computer using a micro usb cable.



Remarks:
- At any time, the user can control the drone (even during the execution of paths and the autonomous mode). The commands sent to the drone will simply superimpose to the programmed ones. If the drone receives two simultaneous contradictory commands (e.g. forward and backward) the commands will cancel out.

- The debuggable version of our app to test the measurements received from the watch is accessible by clicking on the button appearing after clicking on the "Info" menu item of the InitialActivity. This is useful to check whether problems come from a bad GPS connection for instance.

- Following video illustrates the working of our app and some of the test we made:
https://www.youtube.com/watch?v=28YuKKXdjgQ&feature=youtu.be


-----------------------
App Icon sources:

LEVIE, Hallie, 2019.09.03. How to Hear Better at Concerts. Consumer Reports [online]. 2020. [Consulted on the 5th of January 2020]. Available at: https://www.consumerreports.org/hearing-ear-care/hear-better-at-concerts/

Drone Effect [online]. 2017. [Consulted on the 5th of January 2020]. Available at: http://www.adresse.du/site
https://droneeffect.fr


-----------------------
Drone Interface Source:

We based our interface on the one made by Brian Rozmierski available on Github:
https://github.com/bdaroz/HVCC-ParrotDrone
