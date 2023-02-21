# 0. DJI_GSDemo

This program is implemented based on DJI_GSDemo
[Link : DJI_GSDemo](https://github.com/DJI-Mobile-SDK-Tutorials/Android-GSDemo-GoogleMap)

<br>
<br>

# 1. Dependency

## Device and S/W

| Device and S/W   | Product                     |                          Version |
| :--------------- | --------------------------- | -------------------------------: |
| Drone            | DJI AIR 2s                  |                      v02.04.2500 |
| Drone Controller | DJI RC-N1                   |                      v04.12.0100 |
| Simulator        | DJI assistant 2 for windows | v2.1.15 (Consumer Drones Series) |
| Computer         | Windows                     |    v22H2 (OS build : 19045.2486) |
| Android Device   | Samsung Galaxy S20+         |                  >= Android 12.0 |
| Android studio   | Android Studio Electric Eel |                 2022.1.1 Patch 1 |
| Android version  | Mobile SDK                  |                   Android 4.16.1 |

<br>

## Maven package

| Package              | Version |
| :------------------- | ------: |
| multidex             |   2.0.1 |
| otto                 |   1.3.8 |
| dji-sdk              |  4.16.1 |
| dji-sdk-provided     |  4.16.1 |
| opencsv              |   5.7.1 |
| appcompat            |   1.2.0 |
| core                 |   1.3.2 |
| constraintlayout     |   2.0.4 |
| recyclerview         |   1.1.0 |
| lifecycle-extensions |   2.2.0 |
| annotation           |   1.2.0 |
| material             |   1.2.1 |
| play-services        |  10.2.1 |
| play-services-ads    |  10.2.1 |
| play-services-auth   |  10.2.1 |
| play-services-gcm    |  10.2.1 |
| butterknife          |  10.1.0 |
| butterknife-compiler |  10.1.0 |

- It includes Google maps, Android DJI SDK.

<br>
<br>

# 2. User Maunal

## before setting.

- DJI developer , GoogleMaps API key need to be issued. In here, it is my key that is initialized.

<br>

## 0. Setting environment.

1. Build a new project following this settings. (Empty project doesn't matter. Copy paste other files)
   ![](https://velog.velcdn.com/images/jsin2475/post/dd4404c1-e17b-4069-a2f2-40d008e0bb6c/image.png)

<br>

2. Copy & paste the floder "app", "gradle" and file "build.gradle", "gradle.properties", "gradlew", "gradlew.bat", "settings.gradle"

<br>

3. duplicated file issue. -> delete the file
   ![](https://velog.velcdn.com/images/jsin2475/post/1dd82295-23bd-4338-8b86-77c52c449538/image.png)

<br>

4. dependency issue when gradle is synced => add `android:exported="true"` in their referenced manifest (In "merged manifest" category).
   ![](https://velog.velcdn.com/images/jsin2475/post/b55c7f6e-6d8e-4a2e-b416-e72bfd732ce0/image.png)
   ![](https://velog.velcdn.com/images/jsin2475/post/02f5a671-a6a6-4ba3-80f7-3c11c3c5c080/image.png)
   ![](https://velog.velcdn.com/images/jsin2475/post/d4f71127-a68e-4955-879e-ffa6b4d713ed/image.png)

<br>

## 1. Making a dataset with simulator

1. Connect your drone with your computer to turn on the simulator.
   <br>Connect your remote controller of drone with your android phone to register your DJI SDK.
   ![](https://velog.velcdn.com/images/jsin2475/post/a27935a3-72e0-4512-84f9-67f6fc75413d/image.png)<br>![](https://velog.velcdn.com/images/jsin2475/post/3edf1dfa-0cc6-427f-b3a6-33b06e7dc188/image.png)

<br>

2. Click button "Open", and Click button "Waypoint 1.0".<br>There is a 2 way to make a data set.
   <br> - Leave a pinpoint in a application maually(by hand).
   <br> - Leave a pinpoint with a `ArrayList<LatLng> testPos`<br>![](https://velog.velcdn.com/images/jsin2475/post/a1946359-8054-42e8-bd9a-716986d9e2a8/image.png)

<br>

3. When simulator run with "Start", the "locate" button shows where drone exists as the coordinates.
   ![](https://velog.velcdn.com/images/jsin2475/post/4ebbb071-dc92-4510-9939-1e86b3041cae/image.png)<br>![](https://velog.velcdn.com/images/jsin2475/post/bf990083-53d8-4dab-8da8-6e840cf9eaa9/image.png)

<br>

4. After "locate", drone takes off when "TAKE OFF" clicked and start flying when "BELOG" is clicked or manually click the map!
   ![](https://velog.velcdn.com/images/jsin2475/post/07d7e47d-6a3e-4f89-9c34-93608996baa6/image.png)<br>![](https://velog.velcdn.com/images/jsin2475/post/9d5a14b6-1166-434c-9581-a9c02648f42e/image.png)<br>![](https://velog.velcdn.com/images/jsin2475/post/4b9e007b-9942-4569-b0f1-9c0fcf3e7067/image.png)

<br>

5. After flying, the log is saved in internal memory inside of "uavData" folder.<br>Every after flight, the "fileNum" value in the file "Waypoint1Activity" need to be changed or it will replace the old data as a new one.<br>![](https://velog.velcdn.com/images/jsin2475/post/41ae45b6-2622-4e73-954f-caab1e1bd4d9/image.png)
