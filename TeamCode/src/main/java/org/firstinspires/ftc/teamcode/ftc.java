package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "FTC 4-Motor Test", group = "Diagnostic")
public class ftc extends LinearOpMode {

    // --- Drive Motors ---
    private DcMotorEx fr = null;
    private DcMotorEx fl = null;
    private DcMotorEx br = null;
    private DcMotorEx bl = null;

    private static final double TEST_POWER = 0.75;

    // --- Flywheel Motors ---
    private DcMotorEx flywheelMotor1 = null;
    private DcMotorEx flywheelMotor2 = null;

    private static final double FLYWHEEL_MOTOR_TICKS_PER_REV = 560;
    private double targetRpm = 150.0;

    private boolean dpadUpPressedLast = false;
    private boolean dpadDownPressedLast = false;

    // --- Servo Motor ---
    private Servo servo = null;

    private enum ServoState { STOPPED, FORWARD, REVERSE }
    private ServoState servoState = ServoState.STOPPED;

    private boolean yPressedLast = false;
    private boolean xPressedLast = false;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing...");
        telemetry.update();

        // --- Initialize Drive Motors ---
        try {
            fr = hardwareMap.get(DcMotorEx.class, "fr");
            fl = hardwareMap.get(DcMotorEx.class, "fl");
            br = hardwareMap.get(DcMotorEx.class, "br");
            bl = hardwareMap.get(DcMotorEx.class, "bl");

            fr.setDirection(DcMotorEx.Direction.FORWARD);
            fl.setDirection(DcMotorEx.Direction.FORWARD);
            br.setDirection(DcMotorEx.Direction.FORWARD);
            bl.setDirection(DcMotorEx.Direction.FORWARD);

            fr.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            fl.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            br.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            bl.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);

            fr.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
            fl.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
            br.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
            bl.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        } catch (Exception e) {
            telemetry.addData("Drive Motors", "Error: " + e.getMessage());
        }

        // --- Initialize Flywheel Motors ---
        try {
            flywheelMotor1 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_1");
            flywheelMotor1.setDirection(DcMotorEx.Direction.REVERSE);
            flywheelMotor1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
            flywheelMotor1.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        } catch (Exception e) {
            telemetry.addData("Flywheel 1", "Error: " + e.getMessage());
        }

        try {
            flywheelMotor2 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_2");
            flywheelMotor2.setDirection(DcMotorEx.Direction.REVERSE);
            flywheelMotor2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
            flywheelMotor2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        } catch (Exception e) {
            telemetry.addData("Flywheel 2", "Error: " + e.getMessage());
        }

        // --- Initialize Servo ---
        try {
            servo = hardwareMap.get(Servo.class, "servo");
            servo.setPosition(0.5);
        } catch (Exception e) {
            telemetry.addData("Servo", "Error: " + e.getMessage());
        }

        telemetry.addData("Status", "Initialized. Ready to start.");
        telemetry.addLine("\n--- Controls Mapping ---");
        telemetry.addLine("L-Stick UP: FR/BR fwd, FL/BL rev");
        telemetry.addLine("L-Stick DOWN: FR/BR rev, FL/BL fwd");
        telemetry.addLine("L-Stick LEFT: FL/FR rev, BL/BR fwd (Strafe Left)");
        telemetry.addLine("L-Stick RIGHT: FL/FR fwd, BL/BR rev (Strafe Right)");
        telemetry.addLine("Left Bumper: All 4 motors fwd");
        telemetry.addLine("Right Bumper: All 4 motors rev");
        telemetry.addLine("DPAD UP/DOWN: Increase/Decrease Target RPM");
        telemetry.addLine("Button A: Spin Flywheel 1");
        telemetry.addLine("Button B: Spin Flywheel 2");
        telemetry.addLine("Button Y: Servo Forward (Toggle)");
        telemetry.addLine("Button X: Servo Reverse (Toggle)");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            
            // --- Drive Motor Controls ---
            double yAxis = -gamepad1.left_stick_y; 
            double xAxis = gamepad1.left_stick_x;
            double deadzone = 0.5;

            if (yAxis > deadzone) {
                setMotorPowers(TEST_POWER, -TEST_POWER, TEST_POWER, -TEST_POWER);
                telemetry.addData("Drive", "Joystick UP: FR/BR Fwd, FL/BL Rev");
            } else if (yAxis < -deadzone) {
                setMotorPowers(-TEST_POWER, TEST_POWER, -TEST_POWER, TEST_POWER);
                telemetry.addData("Drive", "Joystick DOWN: FR/BR Rev, FL/BL Fwd");
            } else if (xAxis < -deadzone) {
                setMotorPowers(-TEST_POWER, -TEST_POWER, TEST_POWER, TEST_POWER);
                telemetry.addData("Drive", "Joystick LEFT (Strafe): FL/FR Rev, BL/BR Fwd");
            } else if (xAxis > deadzone) {
                setMotorPowers(TEST_POWER, TEST_POWER, -TEST_POWER, -TEST_POWER);
                telemetry.addData("Drive", "Joystick RIGHT (Strafe): FL/FR Fwd, BL/BR Rev");
            } else if (gamepad1.left_bumper) {
                setMotorPowers(TEST_POWER, TEST_POWER, TEST_POWER, TEST_POWER);
                telemetry.addData("Drive", "Left Bumper: All Fwd");
            } else if (gamepad1.right_bumper) {
                setMotorPowers(-TEST_POWER, -TEST_POWER, -TEST_POWER, -TEST_POWER);
                telemetry.addData("Drive", "Right Bumper: All Rev");
            } else {
                setMotorPowers(0, 0, 0, 0);
                telemetry.addData("Drive", "Stopped");
            }

            // --- Flywheel Target RPM Adjustment ---
            if (gamepad1.dpad_up && !dpadUpPressedLast) {
                targetRpm += 10;
            }
            if (gamepad1.dpad_down && !dpadDownPressedLast) {
                targetRpm = Math.max(0, targetRpm - 10);
            }
            dpadUpPressedLast = gamepad1.dpad_up;
            dpadDownPressedLast = gamepad1.dpad_down;

            telemetry.addData("Target RPM", "%.0f RPM", targetRpm);
            double targetVelocity = (targetRpm / 60.0) * FLYWHEEL_MOTOR_TICKS_PER_REV;

            // --- Flywheel 1 Control ---
            if (flywheelMotor1 != null) {
                if (gamepad1.a) {
                    flywheelMotor1.setVelocity(targetVelocity);
                    double actualRpm1 = (flywheelMotor1.getVelocity() / FLYWHEEL_MOTOR_TICKS_PER_REV) * 60.0;
                    telemetry.addData("Flywheel 1", "Actual: %.0f RPM", actualRpm1);
                } else {
                    flywheelMotor1.setVelocity(0);
                    telemetry.addData("Flywheel 1", "Stopped");
                }
            } else {
                telemetry.addData("Flywheel 1", "NOT INITIALIZED");
            }

            // --- Flywheel 2 Control ---
            if (flywheelMotor2 != null) {
                if (gamepad1.b) {
                    flywheelMotor2.setVelocity(targetVelocity);
                    double actualRpm2 = (flywheelMotor2.getVelocity() / FLYWHEEL_MOTOR_TICKS_PER_REV) * 60.0;
                    telemetry.addData("Flywheel 2", "Actual: %.0f RPM", actualRpm2);
                } else {
                    flywheelMotor2.setVelocity(0);
                    telemetry.addData("Flywheel 2", "Stopped");
                }
            } else {
                telemetry.addData("Flywheel 2", "NOT INITIALIZED");
            }

            // --- Servo Controls ---
            boolean currentY = gamepad1.y;
            boolean currentX = gamepad1.x;

            if (currentY && !yPressedLast) {
                if (servoState == ServoState.FORWARD) {
                    servoState = ServoState.STOPPED;
                } else {
                    servoState = ServoState.FORWARD;
                }
            }

            if (currentX && !xPressedLast) {
                if (servoState == ServoState.REVERSE) {
                    servoState = ServoState.STOPPED;
                } else {
                    servoState = ServoState.REVERSE;
                }
            }

            yPressedLast = currentY;
            xPressedLast = currentX;

            if (servo != null) {
                if (servoState == ServoState.FORWARD) {
                    servo.setPosition(1.0);
                    telemetry.addData("Servo", "FORWARD (Pos: 1.0)");
                } else if (servoState == ServoState.REVERSE) {
                    servo.setPosition(0.0);
                    telemetry.addData("Servo", "REVERSE (Pos: 0.0)");
                } else {
                    servo.setPosition(0.5);
                    telemetry.addData("Servo", "STOPPED (Pos: 0.5)");
                }
            } else {
                telemetry.addData("Servo", "NOT INITIALIZED (check config: servo)");
            }

            telemetry.update();
        }

        // Stop all motors on exit
        setMotorPowers(0, 0, 0, 0);
        if (flywheelMotor1 != null) flywheelMotor1.setVelocity(0);
        if (flywheelMotor2 != null) flywheelMotor2.setVelocity(0);
        if (servo != null) servo.setPosition(0.5);
    }

    // Helper method to set drive motor powers safely
    private void setMotorPowers(double frPower, double flPower, double brPower, double blPower) {
        if (fr != null) fr.setPower(frPower);
        if (fl != null) fl.setPower(flPower);
        if (br != null) br.setPower(brPower);
        if (bl != null) bl.setPower(blPower);
    }
}
