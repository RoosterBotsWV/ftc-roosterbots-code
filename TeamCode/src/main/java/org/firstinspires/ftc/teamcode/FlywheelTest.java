package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@TeleOp(name = "Flywheel Diagnostic Test", group = "Diagnostic")
public class FlywheelTest extends LinearOpMode {

    private DcMotorEx flywheelMotor1 = null;
    private DcMotorEx flywheelMotor2 = null;

    // Use a moderate power for testing
    private static final double TEST_POWER = 0.5;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing...");
        telemetry.update();

        // --- Hardware Initialization ---
        // Make sure the names \"flywheel_motor_1\" and \"flywheel_motor_2\"
        // exactly match your robot's configuration file.
        try {
            flywheelMotor1 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_1");
            flywheelMotor1.setDirection(DcMotorEx.Direction.FORWARD);
            flywheelMotor1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor1.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
            telemetry.addData("Motor 1", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Motor 1", "Error: " + e.getMessage());
        }

        try {
            flywheelMotor2 = hardwareMap.get(DcMotorEx.class, "flywheel_motor_2");
            flywheelMotor2.setDirection(DcMotorEx.Direction.REVERSE); // Set to REVERSE as needed
            flywheelMotor2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
            flywheelMotor2.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
            telemetry.addData("Motor 2", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Motor 2", "Error: " + e.getMessage());
        }

        telemetry.addData("Status", "Initialized. Ready to start.");
        telemetry.addLine("\nPress 'A' to test Flywheel Motor 1.");
        telemetry.addLine("Press 'B' to test Flywheel Motor 2.");
        telemetry.addLine("Motors will run as long as the button is held.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            // --- Test Motor 1 ---
            if (gamepad1.a) {
                flywheelMotor1.setPower(TEST_POWER);
                telemetry.addData("Motor 1", "Running at %.2f power", TEST_POWER);
            } else {
                flywheelMotor1.setPower(0);
                telemetry.addData("Motor 1", "Stopped");
            }

            // --- Test Motor 2 ---
            if (gamepad1.b) {
                flywheelMotor2.setPower(TEST_POWER);
                telemetry.addData("Motor 2", "Running at %.2f power", TEST_POWER);
            } else {
                flywheelMotor2.setPower(0);
                telemetry.addData("Motor 2", "Stopped");
            }

            telemetry.update();
        }

        // Stop motors on exit
        if (flywheelMotor1 != null) {
            flywheelMotor1.setPower(0);
        }
        if (flywheelMotor2 != null) {
            flywheelMotor2.setPower(0);
        }
    }
}