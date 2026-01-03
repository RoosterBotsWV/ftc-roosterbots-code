package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@TeleOp(name="RoosterBots - Headlessmode_upper_blue", group="LinearOpMode")
public class headlessmode_upper_blue extends LinearOpMode {

    // --- Time and Drive Train ---
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotorEx frontLeftDrive = null;
    private DcMotorEx frontRightDrive = null;
    private DcMotorEx backLeftDrive = null;
    private DcMotorEx backRightDrive = null;
    private IMU imu = null;

    // --- Mechanisms ---
    private DcMotorEx flywheelMotor1 = null;
    private DcMotorEx flywheelMotor2 = null;
    private Servo pusherServo = null;

    // --- Control Variables ---
    private double drivePowerMultiplier = 1.0;
    private double maxFlywheelRpm = 180;
    
    private boolean flywheelIsRunning = false;
    private boolean flywheelIsReversed = false;

    // --- Button Press Detection ---
    private boolean dpadDownPressedLast = false;
    private boolean dpadUpPressedLast = false;
    private boolean dpadLeftPressedLast = false;
    private boolean dpadRightPressedLast = false;
    private boolean pusherButtonPressedLast = false;
    private boolean leftBumperPressedLast = false;
    private boolean rightBumperPressedLast = false;
    private boolean yPressedLast = false;
    private boolean backPressedLast = false;

    // --- Automated Protocol (Macro) Settings ---
    private enum AutoMoveState { 
        IDLE, MOVING_BACKWARDS, PREPARING_FLYWHEEL, FIRING, 
        WAIT_FOR_PUSHER, WAITING_BETWEEN_SHOTS, TURNING_LEFT, MOVING_FINAL_FORWARD 
    };
    private AutoMoveState autoMoveState = AutoMoveState.IDLE;
    private ElapsedTime autoMoveTimer = new ElapsedTime();
    private int shotCount = 0;
    private double savedMaxRpm = 225;
    private boolean savedFlywheelRunning = false;

    // *** TUNE THESE DISTANCES ***
    private static final double AUTO_DISTANCE_BACKWARD_FT = 12.0;
    private static final double AUTO_TURN_DEGREES = 90.0; 
    private static final double AUTO_DISTANCE_FINAL_FT = 6.0; // "A few feet"
    private static final double AUTO_FIRE_RPM = 150.0;
    private static final double AUTO_DRIVE_POWER = 0.25;
    private static final double AUTO_SHOT_WAIT_MS = 1500; 

    // *** ENCODER CALIBRATION (Used for forward/backward travel) ***
    private static final double TICKS_PER_REV = 28; 
    private static final double DRIVE_GEAR_RATIO = 5.2; 
    private static final double WHEEL_DIAMETER_CM = 10.0;
    private static final double CM_PER_FOOT = 30.48;
    
    private static final double TICKS_PER_FOOT = ((TICKS_PER_REV * DRIVE_GEAR_RATIO) / (WHEEL_DIAMETER_CM * Math.PI)) * CM_PER_FOOT;

    private ElapsedTime pusherServoTimer = new ElapsedTime();
    private enum PusherServoState { IDLE, PUSHING, PAUSE_AFTER_PUSH, RETURNING };
    private PusherServoState pusherServoState = PusherServoState.IDLE;
    private static final double PUSHER_SERVO_FORWARD_TIME_MS = 1000;
    private static final double PUSHER_SERVO_PAUSE_TIME_MS = 50;

    private static final double FLYWHEEL_MOTOR_TICKS_PER_REV = 560;
    private static final double MAX_POSSIBLE_RPM = 5000; 
    private static final double RPM_ADJUST_INCREMENT = 10;

    @Override
    public void runOpMode() {

        // --- Hardware Initialization ---
        frontLeftDrive = hardwareMap.get(DcMotorEx.class, "front_left_drive");
        frontRightDrive = hardwareMap.get(DcMotorEx.class, "front_right_drive");
        backLeftDrive = hardwareMap.get(DcMotorEx.class, "back_left_drive");
        backRightDrive = hardwareMap.get(DcMotorEx.class, "back_right_drive");

        imu = hardwareMap.get(IMU.class, "imu");
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD));
        imu.initialize(parameters);

        frontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        frontLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        flywheelMotor1 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_1");
        flywheelMotor2 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_2");
        pusherServo = hardwareMap.get(Servo.class, "pusher_servo");

        flywheelMotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheelMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheelMotor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheelMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheelMotor2.setDirection(DcMotor.Direction.REVERSE);

        pusherServo.setPosition(0.5);

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        telemetry.setMsTransmissionInterval(50);
        telemetry.addData("Status", "Initialized and Ready");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            boolean currentDpadUp = gamepad1.dpad_up;
            boolean currentDpadDown = gamepad1.dpad_down;
            boolean currentDpadLeft = gamepad1.dpad_left;
            boolean currentDpadRight = gamepad1.dpad_right;
            boolean currentX = gamepad1.x;
            boolean currentY = gamepad1.y;
            boolean currentLB = gamepad1.left_bumper;
            boolean currentRB = gamepad1.right_bumper;
            boolean currentBack = gamepad1.back;

            if (currentBack && !backPressedLast) {
                imu.resetYaw();
            }

            if (currentY && !yPressedLast && autoMoveState == AutoMoveState.IDLE) {
                savedMaxRpm = maxFlywheelRpm;
                savedFlywheelRunning = flywheelIsRunning;
                maxFlywheelRpm = AUTO_FIRE_RPM;
                flywheelIsRunning = true;
                flywheelIsReversed = false;
                
                imu.resetYaw(); // Reset heading to zero for the start of the sequence
                resetDriveEncoders();
                autoMoveState = AutoMoveState.MOVING_BACKWARDS;
            }

            if (autoMoveState != AutoMoveState.IDLE) {
                handleAutoMove();
            } else {
                double axial   = -gamepad1.left_stick_y;
                double lateral =  gamepad1.left_stick_x;
                double yaw     =  gamepad1.right_stick_x;

                axial   = Math.pow(axial, 3);
                lateral = Math.pow(lateral, 3);
                yaw     = Math.pow(yaw, 3);

                double botHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
                double rotX = lateral * Math.cos(-botHeading) - axial * Math.sin(-botHeading);
                double rotY = lateral * Math.sin(-botHeading) + axial * Math.cos(-botHeading);

                double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(yaw), 1.0);
                double fl = (rotY + rotX + yaw) / denominator;
                double bl = (rotY - rotX + yaw) / denominator;
                double fr = (rotY - rotX - yaw) / denominator;
                double br = (rotY + rotX - yaw) / denominator;

                if (currentDpadLeft && !dpadLeftPressedLast) drivePowerMultiplier = Math.min(1.0, drivePowerMultiplier + 0.10);
                if (currentDpadRight && !dpadRightPressedLast) drivePowerMultiplier = Math.max(0.0, drivePowerMultiplier - 0.10);

                frontLeftDrive.setPower(fl * drivePowerMultiplier);
                backLeftDrive.setPower(bl * drivePowerMultiplier);
                frontRightDrive.setPower(fr * drivePowerMultiplier);
                backRightDrive.setPower(br * drivePowerMultiplier);

                if (currentDpadUp && !dpadUpPressedLast) maxFlywheelRpm = Math.min(MAX_POSSIBLE_RPM, maxFlywheelRpm + RPM_ADJUST_INCREMENT);
                if (currentDpadDown && !dpadDownPressedLast) maxFlywheelRpm = Math.max(0, maxFlywheelRpm - RPM_ADJUST_INCREMENT);
                if (currentLB && !leftBumperPressedLast) {
                    if (flywheelIsRunning && !flywheelIsReversed) flywheelIsRunning = false;
                    else { flywheelIsRunning = true; flywheelIsReversed = false; }
                }
                if (currentRB && !rightBumperPressedLast) {
                    if (flywheelIsRunning && flywheelIsReversed) flywheelIsRunning = false;
                    else { flywheelIsRunning = true; flywheelIsReversed = true; }
                }
            }

            double targetFlywheelRpm = flywheelIsRunning ? (flywheelIsReversed ? -maxFlywheelRpm : maxFlywheelRpm) : 0.0;
            double flywheelVelocity = (targetFlywheelRpm / 60.0) * FLYWHEEL_MOTOR_TICKS_PER_REV;
            flywheelMotor1.setVelocity(flywheelVelocity);
            flywheelMotor2.setVelocity(flywheelVelocity);

            switch (pusherServoState) {
                case IDLE:
                    if (autoMoveState == AutoMoveState.IDLE && currentX && !pusherButtonPressedLast) {
                        pusherServoState = PusherServoState.PUSHING; pusherServoTimer.reset();
                    } else pusherServo.setPosition(0.5);
                    break;
                case PUSHING:
                    pusherServo.setPosition(1.0);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_FORWARD_TIME_MS) {
                        pusherServoState = PusherServoState.PAUSE_AFTER_PUSH; pusherServoTimer.reset();
                    }
                    break;
                case PAUSE_AFTER_PUSH:
                    pusherServo.setPosition(0.5);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_PAUSE_TIME_MS) {
                        pusherServoState = PusherServoState.RETURNING; pusherServoTimer.reset();
                    }
                    break;
                case RETURNING:
                    pusherServo.setPosition(0.0);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_FORWARD_TIME_MS) pusherServoState = PusherServoState.IDLE;
                    break;
            }

            dpadDownPressedLast = currentDpadDown; dpadUpPressedLast = currentDpadUp;
            dpadLeftPressedLast = currentDpadLeft; dpadRightPressedLast = currentDpadRight;
            pusherButtonPressedLast = currentX; leftBumperPressedLast = currentLB;
            rightBumperPressedLast = currentRB; yPressedLast = currentY;
            backPressedLast = currentBack;

            telemetry.addData("Heading", "%.1f Deg", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
            telemetry.addData("Auto State", autoMoveState.toString());
            telemetry.addData("FLYWHEEL SETTING", "%.0f RPM", maxFlywheelRpm);
            telemetry.addData("Status", "Headless Mode Active");
            telemetry.update();
        }
    }

    private void handleAutoMove() {
        switch (autoMoveState) {
            case MOVING_BACKWARDS:
                double targetTicksBack = AUTO_DISTANCE_BACKWARD_FT * TICKS_PER_FOOT;
                setDrivePower(-AUTO_DRIVE_POWER, 0, 0); // Negative power for backwards
                if (Math.abs(getAverageDrivePosition()) >= targetTicksBack) {
                    setDrivePower(0, 0, 0);
                    autoMoveTimer.reset();
                    autoMoveState = AutoMoveState.PREPARING_FLYWHEEL;
                }
                break;

            case PREPARING_FLYWHEEL:
                if (autoMoveTimer.milliseconds() >= 1500) {
                    shotCount = 0;
                    autoMoveState = AutoMoveState.FIRING;
                }
                break;

            case FIRING:
                pusherServoState = PusherServoState.PUSHING;
                pusherServoTimer.reset();
                autoMoveState = AutoMoveState.WAIT_FOR_PUSHER;
                break;

            case WAIT_FOR_PUSHER:
                if (pusherServoState == PusherServoState.IDLE) {
                    shotCount++;
                    if (shotCount < 3) {
                        autoMoveTimer.reset();
                        autoMoveState = AutoMoveState.WAITING_BETWEEN_SHOTS;
                    } else {
                        resetDriveEncoders();
                        autoMoveState = AutoMoveState.TURNING_LEFT;
                    }
                }
                break;

            case WAITING_BETWEEN_SHOTS:
                if (autoMoveTimer.milliseconds() >= AUTO_SHOT_WAIT_MS) {
                    autoMoveState = AutoMoveState.FIRING;
                }
                break;

            case TURNING_LEFT:
                double currentHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
                setDrivePower(0, 0, -AUTO_DRIVE_POWER); // Turn left
                if (Math.abs(currentHeading) >= AUTO_TURN_DEGREES) {
                    setDrivePower(0, 0, 0);
                    resetDriveEncoders();
                    autoMoveState = AutoMoveState.MOVING_FINAL_FORWARD;
                }
                break;

            case MOVING_FINAL_FORWARD:
                double targetTicksForward = AUTO_DISTANCE_FINAL_FT * TICKS_PER_FOOT;
                setDrivePower(AUTO_DRIVE_POWER, 0, 0);
                if (Math.abs(getAverageDrivePosition()) >= targetTicksForward) {
                    setDrivePower(0, 0, 0);
                    maxFlywheelRpm = savedMaxRpm;
                    flywheelIsRunning = savedFlywheelRunning;
                    autoMoveState = AutoMoveState.IDLE;
                }
                break;
        }
    }

    private void resetDriveEncoders() {
        frontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private double getAverageDrivePosition() {
        return (Math.abs(frontLeftDrive.getCurrentPosition()) + Math.abs(frontRightDrive.getCurrentPosition()) + 
                Math.abs(backLeftDrive.getCurrentPosition()) + Math.abs(backRightDrive.getCurrentPosition())) / 4.0;
    }

    private void setDrivePower(double axial, double lateral, double yaw) {
        double fl = axial + lateral + yaw; double fr = axial - lateral - yaw;
        double bl = axial - lateral + yaw; double br = axial + lateral - yaw;
        frontLeftDrive.setPower(fl); frontRightDrive.setPower(fr);
        backLeftDrive.setPower(bl); backRightDrive.setPower(br);
    }
}
