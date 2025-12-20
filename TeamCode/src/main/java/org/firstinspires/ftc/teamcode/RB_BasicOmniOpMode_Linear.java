package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name="RoosterBots - Refactored OpMode", group="LinearOpMode")
//@Disabled
public class RB_BasicOmniOpMode_Linear extends LinearOpMode {

    // --- Time and Drive Train ---
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotorEx frontLeftDrive = null;
    private DcMotorEx frontRightDrive = null;
    private DcMotorEx backLeftDrive = null;
    private DcMotorEx backRightDrive = null;

    // --- Mechanisms ---
    private DcMotorEx flywheelMotor1 = null;
    private DcMotorEx flywheelMotor2 = null;
    private Servo pusherServo = null;

    // --- Control Variables ---
    private double drivePowerMultiplier = 1.0;
    private double maxFlywheelRpm = 225; // Persistent base RPM
    
    // Flywheel State Tracking
    private boolean flywheelIsRunning = false;
    private boolean flywheelIsReversed = false;

    // --- Button Press Detection (Previous State) ---
    private boolean dpadDownPressedLast = false;
    private boolean dpadUpPressedLast = false;
    private boolean dpadLeftPressedLast = false;
    private boolean dpadRightPressedLast = false;
    private boolean pusherButtonPressedLast = false;
    private boolean leftBumperPressedLast = false;
    private boolean rightBumperPressedLast = false;
    private boolean yPressedLast = false;

    // --- Automated Protocol (Macro) Settings ---
    private enum AutoMoveState { 
        IDLE, 
        TURNING_RIGHT, 
        PREPARING_FLYWHEEL, 
        FIRING, 
        WAIT_FOR_PUSHER, 
        WAITING_BETWEEN_SHOTS, 
        MOVING_FINAL_FORWARD 
    };
    private AutoMoveState autoMoveState = AutoMoveState.IDLE;
    private ElapsedTime autoMoveTimer = new ElapsedTime();
    private int shotCount = 0;
    private double savedMaxRpm = 225;
    private boolean savedFlywheelRunning = false;

    // *** TUNE THESE DISTANCES ***
    private static final double AUTO_DISTANCE_BASE_FT = 2.0; 
    private static final double AUTO_TURN_DEGREES = 30.0;
    private static final double AUTO_FIRE_RPM = 175.0;
    private static final double AUTO_DRIVE_POWER = 0.25;
    private static final double AUTO_SHOT_WAIT_MS = 150; 

    // *** ENCODER CALIBRATION ***
    private static final double TICKS_PER_REV = 28; 
    private static final double WHEEL_DIAMETER_CM = 10.0;
    private static final double CM_PER_FOOT = 30.48;
    private static final double TICKS_PER_FOOT = (TICKS_PER_REV / (WHEEL_DIAMETER_CM * Math.PI)) * CM_PER_FOOT;
    private static final double TICKS_PER_DEGREE = 0.31; 

    // --- Pusher Servo State Machine ---
    private ElapsedTime pusherServoTimer = new ElapsedTime();
    private enum PusherServoState { IDLE, PUSHING, PAUSE_AFTER_PUSH, RETURNING };
    private PusherServoState pusherServoState = PusherServoState.IDLE;
    private static final double PUSHER_SERVO_FORWARD_TIME_MS = 1000;
    private static final double PUSHER_SERVO_PAUSE_TIME_MS = 50;

    // --- Flywheel Constants ---
    private static final double FLYWHEEL_MOTOR_TICKS_PER_REV = 560;
    private static final double MAX_POSSIBLE_RPM = 5000; 
    private static final double RPM_ADJUST_INCREMENT = 25;

    @Override
    public void runOpMode() {

        // --- Initialization ---
        frontLeftDrive = hardwareMap.get(DcMotorEx.class, "front_left_drive");
        frontRightDrive = hardwareMap.get(DcMotorEx.class, "front_right_drive");
        backLeftDrive = hardwareMap.get(DcMotorEx.class, "back_left_drive");
        backRightDrive = hardwareMap.get(DcMotorEx.class, "back_right_drive");

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

        // Make the phone screen update faster
        telemetry.setMsTransmissionInterval(50);
        telemetry.addData("Status", "Initialized and Ready");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            // --- Snapshot Gamepad State ---
            // We capture the buttons ONCE at the start of the loop to prevent "missed" presses
            boolean currentDpadUp = gamepad1.dpad_up;
            boolean currentDpadDown = gamepad1.dpad_down;
            boolean currentDpadLeft = gamepad1.dpad_left;
            boolean currentDpadRight = gamepad1.dpad_right;
            boolean currentX = gamepad1.x;
            boolean currentY = gamepad1.y;
            boolean currentLB = gamepad1.left_bumper;
            boolean currentRB = gamepad1.right_bumper;

            // --- Automated Protocol Logic (Y Button) ---
            if (currentY && !yPressedLast && autoMoveState == AutoMoveState.IDLE) {
                resetDriveEncoders();
                autoMoveState = AutoMoveState.TURNING_RIGHT;
            }

            if (autoMoveState != AutoMoveState.IDLE) {
                handleAutoMove();
            } else {
                // --- Manual Controls ---
                double axial   = -gamepad1.left_stick_y;
                double lateral =  gamepad1.left_stick_x;
                double yaw     =  gamepad1.right_stick_x;

                axial   = Math.pow(axial, 3);
                lateral = Math.pow(lateral, 3);
                yaw     = Math.pow(yaw, 3);

                double frontLeftPower  = axial + lateral + yaw;
                double frontRightPower = axial - lateral - yaw;
                double backLeftPower   = axial - lateral + yaw;
                double backRightPower  = axial + lateral - yaw;

                double max = Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower));
                max = Math.max(max, Math.abs(backLeftPower));
                max = Math.max(max, Math.abs(backRightPower));

                if (max > 1.0) {
                    frontLeftPower /= max; frontRightPower /= max;
                    backLeftPower /= max; backRightPower /= max;
                }

                if (currentDpadLeft && !dpadLeftPressedLast) drivePowerMultiplier = Math.min(1.0, drivePowerMultiplier + 0.10);
                if (currentDpadRight && !dpadRightPressedLast) drivePowerMultiplier = Math.max(0.0, drivePowerMultiplier - 0.10);

                frontLeftDrive.setPower(frontLeftPower * drivePowerMultiplier);
                frontRightDrive.setPower(frontRightPower * drivePowerMultiplier);
                backLeftDrive.setPower(backLeftPower * drivePowerMultiplier);
                backRightDrive.setPower(backRightPower * drivePowerMultiplier);

                // Manual Flywheel RPM Adjustment
                if (currentDpadUp && !dpadUpPressedLast) {
                    maxFlywheelRpm = Math.min(MAX_POSSIBLE_RPM, maxFlywheelRpm + RPM_ADJUST_INCREMENT);
                }
                if (currentDpadDown && !dpadDownPressedLast) {
                    maxFlywheelRpm = Math.max(0, maxFlywheelRpm - RPM_ADJUST_INCREMENT);
                }

                if (currentLB && !leftBumperPressedLast) {
                    if (flywheelIsRunning && !flywheelIsReversed) flywheelIsRunning = false;
                    else { flywheelIsRunning = true; flywheelIsReversed = false; }
                }
                if (currentRB && !rightBumperPressedLast) {
                    if (flywheelIsRunning && flywheelIsReversed) flywheelIsRunning = false;
                    else { flywheelIsRunning = true; flywheelIsReversed = true; }
                }
            }

            double targetFlywheelRpm = 0.0;
            if (flywheelIsRunning) {
                targetFlywheelRpm = flywheelIsReversed ? -maxFlywheelRpm : maxFlywheelRpm;
            }
            double flywheelVelocity = (targetFlywheelRpm / 60.0) * FLYWHEEL_MOTOR_TICKS_PER_REV;
            flywheelMotor1.setVelocity(flywheelVelocity);
            flywheelMotor2.setVelocity(flywheelVelocity);

            switch (pusherServoState) {
                case IDLE:
                    if (autoMoveState == AutoMoveState.IDLE && currentX && !pusherButtonPressedLast) {
                        pusherServoState = PusherServoState.PUSHING;
                        pusherServoTimer.reset();
                    } else {
                        pusherServo.setPosition(0.5);
                    }
                    break;
                case PUSHING:
                    pusherServo.setPosition(1.0);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_FORWARD_TIME_MS) {
                        pusherServoState = PusherServoState.PAUSE_AFTER_PUSH;
                        pusherServoTimer.reset();
                    }
                    break;
                case PAUSE_AFTER_PUSH:
                    pusherServo.setPosition(0.5);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_PAUSE_TIME_MS) {
                        pusherServoState = PusherServoState.RETURNING;
                        pusherServoTimer.reset();
                    }
                    break;
                case RETURNING:
                    pusherServo.setPosition(0.0);
                    if (pusherServoTimer.milliseconds() >= PUSHER_SERVO_FORWARD_TIME_MS) {
                        pusherServoState = PusherServoState.IDLE;
                    }
                    break;
            }

            // --- Update States ---
            // Save current snapshot into "Last" variables for next loop
            dpadDownPressedLast = currentDpadDown;
            dpadUpPressedLast = currentDpadUp;
            dpadLeftPressedLast = currentDpadLeft;
            dpadRightPressedLast = currentDpadRight;
            pusherButtonPressedLast = currentX;
            leftBumperPressedLast = currentLB;
            rightBumperPressedLast = currentRB;
            yPressedLast = currentY;

            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.addData("FLYWHEEL SETTING", "%.0f RPM", maxFlywheelRpm);
            telemetry.addData("Current Target", "%.0f RPM", targetFlywheelRpm);
            telemetry.addData("Drive Mode", autoMoveState == AutoMoveState.IDLE ? "Manual" : "Auto (" + autoMoveState + ")");
            telemetry.update();
        }
    }

    private void handleAutoMove() {
        switch (autoMoveState) {
            case TURNING_RIGHT:
                double targetTicksTurn = AUTO_TURN_DEGREES * TICKS_PER_DEGREE;
                setDrivePower(0, 0, AUTO_DRIVE_POWER); 
                if (Math.abs(getAverageDrivePosition()) >= targetTicksTurn) {
                    setDrivePower(0, 0, 0);
                    resetDriveEncoders();
                    savedMaxRpm = maxFlywheelRpm;
                    savedFlywheelRunning = flywheelIsRunning;
                    maxFlywheelRpm = AUTO_FIRE_RPM;
                    flywheelIsRunning = true;
                    flywheelIsReversed = false;
                    shotCount = 0;
                    autoMoveTimer.reset();
                    autoMoveState = AutoMoveState.PREPARING_FLYWHEEL;
                }
                break;

            case PREPARING_FLYWHEEL:
                if (autoMoveTimer.milliseconds() >= 1500) { 
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
                        maxFlywheelRpm = savedMaxRpm;
                        flywheelIsRunning = savedFlywheelRunning;
                        resetDriveEncoders();
                        autoMoveState = AutoMoveState.MOVING_FINAL_FORWARD;
                    }
                }
                break;

            case WAITING_BETWEEN_SHOTS:
                if (autoMoveTimer.milliseconds() >= AUTO_SHOT_WAIT_MS) {
                    autoMoveState = AutoMoveState.FIRING;
                }
                break;

            case MOVING_FINAL_FORWARD:
                double targetTicksForward = 6.0 * AUTO_DISTANCE_BASE_FT * TICKS_PER_FOOT;
                setDrivePower(AUTO_DRIVE_POWER, 0, 0);
                if (Math.abs(getAverageDrivePosition()) >= targetTicksForward) {
                    setDrivePower(0, 0, 0);
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
        return (Math.abs(frontLeftDrive.getCurrentPosition()) + 
                Math.abs(frontRightDrive.getCurrentPosition()) + 
                Math.abs(backLeftDrive.getCurrentPosition()) + 
                Math.abs(backRightDrive.getCurrentPosition())) / 4.0;
    }

    private void setDrivePower(double axial, double lateral, double yaw) {
        double fl = axial + lateral + yaw;
        double fr = axial - lateral - yaw;
        double bl = axial - lateral + yaw;
        double br = axial + lateral - yaw;
        frontLeftDrive.setPower(fl);
        frontRightDrive.setPower(fr);
        backLeftDrive.setPower(bl);
        backRightDrive.setPower(br);
    }
}
