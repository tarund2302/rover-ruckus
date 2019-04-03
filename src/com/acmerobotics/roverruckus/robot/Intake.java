package com.acmerobotics.roverruckus.robot;

import android.util.Log;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.profile.MotionProfile;
import com.acmerobotics.roadrunner.profile.MotionProfileGenerator;
import com.acmerobotics.roadrunner.profile.MotionState;
import com.acmerobotics.roverruckus.hardware.CachingDcMotorEx;
import com.acmerobotics.roverruckus.util.PIDController;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * Created by Emma Sheffo on 11/16/2018.
 */
@Config
public class Intake extends Subsystem {
    public static final String TAG = "intake";
    public static double RAKE_UP = .8;
    public static double RAKE_DOWN = .35;
    public static double GROUND_INTAKERER_IN = .5;
    public static double GROUND_INTAKERER_OUT = .5;
    public static double WINCH_RADIUS = .5;
    public static double MAX_V = 30;
    public static double MAX_A = 30;
    public static double MAX_J = 15;
    public static double P = -10;
    public static double I = 0;
    public static double D = 0;
    public static double RAKE_RETRACT_DISTANCE = 6.5;
    public static double EXPEL_DURATION = 1000;
    public static double DEBOUNCE_TIME = 500;

    private DcMotorEx rakeMotor, intakeMotor;

    private Servo rakeServo, groundIntakerer;

    private boolean driverControled = true;
    private boolean profileComplete = true;
    private double armPower = 0;
    private MotionProfile armProfile;
    private PIDController controller;
    private long startTime;
    private double offset = 0;
    private double targetX = 0;

    private AnalogInput beamBreak;
    private boolean intaking = false;
    private boolean needStop = false;
    private long stopTime;
    private double lastLow = 0;

    public Intake(Robot robot, HardwareMap map) {

        rakeMotor = robot.getMotor("rakeMotor");
        rakeMotor.setDirection(DcMotorSimple.Direction.REVERSE); //todo check direction
        rakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//        setPosition(0);

        intakeMotor = robot.getMotor("intakeMotor");
        intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        beamBreak = robot.getAnalogInput(0, 1);

        rakeServo = robot.getServo("rake");
        groundIntakerer = robot.getServo("ground");
        setPosition(0);
        controller = new PIDController(P, I, D);
        driverControled = true;
        rakeUp();

    }

    public void setArmPower(double power) {
        armPower = power;
        driverControled = driverControled || power > 0;
    }

    @Override
    public void update(TelemetryPacket packet) {
        packet.put("rakePosition", getPosition());
        packet.put("beamBreak", beamBreak.getVoltage());
        Log.i(TAG, "position: " + getPosition());
        Log.i(TAG, "driver controlled: " + driverControled);
        Log.i(TAG, "complete" + profileComplete);
        if (driverControled) {
            rakeMotor.setPower(armPower);
            targetX = getPosition();

        } else {
            long now = System.currentTimeMillis();
            double t = (now - startTime) / 1000.0;
            Log.i(TAG, "t: " + t);
//            Log.i(TAG, "duration" + armProfile.duration());
            double targetV = 0;
            if (armProfile != null && t <= armProfile.duration()) {
                MotionState targetState = armProfile.get(t);
                targetV = targetState.getV();
                Log.i(TAG, "targetV: " + targetV);
                targetX = targetState.getX();
                Log.i(TAG, "targetX:" + targetX);
            } else {
                profileComplete = true;
            }
            double error = getPosition() - targetX;
            Log.i(TAG, "error" + error);
            packet.put("rakeError", error);
            double correction = controller.update(error);
            Log.i(TAG, "correction: " + correction);
            packet.put("command", targetV + correction);
            rakeMotor.setVelocity((targetV + correction) / WINCH_RADIUS, AngleUnit.RADIANS);
        }

        if (intaking) {
            if (beamBreak.getVoltage() > 500) lastLow = System.currentTimeMillis();
            if (beamBreak.getVoltage() < 500 && lastLow + DEBOUNCE_TIME <= System.currentTimeMillis()) {
                intaking = false;
                intakeMotor.setPower(-1);
                needStop = true;
                stopTime = System.currentTimeMillis() + (long) EXPEL_DURATION;
            }
        }

        if (needStop) {
            if (stopTime <= System.currentTimeMillis()) {
                needStop = false;
                intakeMotor.setPower(0);
            }
        }
    }

    private void goToPosition(double position) {
        armProfile = MotionProfileGenerator.generateSimpleMotionProfile(
                new MotionState(getPosition(), 0, 0, 0),
                new MotionState(position, 0, 0, 0),
                MAX_V, MAX_A, MAX_J);
        driverControled = false;

        startTime = System.currentTimeMillis();
        profileComplete = false;

    }

    public double getPosition() {
        return rakeMotor.getCurrentPosition() / rakeMotor.getMotorType().getTicksPerRev() * (2 * Math.PI * WINCH_RADIUS) + offset;
    }

    public void setPosition(double position) {
        offset = -(getPosition() - offset + position);
    }


    public void setIntakePower(double power) {
        intakeMotor.setPower(power);
    }

    private boolean rakeDown = false;

    public void rakeUp() {
        rakeServo.setPosition(RAKE_UP);
        rakeDown = false;
    }

    public void rakeDown() {
        rakeDown = true;
        rakeServo.setPosition(RAKE_DOWN);
    }

    public void toggleRake() {
        if (rakeDown) {
            rakeUp();
        } else {
            rakeDown();
        }
    }

    public void retractRake() {
        rakeUp();
        goToPosition(RAKE_RETRACT_DISTANCE);
        Log.e(TAG, "retracting");
    }

    public void groundIntakererIn () {
        groundIntakerer.setPosition(GROUND_INTAKERER_IN);
    }

    public void groundIntakererOut () {
        groundIntakerer.setPosition(GROUND_INTAKERER_OUT);
    }

    @Override
    public boolean isBusy() {
        return !profileComplete || intaking;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = 0;
    }

    public void intake () {
        intaking = true;
        intakeMotor.setPower(1);
    }

}


