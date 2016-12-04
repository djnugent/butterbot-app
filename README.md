# Butterbot-app
This is an android app that I made for the my [butterbot project](https://github.com/djnugent/butterbot). It connects to butterbot over wifi(tcp) and can control his arms, speech, treads, and led. It is self configuring and will auto discover butterbot as long as he is on the same wifi network. Below you'll find a brief overview of the UI functionality.

## Connecting Screen
This is the connecting screen the user is presented with while the app connects to butterbot. If butterbot is already online it will only appear for a brief moment, but it has a nice connecting animation if the user has to wait.

![ConnectingScreen](https://raw.githubusercontent.com/djnugent/butterbot-app/master/git-res/connect.gif)

## Splash Screen Transition
When the app connects to butterbot it transitions to the control page.

![SplashScreenTransition](https://raw.githubusercontent.com/djnugent/butterbot-app/master/git-res/transition.gif)

## Main screen Controls
The arms are controlled by the slider and his tank treads are controlled by the joystick. The joystick centers to the user's initial touch. This prevents any off center presses from causing undesired movement.

![MainScreenControls](https://raw.githubusercontent.com/djnugent/butterbot-app/master/git-res/control.gif)

## Play audio or do preprogrammed actions
This menu allow the user to play audio clips or play preprogrammed motion/audio clips.

![Audio](https://raw.githubusercontent.com/djnugent/butterbot-app/master/git-res/audio.gif)

## Enable Accelerometer neck control
This button enables neck control from phone tilt. The neck is super sensitive so the user is prompted before enabling neck control.

![Neck](https://raw.githubusercontent.com/djnugent/butterbot-app/master/git-res/neck.gif)
