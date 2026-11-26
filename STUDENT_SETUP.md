# RoosterBotsWV FTC Student Setup Guide

Set up a Windows computer to build the team’s robot code and share changes from the workshop or from home. You can do the setup and build with the robot off. The final test uses the robot at the workshop.

## 1. Install Android Studio

Download Android Studio for Windows and run the installer. In the Setup Wizard, choose **Standard**, accept the SDK licenses, and let the downloads finish. If Android Studio asks you to sign in, choose **Skip**. [Download Android Studio](https://developer.android.com/studio)

## 2. Clone the team project

A clone is your own local copy of the project, connected to the team’s GitHub repository.

1. On the **Welcome to Android Studio** screen, click **Clone Repository**.
2. Select **Repository URL** and paste this address into the **URL** field.

   ```text
   https://github.com/RoosterBotsWV/ftc-roosterbots-code.git
   ```

3. Keep the default local directory under **StudioProjects** on your `C:` drive. Leave **Shallow clone** unchecked.
4. If Android Studio says **Git is not installed**, click **Download and Install**. When **Clone** becomes active, click it.

## 3. Open and sync the project

When **Trust and Open Project** appears, make sure the project is `ftc-roosterbots-code`. Clear both checkboxes, then click **Trust Project**.

If Android Studio asks you to select a Gradle JVM, click **Use JVM 21**. If it asks for the Android SDK platform, install **Android 11 (API 30)**. Keep the laptop online while Gradle downloads dependencies and imports the project. You can dismiss the optional Gradle upgrade and migration notices.

The import is ready when **FtcRobotController** and **TeamCode** appear in the left panel and the progress bar clears. [Project build settings](https://github.com/RoosterBotsWV/ftc-roosterbots-code/blob/ProofOfConcept/build.common.gradle)

## 4. Check that the code builds

Click the **☰** menu at the top left, then choose **Build → Assemble Project**. Look for **BUILD SUCCESSFUL** in the **Build** panel. Click the hammer icon on the left edge if the panel is hidden. If the build fails, send the first error message to a mentor. [Android Studio build menu](https://developer.android.com/build/build-for-release)

## 5. Share your changes with the team

Use your own GitHub account. Ask a mentor for write access to the team repository before your first push.

1. Start on **ProofOfConcept**. Click the branch name at the top of Android Studio and select **Update Project…** to get the latest changes.
2. From the same menu, choose **New Branch…**. Give your branch a short name that describes your change. Keep **Checkout branch** checked and click **Create**.
3. Make your change and build the project. Most team Java code is in **TeamCode**.
4. Open the branch menu and choose **Commit…**. Select the files you changed, write a short message describing the change, and commit. Then choose **Push…** to send your branch to GitHub.
5. Open a pull request on GitHub so your teammates can review it. Choose **RoosterBotsWV/ftc-roosterbots-code** as the base repository and **ProofOfConcept** as the base branch. [GitHub pull request guide](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests)

## 6. Test on the robot at the workshop

Work with a mentor for your first install. Check **⋮ → About** in **FTC Driver Station** on the handheld. This project builds Robot Controller app version **11.0**. FIRST recommends matching the major and minor versions of the handheld and robot apps. [FTC app compatibility](https://ftc-docs.firstinspires.org/en/latest/ftc_sdk/updating/index.html)

Secure the robot with its drive wheels off the ground. Power it on and connect the laptop to the Control Hub using a **USB-C data cable**. In Android Studio, select **TeamCode** and the **Control Hub**, then click **Run**.

On the handheld, connect to the robot’s Wi-Fi and check for a ping time and robot battery voltage. With your mentor, select the team’s **TeleOp**, test it briefly, then press **Stop**. [FIRST’s installation and run guide](https://ftc-docs.firstinspires.org/en/latest/programming_resources/tutorial_specific/android_studio/creating_op_modes/Creating-and-Running-an-Op-Mode-%28Android-Studio%29.html)
