package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Flywheel Diagnostic Test", group = "Diagnostic")
public class FlywheelTest extends LinearOpMode {

    private DcMotorEx flywheelMotor1 = null;
    private DcMotorEx flywheelMotor2 = null;

    // Encoder ticks per ro
    // [[[[[[[[evolution for the flywheel motor
    private static final double FLYWHEEL_MOTOR_TICKS_PER_REV = 560;

    // Target velocity in RPM (starts at 100 RPM)
    private double targetRpm = 300.0;

    private boolean dpadUpPressedLast = false;
    private boolean dpadDownPressedLast = false;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing...");
        telemetry.update();

        // --- Hardware Initialization ---
        try {
            flywheelMotor1 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_1");
            flywheelMotor1.setDirection(DcMotorEx.Direction.FORWARD);
            flywheelMotor1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
            flywheelMotor1.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
            telemetry.addData("Motor 1", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Motor 1", "Error: " + e.getMessage());
        }

        try {
            flywheelMotor2 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_2");
            flywheelMotor2.setDirection(DcMotorEx.Direction.REVERSE);
            flywheelMotor2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
            flywheelMotor2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
            telemetry.addData("Motor 2", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Motor 2", "Error: " + e.getMessage());
        }

        telemetry.addData("Status", "Initialized. Ready to start.");
        telemetry.addLine("\nUse DPAD Up / Down to adjust Target RPM.");
        telemetry.addLine("Press 'A' to test Flywheel Motor 1.");
        telemetry.addLine("Press 'B' to test Flywheel Motor 2.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            // --- Adjust Target RPM with DPAD UP / DOWN ---
            if (gamepad1.dpad_up && !dpadUpPressedLast) {
                targetRpm += 25;
            }
            if (gamepad1.dpad_down && !dpadDownPressedLast) {
                targetRpm = Math.max(0, targetRpm - 25);
            }
            dpadUpPressedLast = gamepad1.dpad_up;
            dpadDownPressedLast = gamepad1.dpad_down;

            telemetry.addData("Target RPM", "%.0f RPM (DPAD Up/Down to adjust)", targetRpm);

            double targetVelocity = (targetRpm / 60.0) * FLYWHEEL_MOTOR_TICKS_PER_REV;

            // --- Test Motor 1 ---
            if (flywheelMotor1 != null) {
                if (gamepad1.a) {
                    flywheelMotor1.setVelocity(targetVelocity);
                    double actualRpm1 = (flywheelMotor1.getVelocity() / FLYWHEEL_MOTOR_TICKS_PER_REV) * 60.0;
                    telemetry.addData("Motor 1", "Target: %.0f RPM | Actual: %.0f RPM", targetRpm, actualRpm1);
                } else {
                    flywheelMotor1.setVelocity(0);
                    telemetry.addData("Motor 1", "Stopped");
                }
            } else {
                telemetry.addData("Motor 1", "NOT INITIALIZED (check config: flywheel_motor_1)");
            }

            // --- Test Motor 2 ---
            if (flywheelMotor2 != null) {
                if (gamepad1.b) {
                    flywheelMotor2.setVelocity(targetVelocity);
                    double actualRpm2 = (flywheelMotor2.getVelocity() / FLYWHEEL_MOTOR_TICKS_PER_REV) * 60.0;
                    telemetry.addData("Motor 2", "Target: %.0f RPM | Actual: %.0f RPM", targetRpm, actualRpm2);
                } else {
                    flywheelMotor2.setVelocity(0);
                    telemetry.addData("Motor 2", "Stopped");
                }
            } else {
                telemetry.addData("Motor 2", "NOT INITIALIZED (check config: flywheel_motor_2)");
            }

            telemetry.update();
        }

        // Stop motors on exit
        if (flywheelMotor1 != null) {
            flywheelMotor1.setVelocity(0);
        }
        if (flywheelMotor2 != null) {
            flywheelMotor2.setVelocity(0);
        }
    }
}


//this is a note. please read these when they pop up